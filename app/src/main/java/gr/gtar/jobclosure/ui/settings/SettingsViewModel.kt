package gr.gtar.jobclosure.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.crash.CrashReport
import gr.gtar.jobclosure.crash.CrashReportSender
import gr.gtar.jobclosure.crash.CrashReporter
import gr.gtar.jobclosure.crash.CrashSendResult
import gr.gtar.jobclosure.data.AppSettings
import gr.gtar.jobclosure.data.MapsProvider
import gr.gtar.jobclosure.data.SettingsRepository
import gr.gtar.jobclosure.network.PlaceSearchRepository
import gr.gtar.jobclosure.update.UpdateCheckResult
import gr.gtar.jobclosure.update.UpdateRepository
import gr.gtar.jobclosure.update.UpdateStatusHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import gr.gtar.jobclosure.dagr.DagrAccount
import gr.gtar.jobclosure.dagr.DagrSecureStore

class SettingsViewModel(
    application: Application,
    private val repository: SettingsRepository,
    private val updateRepository: UpdateRepository,
    private val crashReportSender: CrashReportSender,
    val placeSearchRepository: PlaceSearchRepository,
) : AndroidViewModel(application) {

    private val dagrSecureStore = DagrSecureStore(application)

    /** The DAGR sign-in, read straight from the Keystore-backed store rather than from DataStore -
     *  see DagrSecureStore for why it is kept apart from the rest of settings. */
    private val _dagrAccount = MutableStateFlow(dagrSecureStore.load())
    val dagrAccount: StateFlow<DagrAccount> = _dagrAccount.asStateFlow()

    fun saveDagrAccount(username: String, password: String) {
        val account = DagrAccount(username.trim(), password)
        dagrSecureStore.save(account)
        _dagrAccount.value = account
    }

    fun clearDagrAccount() {
        dagrSecureStore.clear()
        _dagrAccount.value = DagrAccount()
    }

    fun setDagrProfile(
        operatorRegistration: String,
        pilotName: String,
        uasModel: String,
        maxAltitudeMeters: Int,
        radiusMeters: Int,
    ) {
        viewModelScope.launch {
            repository.setDagrProfile(
                operatorRegistration.trim(),
                pilotName.trim(),
                uasModel.trim(),
                maxAltitudeMeters,
                radiusMeters,
            )
        }
    }

    private val _crashReports = MutableStateFlow(CrashReporter.reports(application))
    val crashReports: StateFlow<List<CrashReport>> = _crashReports.asStateFlow()

    private val _crashSendStatus = MutableStateFlow<String?>(null)
    val crashSendStatus: StateFlow<String?> = _crashSendStatus.asStateFlow()

    fun refreshCrashReports() {
        _crashReports.value = CrashReporter.reports(getApplication())
    }

    fun clearCrashReports() {
        CrashReporter.clear(getApplication())
        _crashSendStatus.value = null
        refreshCrashReports()
    }

    /** Sends the most recent crash - the one the user just hit, and the only one they can speak to. */
    fun sendLatestCrashReport() {
        val report = _crashReports.value.firstOrNull() ?: return
        viewModelScope.launch {
            _crashSendStatus.value = "Αποστολή..."
            val token = repository.settings.first().gitHubToken
            _crashSendStatus.value = when (val result = crashReportSender.send(report, token)) {
                is CrashSendResult.Sent -> "Στάλθηκε: ${result.url}"
                CrashSendResult.AlreadyReported -> "Έχει ήδη αναφερθεί αυτό το σφάλμα."
                is CrashSendResult.Failed -> result.message
            }
        }
    }

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val updateStatus: StateFlow<UpdateCheckResult?> = UpdateStatusHolder.status

    /**
     * The stored settings, read straight from DataStore. [settings] is a StateFlow seeded with a
     * placeholder [AppSettings], so `settings.first()` hands back those defaults synchronously
     * whenever this ViewModel is young enough that DataStore hasn't emitted yet - and since a fresh
     * ViewModel is built every time Settings is opened, that placeholder is exactly what the form
     * used to prefill itself with, silently resetting the maps provider to OpenStreetMap and
     * blanking the API key. Collecting the repository flow instead suspends until the real values
     * arrive.
     */
    suspend fun currentSettings(): AppSettings = repository.settings.first()

    fun setHomeAddress(address: String) {
        viewModelScope.launch { repository.setHomeAddress(address) }
    }

    fun setMapsApiKey(key: String) {
        viewModelScope.launch { repository.setMapsApiKey(key) }
    }

    fun setMapsProvider(provider: MapsProvider) {
        viewModelScope.launch { repository.setMapsProvider(provider) }
    }

    fun setReminderMinutesBefore(minutes: Int) {
        viewModelScope.launch { repository.setReminderMinutesBefore(minutes) }
    }

    fun setDronePartnerEmail(email: String) {
        viewModelScope.launch { repository.setDronePartnerEmail(email) }
    }

    fun setGitHubToken(token: String) {
        viewModelScope.launch { repository.setGitHubToken(token) }
    }

    fun setUseNewDesign(enabled: Boolean) {
        viewModelScope.launch { repository.setUseNewDesign(enabled) }
    }

    fun setThemeKey(key: String) {
        viewModelScope.launch { repository.setThemeKey(key) }
    }

    fun checkForUpdateNow() {
        viewModelScope.launch {
            val token = repository.settings.first().gitHubToken
            UpdateStatusHolder.set(updateRepository.checkForUpdate(token))
        }
    }
}
