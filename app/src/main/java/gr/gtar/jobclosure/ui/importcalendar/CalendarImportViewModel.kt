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
    val duplicatesRemoved: Int? = null,
    val missingPermission: Boolean = false,
) {
    val selectableCount: Int get() = candidates.count { !it.alreadyImported }
    val selectedCount: Int get() = candidates.count { it.selected && !it.alreadyImported }
}

/** How far back to look. Sacraments booked years ago are exactly the point of this screen. */
private const val YEARS_BACK = 15L

/**
 * Identifies a job by what it is rather than which calendar entry produced it: same minute, same
 * type, same client. Case and accents are folded out because the two copies of a shared calendar
 * are often typed by different people.
 */
private fun contentKey(startMillis: Long, typeName: String, client: String): String {
    val folded = java.text.Normalizer.normalize(client.trim(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(java.util.Locale("el", "GR"))
        .replace('ς', 'σ')
        .replace(Regex("\\s+"), " ")
    return "${startMillis / 60_000L}|$typeName|$folded"
}

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
                CalendarHelper.readEvents(context, from, to)
                    .mapNotNull(CalendarBookingParser::parse)
                    // A calendar shared with someone else delivers the same sacrament twice, once
                    // per calendar, with different event ids but identical content. Offering both
                    // would import the job twice.
                    .distinctBy { contentKey(it.startMillis, it.type.name, it.clientName) }
            }

            // Two ways an event can already be in the app: it was imported before (so its calendar
            // event id is on a booking), or the app itself created it (same id, other direction).
            val existing = bookingRepository.observeAll().first()
            val knownEventIds = existing.flatMap { listOfNotNull(it.churchCalendarEventId, it.receptionCalendarEventId) }.toSet()
            // Matching on content as well as event id: the same sacrament imported from the other
            // copy of a shared calendar carries a different event id but is still the same job.
            val knownContent = existing.map {
                contentKey(Booking.epochMinute(it.ceremonyStart) * 60_000L, it.type.name, it.title)
            }.toSet()

            val candidates = parsed.map { candidate ->
                val already = candidate.calendarEventId in knownEventIds ||
                    contentKey(candidate.startMillis, candidate.type.name, candidate.clientName) in knownContent
                ImportCandidate(parsed = candidate, alreadyImported = already, selected = !already)
            }

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                hasScanned = true,
                candidates = candidates,
            )
        }
    }

    /**
     * Removes jobs already saved twice - the state a shared calendar left behind before the scan
     * learned to collapse them. Two bookings count as the same when their ceremony minute, type and
     * client all match; the oldest row of each group is kept, since anything the user has since
     * edited (price, notes, reception) is most likely on the one that has been around longest.
     */
    fun removeDuplicateBookings() {
        viewModelScope.launch {
            val all = bookingRepository.observeAll().first()
            val removed = all
                .groupBy { contentKey(Booking.epochMinute(it.ceremonyStart) * 60_000L, it.type.name, it.title) }
                .values
                .filter { it.size > 1 }
                .flatMap { group -> group.sortedBy { it.id }.drop(1) }

            removed.forEach { bookingRepository.delete(it) }
            _uiState.value = _uiState.value.copy(duplicatesRemoved = removed.size)
            if (_uiState.value.hasScanned) scan()
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
