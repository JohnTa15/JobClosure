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

    fun confirmDelete() {
        val booking = _pendingDelete.value ?: return
        _pendingDelete.value = null
        viewModelScope.launch {
            val context = getApplication<Application>()
            booking.churchCalendarEventId?.let { CalendarHelper.deleteEvent(context, it) }
            booking.receptionCalendarEventId?.let { CalendarHelper.deleteEvent(context, it) }
            repository.delete(booking)
        }
    }
}
