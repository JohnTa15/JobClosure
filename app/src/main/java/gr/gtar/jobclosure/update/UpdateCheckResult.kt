package gr.gtar.jobclosure.update

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val versionName: String, val downloadUrl: String) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}
