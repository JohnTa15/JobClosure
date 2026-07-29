package gr.gtar.jobclosure.ui.bookinglist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.calendar.CalendarHelper
import gr.gtar.jobclosure.data.Booking
import gr.gtar.jobclosure.data.BookingRepository
import gr.gtar.jobclosure.data.BookingType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
) : AndroidViewModel(application) {

    private val activeFilter = MutableStateFlow(BookingFilter.ALL)
    val filter: StateFlow<BookingFilter> = activeFilter

    private val _pendingDelete = MutableStateFlow<Booking?>(null)
    val pendingDelete: StateFlow<Booking?> = _pendingDelete

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
