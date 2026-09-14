package gr.gtar.jobclosure.ui.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gr.gtar.jobclosure.backup.BackupFile
import gr.gtar.jobclosure.backup.BackupManager
import gr.gtar.jobclosure.data.ActivityEntry
import gr.gtar.jobclosure.data.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** Drives the history screen: what the app has done, and the backups it has written. */
class ActivityViewModel(
    application: Application,
    private val activityRepository: ActivityRepository,
    private val backupManager: BackupManager,
) : AndroidViewModel(application) {

    val entries: StateFlow<List<ActivityEntry>> =
        activityRepository.observeRecent()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _backups = MutableStateFlow<List<BackupFile>>(emptyList())
    val backups: StateFlow<List<BackupFile>> = _backups

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking

    /** Last thing that happened, shown once under the buttons. Null until something does. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        refreshBackups()
    }

    fun refreshBackups() {
        viewModelScope.launch { _backups.value = backupManager.listBackups() }
    }

    fun backupNow() {
        viewModelScope.launch {
            _isWorking.value = true
            val created = backupManager.backupNow()
            _isWorking.value = false
            _message.value = if (created == null) {
                "Το αντίγραφο δεν γράφτηκε."
            } else {
                "Κρατήθηκε αντίγραφο με ${created.bookingCount} δουλειές."
            }
            refreshBackups()
        }
    }

    fun restore(file: File) {
        viewModelScope.launch {
            _isWorking.value = true
            val result = backupManager.restore(file)
            _isWorking.value = false
            _message.value = when {
                result.restored == 0 && result.alreadyPresent > 0 ->
                    "Όλες οι δουλειές του αντιγράφου υπάρχουν ήδη - δεν προστέθηκε τίποτα."
                result.restored == 0 -> "Δεν βρέθηκε τίποτα για επαναφορά."
                else -> "Επαναφέρθηκαν ${result.restored} δουλειές (${result.alreadyPresent} υπήρχαν ήδη)."
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { activityRepository.clear() }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
