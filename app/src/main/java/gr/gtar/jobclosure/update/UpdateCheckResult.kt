package gr.gtar.jobclosure.update

sealed interface UpdateCheckResult {
    /** [expectedSizeBytes] and [expectedSha256] come from the release asset and let the download be
     *  checked before it's handed to the package installer. Either may be absent (0 / null) on
     *  releases GitHub hasn't reported them for, in which case that check is simply skipped. */
    data class UpdateAvailable(
        val versionName: String,
        val downloadUrl: String,
        val expectedSizeBytes: Long = 0,
        val expectedSha256: String? = null,
    ) : UpdateCheckResult

    data object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}
