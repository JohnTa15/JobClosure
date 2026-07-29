package gr.gtar.jobclosure.ui.bookingedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.calendar.CalendarHelper
import gr.gtar.jobclosure.calendar.CalendarInfo
import gr.gtar.jobclosure.data.Booking
import gr.gtar.jobclosure.data.BookingRepository
import gr.gtar.jobclosure.data.BookingType
import gr.gtar.jobclosure.data.SettingsRepository
import gr.gtar.jobclosure.shared.calendar.BookingMetadata
import gr.gtar.jobclosure.shared.calendar.BookingMetadataCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

data class BookingEditUiState(
    val id: Long = 0L,
    val bookingId: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val type: BookingType = BookingType.WEDDING,
    val notes: String = "",
    val isConfirmed: Boolean = true,
    val hasDrone: Boolean = false,
    val churchName: String = "",
    val churchAddress: String = "",
    val ceremonyStart: LocalDateTime = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0),
    val ceremonyDurationMinutes: Int = 60,
    val hasReception: Boolean = false,
    val receptionVenueName: String = "",
    val receptionVenueAddress: String = "",
    val receptionStart: LocalDateTime? = null,
    val receptionDurationMinutes: Int = 240,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val addToCalendar: Boolean = true,
    val selectedCalendarId: Long? = null,
    val availableCalendars: List<CalendarInfo> = emptyList(),
    val conflicts: List<Booking> = emptyList(),
    val showDeleteConfirmation: Boolean = false,
    val saved: Boolean = false,
    val churchCalendarEventId: Long? = null,
    val receptionCalendarEventId: Long? = null,
) {
    val isChurchSacrament get() = type.isChurchSacrament
}

class BookingEditViewModel(
    application: Application,
    private val repository: BookingRepository,
    private val settingsRepository: SettingsRepository,
    private val bookingId: Long?,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BookingEditUiState())
    val uiState: StateFlow<BookingEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val calendars = CalendarHelper.getWritableCalendars(getApplication())
            val settings = settingsRepository.settings.first()
            val existing = bookingId?.let { repository.getById(it) }
            _uiState.value = if (existing != null) {
                BookingEditUiState(
                    id = existing.id,
                    bookingId = existing.bookingId,
                    title = existing.title,
                    type = existing.type,
                    notes = existing.notes,
                    isConfirmed = existing.isConfirmed,
                    hasDrone = existing.hasDrone,
                    churchName = existing.churchName,
                    churchAddress = existing.churchAddress,
                    ceremonyStart = existing.ceremonyStart,
                    ceremonyDurationMinutes = existing.ceremonyDurationMinutes,
                    hasReception = existing.hasReception,
                    receptionVenueName = existing.receptionVenueName,
                    receptionVenueAddress = existing.receptionVenueAddress,
                    receptionStart = existing.receptionStart,
                    receptionDurationMinutes = existing.receptionDurationMinutes,
                    selectedCalendarId = existing.calendarId ?: settings.defaultCalendarId,
                    availableCalendars = calendars,
                    isLoading = false,
                    churchCalendarEventId = existing.churchCalendarEventId,
                    receptionCalendarEventId = existing.receptionCalendarEventId,
                )
            } else {
                _uiState.value.copy(
                    selectedCalendarId = settings.defaultCalendarId,
                    availableCalendars = calendars,
                    isLoading = false,
                )
            }
        }
    }

    fun update(transform: (BookingEditUiState) -> BookingEditUiState) {
        _uiState.value = transform(_uiState.value)
    }

    fun dismissConflicts() {
        _uiState.value = _uiState.value.copy(conflicts = emptyList())
    }

    /** Validates for conflicts first; if none (or the caller already confirmed), saves and syncs the calendar. */
    fun save(ignoreConflicts: Boolean = false) {
        val state = _uiState.value
        val booking = state.toBooking()

        viewModelScope.launch {
            if (!ignoreConflicts) {
                val conflicts = repository.findConflicts(booking)
                if (conflicts.isNotEmpty()) {
                    _uiState.value = state.copy(conflicts = conflicts)
                    return@launch
                }
            }

            _uiState.value = state.copy(isSaving = true, conflicts = emptyList())

            val savedId = repository.save(booking)
            var churchEventId = booking.churchCalendarEventId
            var receptionEventId = booking.receptionCalendarEventId

            if (state.addToCalendar && state.selectedCalendarId != null) {
                val calendarId = state.selectedCalendarId
                val context = getApplication<Application>()
                val appSettings = settingsRepository.settings.first()
                val reminderMinutes = appSettings.reminderMinutesBefore

                val churchDescription = BookingMetadataCodec.encode(
                    booking.notes,
                    BookingMetadata(
                        bookingId = booking.bookingId,
                        role = BookingMetadataCodec.ROLE_CEREMONY,
                        type = booking.type.name,
                        hasDrone = state.hasDrone,
                        isConfirmed = state.isConfirmed,
                        title = booking.title,
                        venueName = booking.churchName,
                    ),
                )
                val churchStartMillis = booking.ceremonyStart.toMillis()
                val churchEndMillis = booking.ceremonyEnd.toMillis()
                churchEventId = if (churchEventId != null) {
                    CalendarHelper.updateEvent(
                        context, churchEventId, eventTitle(state), booking.churchAddress,
                        churchDescription, churchStartMillis, churchEndMillis, reminderMinutes,
                    )
                    churchEventId
                } else {
                    CalendarHelper.insertEvent(
                        context, calendarId, eventTitle(state), booking.churchAddress,
                        churchDescription, churchStartMillis, churchEndMillis, reminderMinutes,
                    )
                }

                if (state.hasReception && state.receptionStart != null) {
                    val recDescription = BookingMetadataCodec.encode(
                        "",
                        BookingMetadata(
                            bookingId = booking.bookingId,
                            role = BookingMetadataCodec.ROLE_RECEPTION,
                            venueName = booking.receptionVenueName,
                        ),
                    )
                    val recStartMillis = state.receptionStart.toMillis()
                    val recEndMillis = booking.receptionEnd!!.toMillis()
                    val recTitle = "Δεξίωση: ${state.title}"
                    receptionEventId = if (receptionEventId != null) {
                        CalendarHelper.updateEvent(
                            context, receptionEventId, recTitle, booking.receptionVenueAddress,
                            recDescription, recStartMillis, recEndMillis, reminderMinutes,
                        )
                        receptionEventId
                    } else {
                        CalendarHelper.insertEvent(
                            context, calendarId, recTitle, booking.receptionVenueAddress,
                            recDescription, recStartMillis, recEndMillis, reminderMinutes,
                        )
                    }
                } else if (receptionEventId != null) {
                    CalendarHelper.deleteEvent(context, receptionEventId)
                    receptionEventId = null
                }

                // Inviting the drone partner as an attendee both puts the job on his calendar
                // (once he accepts) and makes Google Calendar send him the invite email - no
                // separate email-sending code needed. Turning the drone switch back off removes
                // the invite again instead of leaving a stale one.
                if (appSettings.dronePartnerEmail.isNotBlank()) {
                    churchEventId?.let {
                        CalendarHelper.setAttendee(context, it, appSettings.dronePartnerEmail, state.hasDrone)
                    }
                    receptionEventId?.let {
                        CalendarHelper.setAttendee(context, it, appSettings.dronePartnerEmail, state.hasDrone)
                    }
                }

                repository.save(
                    booking.copy(
                        id = savedId,
                        calendarId = calendarId,
                        churchCalendarEventId = churchEventId,
                        receptionCalendarEventId = receptionEventId,
                    )
                )
            }

            _uiState.value = state.copy(isSaving = false, saved = true)
        }
    }

    fun requestDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = true)
    }

    fun dismissDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
    }

    fun deleteAndFinish() {
        viewModelScope.launch {
            val existing = bookingId?.let { repository.getById(it) } ?: return@launch
            val context = getApplication<Application>()
            existing.churchCalendarEventId?.let { CalendarHelper.deleteEvent(context, it) }
            existing.receptionCalendarEventId?.let { CalendarHelper.deleteEvent(context, it) }
            repository.delete(existing)
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    private fun eventTitle(state: BookingEditUiState): String =
        "${state.type.displayName}: ${state.title}"

    private fun LocalDateTime.toMillis(): Long =
        this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun BookingEditUiState.toBooking(): Booking = Booking(
    id = id,
    bookingId = bookingId,
    title = title,
    type = type,
    notes = notes,
    isConfirmed = isConfirmed,
    hasDrone = hasDrone,
    churchName = churchName,
    churchAddress = churchAddress,
    ceremonyStart = ceremonyStart,
    ceremonyDurationMinutes = ceremonyDurationMinutes,
    hasReception = hasReception,
    receptionVenueName = receptionVenueName,
    receptionVenueAddress = receptionVenueAddress,
    receptionStart = if (hasReception) receptionStart else null,
    receptionDurationMinutes = receptionDurationMinutes,
    calendarId = selectedCalendarId,
    churchCalendarEventId = churchCalendarEventId,
    receptionCalendarEventId = receptionCalendarEventId,
)
