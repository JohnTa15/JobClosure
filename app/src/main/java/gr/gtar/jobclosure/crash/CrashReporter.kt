package gr.gtar.jobclosure.crash

import android.content.Context
import android.os.Build
import gr.gtar.jobclosure.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One captured crash, newest first when listed. */
data class CrashReport(
    val file: File,
    val timestamp: Instant,
    val summary: String,
) {
    val text: String get() = runCatching { file.readText() }.getOrDefault("")
}

/**
 * Captures uncaught exceptions to disk so a crash that happened out in the field - phone in hand at
 * a church, no laptop, no adb - can still be read and sent afterwards.
 *
 * Writing to a file and re-raising is deliberately all that happens at crash time: the process is
 * already dying, so anything slower (a network call, a database write) is likely to be killed
 * halfway and lose the report entirely. Sending is left to the next launch, when the app is healthy.
 */
object CrashReporter {

    private const val DIRECTORY = "crashes"
    private const val MAX_REPORTS = 20
    private val fileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneId.systemDefault())

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(appContext, thread, throwable) }
            // Hand back to whatever was there so Android still shows its dialog and records the
            // crash normally - swallowing it would leave the app frozen instead of restarting.
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun reports(context: Context): List<CrashReport> {
        val dir = directory(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            .orEmpty()
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                CrashReport(
                    file = file,
                    timestamp = Instant.ofEpochMilli(file.lastModified()),
                    summary = runCatching {
                        file.useLines { lines -> lines.firstOrNull { it.startsWith("Exception:") } }
                            ?.removePrefix("Exception:")
                            ?.trim()
                    }.getOrNull().orEmpty().ifBlank { "Άγνωστο σφάλμα" },
                )
            }
    }

    fun clear(context: Context) {
        directory(context).listFiles()?.forEach { it.delete() }
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val dir = directory(context).apply { mkdirs() }
        val stackTrace = StringWriter().also { writer ->
            PrintWriter(writer).use { throwable.printStackTrace(it) }
        }.toString()

        val report = buildString {
            appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Thread: ${thread.name}")
            appendLine("Exception: ${throwable::class.java.simpleName}: ${throwable.message ?: "-"}")
            appendLine()
            append(stackTrace)
        }

        File(dir, "crash-${fileTimestampFormatter.format(Instant.now())}.txt").writeText(report)
        trimOldest(dir)
    }

    /** Keeps the newest [MAX_REPORTS]; a crash loop would otherwise fill the device unbounded. */
    private fun trimOldest(dir: File) {
        val files = dir.listFiles().orEmpty().sortedByDescending { it.lastModified() }
        files.drop(MAX_REPORTS).forEach { it.delete() }
    }

    private fun directory(context: Context) = File(context.filesDir, DIRECTORY)
}
