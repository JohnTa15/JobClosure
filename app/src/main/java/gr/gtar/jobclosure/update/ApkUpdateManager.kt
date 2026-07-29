package gr.gtar.jobclosure.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

sealed interface DownloadResult {
    data class Success(val apkUri: Uri) : DownloadResult
    data class Error(val message: String) : DownloadResult
}

/**
 * Downloads the update APK and launches the package installer directly once it's done, so
 * updating happens without ever having to leave the app or go through GitHub's UI.
 *
 * Uses OkHttp instead of Android's DownloadManager on purpose: a private repo's asset URL
 * (api.github.com) responds with a redirect to a signed blob-storage URL, and DownloadManager
 * resends the same custom "Authorization" header to that redirected URL too - which the storage
 * backend then rejects, since a bearer token has no meaning there. The download then just fails
 * silently (DownloadManager's own completion broadcast still fires, but getUriForDownloadedFile
 * returns null), leaving the UI stuck showing "downloading" forever with no error. OkHttp's
 * redirect handling drops the Authorization header automatically whenever a redirect points at a
 * different host, which is exactly what's needed here - and it lets failures be reported back to
 * the caller instead of disappearing.
 */
object ApkUpdateManager {

    private const val FILE_NAME = "jobclosure-update.apk"

    fun canInstallUnknownApps(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )
        context.startActivity(intent)
    }

    /**
     * [gitHubToken], if non-blank, is sent as a Bearer Authorization header on the initial
     * request - required to fetch a release asset from a private repo via the api.github.com
     * asset URL (plain unauthenticated browser_download_url links don't work there).
     */
    suspend fun downloadUpdate(context: Context, downloadUrl: String, gitHubToken: String): DownloadResult =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder()
                    .url(downloadUrl)
                    .header("Accept", "application/octet-stream")
                if (gitHubToken.isNotBlank()) {
                    requestBuilder.header("Authorization", "Bearer $gitHubToken")
                }

                OkHttpClient().newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext DownloadResult.Error(
                            "Αποτυχία λήψης: HTTP ${response.code}" +
                                if (response.code == 401 || response.code == 404) {
                                    " - έλεγξε το GitHub token στις Ρυθμίσεις"
                                } else {
                                    ""
                                },
                        )
                    }
                    val body = response.body
                        ?: return@withContext DownloadResult.Error("Άδειο αρχείο λήψης")

                    val appContext = context.applicationContext
                    val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: appContext.filesDir
                    val file = File(dir, FILE_NAME)
                    body.byteStream().use { input ->
                        FileOutputStream(file).use { output -> input.copyTo(output) }
                    }

                    val uri = FileProvider.getUriForFile(
                        appContext,
                        "${appContext.packageName}.fileprovider",
                        file,
                    )
                    DownloadResult.Success(uri)
                }
            } catch (e: Exception) {
                DownloadResult.Error("Αποτυχία λήψης: ${e.message ?: "άγνωστο σφάλμα"}")
            }
        }

    fun promptInstall(context: Context, apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
