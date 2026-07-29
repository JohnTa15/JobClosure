package gr.gtar.jobclosure.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Process-wide holder for the last update check result, shared between the startup check
 *  (which posts the notification) and the Settings screen (which shows it and offers install). */
object UpdateStatusHolder {
    private val _status = MutableStateFlow<UpdateCheckResult?>(null)
    val status: StateFlow<UpdateCheckResult?> = _status

    fun set(result: UpdateCheckResult) {
        _status.value = result
    }
}
