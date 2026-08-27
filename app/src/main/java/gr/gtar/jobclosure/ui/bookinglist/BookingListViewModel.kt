package gr.gtar.jobclosure.ui.bookinglist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.calendar.CalendarHelper
import gr.gtar.jobclosure.data.AppSettings
import gr.gtar.jobclosure.data.Booking
import gr.gtar.jobclosure.data.BookingRepository
import gr.gtar.jobclosure.data.BookingType
import gr.gtar.jobclosure.data.SettingsRepository
import gr.gtar.jobclosure.shared.changelog.CHANGELOG_HISTORY
import gr.gtar.jobclosure.shared.changelog.ChangelogEntry
import gr.gtar.jobclosure.shared.changelog.CURRENT_CHANGELOG_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What a bulk delete was asked to do, and what it managed. */
data class BulkDeleteRequest(
    val bookings: List<Booking>,
    /** How many of them actually have a device-calendar event behind them - without this the
     *  "delete from the calendar too" choice is an abstraction the user cannot check. */
    val withCalendarEvents: Int,
    val calendarPermissionGranted: Boolean,
)

data class BulkDeleteResult(
    val deleted: Int,
    val calendarEventsDeleted: Int,
    val calendarEventsFailed: Int,
)

enum class BookingFilter(val label: String) {
    ALL("Όλα"),
    WEDDING_BAPTISM("Γάμοι/Βαφτίσεις"),
    DRONE("Με Drone"),
    RECEPTION("Με Δεξίωση"),
}

class BookingListViewModel(
    application: Application,
    private val repository: BookingRepository,
    private val settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {

    private val activeFilter = MutableStateFlow(BookingFilter.ALL)
    val filter: StateFlow<BookingFilter> = activeFilter

    private val _pendingDelete = MutableStateFlow<Booking?>(null)
    val pendingDelete: StateFlow<Booking?> = _pendingDelete

    /** Ids rather than Bookings: the list is a live query, so holding the rows would keep a stale
     *  copy of anything edited while the selection is open. */
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    private val _pendingBulkDelete = MutableStateFlow<BulkDeleteRequest?>(null)
    val pendingBulkDelete: StateFlow<BulkDeleteRequest?> = _pendingBulkDelete

    private val _lastDeleteResult = MutableStateFlow<BulkDeleteResult?>(null)
    val lastDeleteResult: StateFlow<BulkDeleteResult?> = _lastDeleteResult

    /** Entries not yet shown, right after an update, until the user dismisses the "what's new"
     *  dialog - can be more than one if several updates happened between app opens. */
    val unseenChangelogEntries: StateFlow<List<ChangelogEntry>> =
        settingsRepository.settings
            .map { settings -> CHANGELOG_HISTORY.filter { it.id > settings.changelogLastSeenId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissChangelog() {
        viewModelScope.launch { settingsRepository.markChangelogSeen(CURRENT_CHANGELOG_ID) }
    }

    /** Read here (rather than only in Settings) so the restyled list screen's theme picker can
     *  apply a theme immediately without navigating away. */
    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setThemeKey(key: String) {
        viewModelScope.launch { settingsRepository.setThemeKey(key) }
    }

    val bookings: StateFlow<List<Booking>> =
        combine(repository.observeAll(), activeFilter) { all, filter ->
            when (filter) {
                BookingFilter.ALL -> all
                BookingFilter.WEDDING_BAPTISM -> all.filter { it.type.isChurchSacrament }
                BookingFilter.DRONE -> all.filter { it.hasDrone }
                BookingFilter.RECEPTION -> all.filter { it.hasReception }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: BookingFilter) {
        activeFilter.value = filter
    }

    fun requestDelete(booking: Booking) {
        _pendingDelete.value = booking
    }

    fun dismissDeleteRequest() {
        _pendingDelete.value = null
    }

    /** Single delete, routed through the same path as a bulk one so both offer the same choice
     *  about the calendar. It used to delete the calendar events unconditionally, with nothing
     *  said - which is not something to do silently to an entry the user may share with others. */
    fun confirmDelete(alsoDeleteFromCalendar: Boolean) {
        val booking = _pendingDelete.value ?: return
        _pendingDelete.value = null
        deleteBookings(listOf(booking), alsoDeleteFromCalendar)
    }

    fun startSelection(booking: Booking) {
        _isSelectionMode.value = true
        _selectedIds.value = setOf(booking.id)
    }

    fun toggleSelection(booking: Booking) {
        val current = _selectedIds.value
        val next = if (booking.id in current) current - booking.id else current + booking.id
        _selectedIds.value = next
        // Emptying the selection leaves selection mode, so there is no state where the screen is
        // in "selection mode" with nothing selected and no obvious way out.
        if (next.isEmpty()) _isSelectionMode.value = false
    }

    /** Selects everything currently *visible*, which is what the filter chips make the user expect
     *  - "select all" while filtered to "Με Drone" must not quietly pick up the rest. */
    fun selectAllVisible() {
        _selectedIds.value = bookings.value.map { it.id }.toSet()
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
    }

    private fun selectedBookings(): List<Booking> {
        val ids = _selectedIds.value
        return bookings.value.filter { it.id in ids }
    }

    fun requestBulkDelete() {
        val selected = selectedBookings()
        if (selected.isEmpty()) return
        _pendingBulkDelete.value = BulkDeleteRequest(
            bookings = selected,
            withCalendarEvents = selected.count { it.churchCalendarEventId != null || it.receptionCalendarEventId != null },
            calendarPermissionGranted = CalendarHelper.hasCalendarPermissions(getApplication()),
        )
    }

    fun dismissBulkDelete() {
        _pendingBulkDelete.value = null
    }

    fun confirmBulkDelete(alsoDeleteFromCalendar: Boolean) {
        val request = _pendingBulkDelete.value ?: return
        _pendingBulkDelete.value = null
        clearSelection()
        deleteBookings(request.bookings, alsoDeleteFromCalendar)
    }

    fun dismissDeleteResult() {
        _lastDeleteResult.value = null
    }

    private fun deleteBookings(bookings: List<Booking>, alsoDeleteFromCalendar: Boolean) {
        if (bookings.isEmpty()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            var calendarDeleted = 0
            var calendarFailed = 0

            if (alsoDeleteFromCalendar) {
                // Calendar first: if the app rows went first and this then failed, the events would
                // be orphaned with nothing left pointing at them to try again.
                bookings.forEach { booking ->
                    listOfNotNull(booking.churchCalendarEventId, booking.receptionCalendarEventId)
                        .forEach { eventId ->
                            val removed = runCatching { CalendarHelper.deleteEvent(context, eventId) }.getOrDefault(false)
                            if (removed) calendarDeleted++ else calendarFailed++
                        }
                }
            }

            repository.deleteAll(bookings)
            _lastDeleteResult.value = BulkDeleteResult(
                deleted = bookings.size,
                calendarEventsDeleted = calendarDeleted,
                calendarEventsFailed = calendarFailed,
            )
        }
    }
}
