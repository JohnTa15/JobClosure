package gr.gtar.jobclosure.update

import gr.gtar.jobclosure.BuildConfig
import gr.gtar.jobclosure.network.GitHubReleaseApi

/**
 * Checks the rolling "debug-latest" GitHub Release (published by the CI workflow on every push)
 * against the running app's own build number, and hands back the APK asset's authenticated
 * download URL if a newer build is available. Works unauthenticated for a public repo, or with a
 * user-supplied GitHub token (Settings) for a private one.
 */
class UpdateRepository(private val api: GitHubReleaseApi) {

    suspend fun checkForUpdate(gitHubToken: String): UpdateCheckResult {
        return try {
            val authHeader = gitHubToken.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
            val release = api.getReleaseByTag(OWNER, REPO, TAG, authHeader)
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return UpdateCheckResult.Error("Δεν βρέθηκε APK στην τελευταία έκδοση")

            // CI republishes this rolling release on every push, deleting the old assets before
            // uploading the new ones. Downloading during that window yields a truncated APK that
            // the package installer rejects with a bare "There's a problem with the app file", so
            // an asset GitHub hasn't finished writing is treated as "no update yet".
            if (apkAsset.state.isNotBlank() && apkAsset.state != "uploaded") {
                return UpdateCheckResult.Error("Η νέα έκδοση ανεβαίνει ακόμα - δοκίμασε ξανά σε λίγο")
            }

            val remoteBuild = extractBuildNumber(release.name ?: release.tagName)
            val localBuild = extractBuildNumber(BuildConfig.VERSION_NAME)

            if (remoteBuild != null && localBuild != null && remoteBuild > localBuild) {
                UpdateCheckResult.UpdateAvailable(
                    versionName = "1.0.$remoteBuild",
                    downloadUrl = apkAsset.url,
                    expectedSizeBytes = apkAsset.size,
                    expectedSha256 = apkAsset.digest?.removePrefix("sha256:"),
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
