package gr.gtar.jobclosure.update

import gr.gtar.jobclosure.BuildConfig
import gr.gtar.jobclosure.network.GitHubReleaseApi

/**
 * Checks the rolling "debug-latest" GitHub Release (published by the CI workflow on every push)
 * against the running app's own build number, and hands back the APK download URL if a newer
 * build is available.
 */
class UpdateRepository(private val api: GitHubReleaseApi) {

    suspend fun checkForUpdate(): UpdateCheckResult {
        return try {
            val release = api.getReleaseByTag(OWNER, REPO, TAG)
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return UpdateCheckResult.Error("Δεν βρέθηκε APK στην τελευταία έκδοση")

            val remoteBuild = extractBuildNumber(release.name ?: release.tagName)
            val localBuild = extractBuildNumber(BuildConfig.VERSION_NAME)

            if (remoteBuild != null && localBuild != null && remoteBuild > localBuild) {
                UpdateCheckResult.UpdateAvailable(
                    versionName = "1.0.$remoteBuild",
                    downloadUrl = apkAsset.browserDownloadUrl,
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Αποτυχία ελέγχου ενημέρωσης: ${e.message ?: "άγνωστο σφάλμα"}")
        }
    }

    private fun extractBuildNumber(text: String): Int? =
        Regex("""1\.0\.(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull()

    private companion object {
        const val OWNER = "JohnTa15"
        const val REPO = "JobClosure"
        const val TAG = "debug-latest"
    }
}
