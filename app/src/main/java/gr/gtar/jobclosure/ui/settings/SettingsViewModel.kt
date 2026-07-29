package gr.gtar.jobclosure.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.data.AppSettings
import gr.gtar.jobclosure.data.MapsProvider
import gr.gtar.jobclosure.data.SettingsRepository
import gr.gtar.jobclosure.update.UpdateCheckResult
import gr.gtar.jobclosure.update.UpdateRepository
import gr.gtar.jobclosure.update.UpdateStatusHolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val updateStatus: StateFlow<UpdateCheckResult?> = UpdateStatusHolder.status

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

    fun checkForUpdateNow() {
        viewModelScope.launch {
            val token = repository.settings.first().gitHubToken
            UpdateStatusHolder.set(updateRepository.checkForUpdate(token))
        }
    }
}
