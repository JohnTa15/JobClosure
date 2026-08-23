package gr.gtar.jobclosure.ui.dagr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.dagr.DagrAccount
import gr.gtar.jobclosure.dagr.DagrFillReport
import gr.gtar.jobclosure.dagr.DagrFlightRequest
import gr.gtar.jobclosure.dagr.DagrOperatorProfile
import gr.gtar.jobclosure.dagr.DagrSecureStore
import gr.gtar.jobclosure.dagr.DagrVenue
import gr.gtar.jobclosure.data.BookingRepository
import gr.gtar.jobclosure.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DagrUiState(
    val isLoading: Boolean = true,
    val request: DagrFlightRequest? = null,
    val account: DagrAccount = DagrAccount(),
    /** Set once the login form has been auto-filled, so a page that keeps re-rendering does not
     *  retype the password on every load. */
    val hasAttemptedLogin: Boolean = false,
    val loginReport: DagrFillReport? = null,
    val fillReport: DagrFillReport? = null,
)

class DagrViewModel(
    application: Application,
    private val bookingRepository: BookingRepository,
    private val settingsRepository: SettingsRepository,
    private val bookingId: Long,
    private val venue: DagrVenue,
    private val coordinates: Pair<Double, Double>?,
) : AndroidViewModel(application) {

    private val secureStore = DagrSecureStore(application)
    private val _state = MutableStateFlow(DagrUiState())
    val state: StateFlow<DagrUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val booking = bookingRepository.getById(bookingId)
            val settings = settingsRepository.settings.first()
            _state.update {
                it.copy(
                    isLoading = false,
                    account = secureStore.load(),
                    request = booking?.let { loaded ->
                        DagrFlightRequest.from(
                            booking = loaded,
                            venue = venue,
                            coordinates = coordinates,
                            profile = DagrOperatorProfile(
                                operatorRegistration = settings.dagrOperatorRegistration,
                                pilotName = settings.dagrPilotName,
                                uasModel = settings.dagrUasModel,
                                maxAltitudeMeters = settings.dagrMaxAltitudeMeters,
                                radiusMeters = settings.dagrRadiusMeters,
                            ),
                        )
                    },
                )
            }
        }
    }

    fun onLoginAttempted(report: DagrFillReport) {
        _state.update { it.copy(hasAttemptedLogin = true, loginReport = report) }
    }

    fun onFilled(report: DagrFillReport) {
        _state.update { it.copy(fillReport = report) }
    }

    fun dismissReports() {
        _state.update { it.copy(loginReport = null, fillReport = null) }
    }
}
