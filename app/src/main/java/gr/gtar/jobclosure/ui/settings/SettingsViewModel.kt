package gr.gtar.jobclosure.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.data.AppSettings
import gr.gtar.jobclosure.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setHomeAddress(address: String) {
        viewModelScope.launch { repository.setHomeAddress(address) }
    }

    fun setMapsApiKey(key: String) {
        viewModelScope.launch { repository.setMapsApiKey(key) }
    }

    fun setReminderMinutesBefore(minutes: Int) {
        viewModelScope.launch { repository.setReminderMinutesBefore(minutes) }
    }
}
