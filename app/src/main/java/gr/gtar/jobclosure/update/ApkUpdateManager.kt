package gr.gtar.jobclosure.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/**
 * Downloads the update APK via the system DownloadManager (which shows its own progress/complete
 * notification) and launches the package installer directly once it's done, so updating happens
 * without ever having to leave the app or go through GitHub's UI.
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
     * Enqueues the download and calls [onInstallReady] with the finished file's content Uri.
     * [gitHubToken], if non-blank, is sent as a Bearer Authorization header - required to fetch a
     * release asset from a private repo via the api.github.com asset URL (plain unauthenticated
     * browser_download_url links don't work there).
     */
    fun downloadUpdate(context: Context, downloadUrl: String, gitHubToken: String, onInstallReady: (Uri) -> Unit) {
        val appContext = context.applicationContext
        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Ενημέρωση JobClosure")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
            .setMimeType("application/vnd.android.package-archive")
            .addRequestHeader("Accept", "application/octet-stream")
        if (gitHubToken.isNotBlank()) {
            request.addRequestHeader("Authorization", "Bearer $gitHubToken")
        }
        val downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (completedId == downloadId) {
                    appContext.unregisterReceiver(this)
                    downloadManager.getUriForDownloadedFile(downloadId)?.let(onInstallReady)
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
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
