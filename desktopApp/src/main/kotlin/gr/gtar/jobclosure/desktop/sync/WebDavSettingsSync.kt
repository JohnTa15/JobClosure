package gr.gtar.jobclosure.desktop.sync

import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64
import java.util.Properties

/** What a pull found, or why it found nothing. */
sealed interface WebDavResult {
    data class Loaded(val values: Map<String, String>) : WebDavResult
    data object NotFound : WebDavResult
    data class Failed(val message: String) : WebDavResult
}

/**
 * Reads and writes a small settings file on a WebDAV share (Nextcloud, in practice).
 *
 * This exists so the credentials each install would otherwise ask for - the Google OAuth client,
 * the drone partner's address, the GitHub token - can be typed once and then travel with the user,
 * rather than being re-entered on every machine.
 *
 * It is deliberately best-effort and never blocks anything: the local settings file remains the
 * source of truth, and a share that is unreachable (Pi powered off, away from the network, expired
 * password) leaves the app working exactly as it did before. That is the whole reason this is a
 * pair of explicit buttons rather than a background sync - a silent failure the user cannot see is
 * worse than one they chose to trigger.
 */
class WebDavSettingsSync(
    private val baseUrl: String,
    private val username: String,
    private val password: String,
) {

    private val fileUrl: String
        get() = baseUrl.trimEnd('/') + "/" + FILE_NAME

    fun pull(): WebDavResult {
        val connection = open(fileUrl, "GET") ?: return WebDavResult.Failed(INVALID_URL)
        return try {
            when (val code = connection.responseCode) {
                in 200..299 -> {
                    val props = Properties()
                    connection.inputStream.use { props.load(it) }
                    WebDavResult.Loaded(props.entries.associate { it.key.toString() to it.value.toString() })
                }
                HttpURLConnection.HTTP_NOT_FOUND -> WebDavResult.NotFound
                HttpURLConnection.HTTP_UNAUTHORIZED -> WebDavResult.Failed(
                    "Ο διακομιστής απέρριψε τα στοιχεία - έλεγξε όνομα χρήστη και app password.",
                )
                else -> WebDavResult.Failed("Ο διακομιστής απάντησε HTTP $code")
            }
        } catch (e: Exception) {
            WebDavResult.Failed(unreachable(e))
        } finally {
            connection.disconnect()
        }
    }

    fun push(values: Map<String, String>): WebDavResult {
        val connection = open(fileUrl, "PUT") ?: return WebDavResult.Failed(INVALID_URL)
        return try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            val props = Properties().apply { values.forEach { (key, value) -> setProperty(key, value) } }
            connection.outputStream.use { props.store(it, "JobClosure shared settings") }

            val code = connection.responseCode
            when {
                code in 200..299 -> WebDavResult.Loaded(values)
                code == HttpURLConnection.HTTP_UNAUTHORIZED -> WebDavResult.Failed(
                    "Ο διακομιστής απέρριψε τα στοιχεία - έλεγξε όνομα χρήστη και app password.",
                )
                // A share whose parent folder does not exist answers 409; saying so beats "HTTP 409".
                code == HttpURLConnection.HTTP_CONFLICT -> WebDavResult.Failed(
                    "Ο φάκελος δεν υπάρχει στον διακομιστή - φτιάξε τον πρώτα ή διόρθωσε το URL.",
                )
                else -> WebDavResult.Failed("Ο διακομιστής απάντησε HTTP $code")
            }
        } catch (e: Exception) {
            WebDavResult.Failed(unreachable(e))
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, method: String): HttpURLConnection? = try {
        (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            val credentials = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
            setRequestProperty("Authorization", "Basic $credentials")
        }
    } catch (e: Exception) {
        null
    }

    private fun unreachable(e: Exception) =
        "Ο διακομιστής δεν απάντησε: ${e.message ?: "άγνωστο σφάλμα"}"

    private companion object {
        const val FILE_NAME = "jobclosure-settings.properties"
        const val TIMEOUT_MILLIS = 10_000
        const val INVALID_URL = "Μη έγκυρο URL WebDAV."
    }
}
