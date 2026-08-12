package gr.gtar.jobclosure.ui.importcalendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.calendar.CalendarBookingParser
import gr.gtar.jobclosure.calendar.CalendarHelper
import gr.gtar.jobclosure.calendar.ParsedCalendarBooking
import gr.gtar.jobclosure.data.Booking
import gr.gtar.jobclosure.data.BookingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** A candidate row in the review list: what was parsed, whether it's already in the app, and
 *  whether the user has it ticked for import. */
data class ImportCandidate(
    val parsed: ParsedCalendarBooking,
    val alreadyImported: Boolean,
    val selected: Boolean,
)

data class CalendarImportUiState(
    val isScanning: Boolean = false,
    val hasScanned: Boolean = false,
    val candidates: List<ImportCandidate> = emptyList(),
    val importedCount: Int? = null,
    val missingPermission: Boolean = false,
) {
    val selectableCount: Int get() = candidates.count { !it.alreadyImported }
    val selectedCount: Int get() = candidates.count { it.selected && !it.alreadyImported }
}

/** How far back to look. Sacraments booked years ago are exactly the point of this screen. */
private const val YEARS_BACK = 10L

class CalendarImportViewModel(
    application: Application,
    private val bookingRepository: BookingRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CalendarImportUiState())
    val uiState: StateFlow<CalendarImportUiState> = _uiState.asStateFlow()

    fun scan() {
        val context = getApplication<Application>()
        if (!CalendarHelper.hasCalendarPermissions(context)) {
            _uiState.value = _uiState.value.copy(missingPermission = true)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, missingPermission = false, importedCount = null)

            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now()
            val from = now.minusYears(YEARS_BACK).atZone(zone).toInstant().toEpochMilli()
            val to = now.plusYears(1).atZone(zone).toInstant().toEpochMilli()

            val parsed = withContext(Dispatchers.IO) {
                CalendarHelper.readEvents(context, from, to).mapNotNull(CalendarBookingParser::parse)
            }

            // Two ways an event can already be in the app: it was imported before (so its calendar
            // event id is on a booking), or the app itself created it (same id, other direction).
            val existing = bookingRepository.observeAll().first()
            val knownEventIds = existing.flatMap { listOfNotNull(it.churchCalendarEventId, it.receptionCalendarEventId) }.toSet()

            val candidates = parsed.map { candidate ->
                val already = candidate.calendarEventId in knownEventIds
                ImportCandidate(parsed = candidate, alreadyImported = already, selected = !already)
            }

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                hasScanned = true,
                candidates = candidates,
            )
        }
    }

    fun toggle(calendarEventId: Long) {
        _uiState.value = _uiState.value.copy(
            candidates = _uiState.value.candidates.map { candidate ->
                if (candidate.parsed.calendarEventId == calendarEventId && !candidate.alreadyImported) {
                    candidate.copy(selected = !candidate.selected)
                } else {
                    candidate
                }
            },
        )
    }

    fun setAllSelected(selected: Boolean) {
        _uiState.value = _uiState.value.copy(
            candidates = _uiState.value.candidates.map {
                if (it.alreadyImported) it else it.copy(selected = selected)
            },
        )
    }

    /** Saves every ticked candidate. The calendar event id is carried over so a second scan
     *  recognises these as already imported instead of offering them again. */
    fun importSelected() {
        viewModelScope.launch {
            val toImport = _uiState.value.candidates.filter { it.selected && !it.alreadyImported }
            val zone = ZoneId.systemDefault()

            toImport.forEach { candidate ->
                val parsed = candidate.parsed
                val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(parsed.startMillis), zone)
                bookingRepository.save(
                    Booking(
                        title = parsed.clientName,
                        type = parsed.type,
                        notes = parsed.notes,
                        hasDrone = parsed.hasDrone,
                        churchName = parsed.venueName,
                        churchAddress = parsed.venueAddress,
                        ceremonyStart = start,
                        ceremonyDurationMinutes = parsed.durationMinutes,
                        calendarId = parsed.calendarId,
                        churchCalendarEventId = parsed.calendarEventId,
                    ),
                )
            }

            _uiState.value = _uiState.value.copy(
                importedCount = toImport.size,
                candidates = _uiState.value.candidates.map {
                    if (it.selected && !it.alreadyImported) it.copy(alreadyImported = true, selected = false) else it
                },
            )
        }
    }
}
