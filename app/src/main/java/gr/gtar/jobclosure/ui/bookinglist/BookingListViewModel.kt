package gr.gtar.jobclosure.ui.bookinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.data.Booking
import gr.gtar.jobclosure.data.BookingRepository
import gr.gtar.jobclosure.data.BookingType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class BookingFilter(val label: String) {
    ALL("Όλα"),
    WEDDING_BAPTISM("Γάμοι/Βαφτίσεις"),
    DRONE("Με Drone"),
    RECEPTION("Με Δεξίωση"),
}

class BookingListViewModel(private val repository: BookingRepository) : ViewModel() {

    private val activeFilter = MutableStateFlow(BookingFilter.ALL)
    val filter: StateFlow<BookingFilter> = activeFilter

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
}
