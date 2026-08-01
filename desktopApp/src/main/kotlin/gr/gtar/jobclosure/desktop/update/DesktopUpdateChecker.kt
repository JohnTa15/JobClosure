package gr.gtar.jobclosure.desktop.update

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val versionName: String, val downloadUrl: String) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

@Serializable
private data class GitHubRelease(
    val name: String? = null,
    @SerialName("tag_name") val tagName: String? = null,
    val assets: List<GitHubReleaseAsset> = emptyList(),
)

@Serializable
private data class GitHubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

/**
 * Checks the same rolling "debug-latest" GitHub Release the Android app checks (same repo, same
 * tag), picking the installer for whichever OS this is currently running on. Only *notifies* - it
 * hands back a browser URL to the release page/asset, it doesn't download or install anything
 * itself; the user still does a normal manual install, same as before, just without having to
 * remember to go check for one.
 */
class DesktopUpdateChecker(private val httpClient: HttpClient) {

    suspend fun checkForUpdate(gitHubToken: String): UpdateCheckResult {
        return try {
            val response = httpClient.get("https://api.github.com/repos/$OWNER/$REPO/releases/tags/$TAG") {
                header("Accept", "application/vnd.github+json")
                if (gitHubToken.isNotBlank()) header("Authorization", "Bearer $gitHubToken")
            }
            if (!response.status.isSuccess()) {
                return UpdateCheckResult.Error("Αποτυχία ελέγχου ενημέρωσης: HTTP ${response.status.value}")
            }
            val release: GitHubRelease = response.body()
            val installerSuffix = installerSuffixForCurrentOs()
            val asset = release.assets.firstOrNull { it.name.endsWith(installerSuffix) }
                ?: return UpdateCheckResult.Error("Δεν βρέθηκε installer ($installerSuffix) στην τελευταία έκδοση")

            val remoteBuild = extractBuildNumber(release.name ?: release.tagName ?: "")
            val localBuild = extractBuildNumber(AppVersion.current)
            if (remoteBuild != null && localBuild != null && remoteBuild > localBuild) {
                UpdateCheckResult.UpdateAvailable("1.0.$remoteBuild", asset.browserDownloadUrl)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Αποτυχία ελέγχου ενημέρωσης: ${e.message ?: "άγνωστο σφάλμα"}")
        }
    }

    private fun installerSuffixForCurrentOs(): String {
        val os = System.getProperty("os.name")?.lowercase().orEmpty()
        return when {
            os.contains("win") -> ".msi"
            os.contains("mac") -> ".dmg"
            else -> ".deb"
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
