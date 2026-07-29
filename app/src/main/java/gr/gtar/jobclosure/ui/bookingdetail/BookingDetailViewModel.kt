package gr.gtar.jobclosure.ui.bookingdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.data.AppSettings
import gr.gtar.jobclosure.data.Booking
import gr.gtar.jobclosure.data.BookingRepository
import gr.gtar.jobclosure.data.SettingsRepository
import gr.gtar.jobclosure.network.DroneConditionsRepository
import gr.gtar.jobclosure.network.DroneConditionsResult
import gr.gtar.jobclosure.network.DroneZoneRepository
import gr.gtar.jobclosure.network.DroneZoneResult
import gr.gtar.jobclosure.network.TravelTimeRepository
import gr.gtar.jobclosure.network.TravelTimeResult
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class BookingDetailUiState(
    val booking: Booking? = null,
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
    val isLoadingTravelTimes: Boolean = false,
    val homeToChurch: TravelTimeResult? = null,
    val churchToReception: TravelTimeResult? = null,
    val isLoadingDroneConditions: Boolean = false,
    val churchDroneConditions: DroneConditionsResult? = null,
    val receptionDroneConditions: DroneConditionsResult? = null,
    val isLoadingDroneZones: Boolean = false,
    val churchDroneZone: DroneZoneResult? = null,
    val receptionDroneZone: DroneZoneResult? = null,
)

class BookingDetailViewModel(
    application: Application,
    private val bookingRepository: BookingRepository,
    private val settingsRepository: SettingsRepository,
    private val travelTimeRepository: TravelTimeRepository,
    private val droneConditionsRepository: DroneConditionsRepository,
    private val droneZoneRepository: DroneZoneRepository,
    private val bookingId: Long,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BookingDetailUiState())
    val uiState: StateFlow<BookingDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val booking = bookingRepository.getById(bookingId)
            val settings = settingsRepository.settings.first()
            _uiState.value = _uiState.value.copy(booking = booking, settings = settings, isLoading = false)
            if (booking != null) {
                refreshTravelTimes(booking, settings)
                if (booking.hasDrone) {
                    refreshDroneConditions(booking, settings)
                    refreshDroneZones(booking, settings)
                }
            }
        }
    }

    fun refreshTravelTimes() {
        val booking = _uiState.value.booking ?: return
        viewModelScope.launch {
            refreshTravelTimes(booking, _uiState.value.settings)
            if (booking.hasDrone) {
                refreshDroneConditions(booking, _uiState.value.settings)
                refreshDroneZones(booking, _uiState.value.settings)
            }
        }
    }

    private suspend fun refreshTravelTimes(booking: Booking, settings: AppSettings) {
        _uiState.value = _uiState.value.copy(isLoadingTravelTimes = true)

        val homeToChurchDeferred = viewModelScope.async {
            travelTimeRepository.getTravelTime(settings.homeAddress, booking.churchAddress, settings.mapsApiKey)
        }
        val churchToReceptionDeferred = viewModelScope.async {
            if (booking.hasReception && booking.receptionVenueAddress.isNotBlank()) {
                travelTimeRepository.getTravelTime(
                    booking.churchAddress,
                    booking.receptionVenueAddress,
                    settings.mapsApiKey,
                )
            } else {
                null
            }
        }
        val homeToChurch = homeToChurchDeferred.await()
        val churchToReception = churchToReceptionDeferred.await()

        _uiState.value = _uiState.value.copy(
            isLoadingTravelTimes = false,
            homeToChurch = homeToChurch,
            churchToReception = churchToReception,
        )
    }

    private suspend fun refreshDroneConditions(booking: Booking, settings: AppSettings) {
        _uiState.value = _uiState.value.copy(isLoadingDroneConditions = true)

        val churchDeferred = viewModelScope.async {
            droneConditionsRepository.getConditions(booking.churchAddress, settings.mapsApiKey)
        }
        val receptionDeferred = viewModelScope.async {
            if (booking.hasReception && booking.receptionVenueAddress.isNotBlank()) {
                droneConditionsRepository.getConditions(booking.receptionVenueAddress, settings.mapsApiKey)
            } else {
                null
            }
        }
        val church = churchDeferred.await()
        val reception = receptionDeferred.await()

        _uiState.value = _uiState.value.copy(
            isLoadingDroneConditions = false,
            churchDroneConditions = church,
            receptionDroneConditions = reception,
        )
    }

    private suspend fun refreshDroneZones(booking: Booking, settings: AppSettings) {
        _uiState.value = _uiState.value.copy(isLoadingDroneZones = true)

        val churchDeferred = viewModelScope.async {
            droneZoneRepository.getNearbyAirspaces(booking.churchAddress, settings.mapsApiKey, settings.openAipApiKey)
        }
        val receptionDeferred = viewModelScope.async {
            if (booking.hasReception && booking.receptionVenueAddress.isNotBlank()) {
                droneZoneRepository.getNearbyAirspaces(
                    booking.receptionVenueAddress,
                    settings.mapsApiKey,
                    settings.openAipApiKey,
                )
            } else {
                null
            }
        }
        val church = churchDeferred.await()
        val reception = receptionDeferred.await()

        _uiState.value = _uiState.value.copy(
            isLoadingDroneZones = false,
            churchDroneZone = church,
            receptionDroneZone = reception,
        )
    }
}
