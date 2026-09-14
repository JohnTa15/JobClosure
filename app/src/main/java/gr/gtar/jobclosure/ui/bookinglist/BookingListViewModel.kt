package gr.gtar.jobclosure.ui.bookinglist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.data.ActivityAction
import gr.gtar.jobclosure.data.ActivityRepository
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
import java.time.LocalDate

/** The bookings a bulk delete is about to remove, held while the user confirms. */
data class BulkDeleteRequest(val bookings: List<Booking>)

data class BulkDeleteResult(val deleted: Int)

enum class BookingFilter(val label: String) {
    ALL("Όλα"),
    WEDDING_BAPTISM("Γάμοι/Βαφτίσεις"),
    DRONE("Με Drone"),
    RECEPTION("Με Δεξίωση"),
}

/** Which part of the calendar to show. Separate from [BookingFilter] because it answers a
 *  different question - "what kind of job" versus "when" - and the two get combined. */
enum class BookingPeriod(val label: String) {
    ALL("Όλες"),
    UPCOMING("Επόμενες"),
    PAST("Περασμένες"),
}

/** Newest first is the default: after importing a decade of history, the jobs worth looking at
 *  are the recent ones, and they used to sit at the very bottom of a list starting in 2011. */
enum class BookingSort(val label: String) {
    NEWEST_FIRST("Νεότερα πρώτα"),
    OLDEST_FIRST("Παλαιότερα πρώτα"),
}

class BookingListViewModel(
    application: Application,
    private val repository: BookingRepository,
    private val settingsRepository: SettingsRepository,
    private val activityRepository: ActivityRepository,
) : AndroidViewModel(application) {

    private val activeFilter = MutableStateFlow(BookingFilter.ALL)
    val filter: StateFlow<BookingFilter> = activeFilter

    private val activePeriod = MutableStateFlow(BookingPeriod.ALL)
    val period: StateFlow<BookingPeriod> = activePeriod

    private val activeSort = MutableStateFlow(BookingSort.NEWEST_FIRST)
    val sort: StateFlow<BookingSort> = activeSort

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

    /**
     * What the list shows: type, period and order, applied in that sequence. The repository query
     * is ordered oldest-first (it is also what the overlap checks read), so the order the user
     * sees is decided here rather than in the database.
     */
    val bookings: StateFlow<List<Booking>> =
        combine(repository.observeAll(), activeFilter, activePeriod, activeSort) { all, filter, period, sort ->
            val byType = when (filter) {
                BookingFilter.ALL -> all
                BookingFilter.WEDDING_BAPTISM -> all.filter { it.type.isChurchSacrament }
                BookingFilter.DRONE -> all.filter { it.hasDrone }
                BookingFilter.RECEPTION -> all.filter { it.hasReception }
            }
            // A job happening today stays under "Επόμενες" all day: it is still ahead of the user
            // until it is over, and dropping it out of the list at its start time would hide the
            // one entry they are most likely to open.
            val today = LocalDate.now()
            val byPeriod = when (period) {
                BookingPeriod.ALL -> byType
                BookingPeriod.UPCOMING -> byType.filter { !it.ceremonyStart.toLocalDate().isBefore(today) }
                BookingPeriod.PAST -> byType.filter { it.ceremonyStart.toLocalDate().isBefore(today) }
            }
            when (sort) {
                BookingSort.NEWEST_FIRST -> byPeriod.sortedByDescending { it.ceremonyStart }
                BookingSort.OLDEST_FIRST -> byPeriod.sortedBy { it.ceremonyStart }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: BookingFilter) {
        activeFilter.value = filter
    }

    fun setPeriod(period: BookingPeriod) {
        activePeriod.value = period
    }

    fun setSort(sort: BookingSort) {
        activeSort.value = sort
    }

    /** Back to the default view - one press instead of putting three chips back by hand. */
    fun resetView() {
        activeFilter.value = BookingFilter.ALL
        activePeriod.value = BookingPeriod.ALL
        activeSort.value = BookingSort.NEWEST_FIRST
    }

    fun requestDelete(booking: Booking) {
        _pendingDelete.value = booking
    }

    fun dismissDeleteRequest() {
        _pendingDelete.value = null
    }

    /** Single delete, routed through the same path as a bulk one so the two cannot drift apart. */
    fun confirmDelete() {
        val booking = _pendingDelete.value ?: return
        _pendingDelete.value = null
        deleteBookings(listOf(booking))
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
        _pendingBulkDelete.value = BulkDeleteRequest(bookings = selected)
    }

    fun dismissBulkDelete() {
        _pendingBulkDelete.value = null
    }

    fun confirmBulkDelete() {
        val request = _pendingBulkDelete.value ?: return
        _pendingBulkDelete.value = null
        clearSelection()
        deleteBookings(request.bookings)
    }

    fun dismissDeleteResult() {
        _lastDeleteResult.value = null
    }

    /** Removes the bookings from the app only. The device calendar is never written to here - what
     *  is in the user's calendar is theirs, and may be the copy other people are looking at. */
    private fun deleteBookings(bookings: List<Booking>) {
        if (bookings.isEmpty()) return
        viewModelScope.launch {
            repository.deleteAll(bookings)
            bookings.forEach { booking ->
                activityRepository.log(
                    action = ActivityAction.BOOKING_DELETED,
                    subject = booking.title.ifBlank { booking.type.displayName },
                    details = booking.ceremonyStart.toLocalDate().toString(),
                )
            }
            _lastDeleteResult.value = BulkDeleteResult(deleted = bookings.size)
        }
    }
}
