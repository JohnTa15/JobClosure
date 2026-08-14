package gr.gtar.jobclosure.desktop.auth

import com.sun.net.httpserver.HttpServer
import gr.gtar.jobclosure.shared.calendar.GoogleOAuthTokenService
import gr.gtar.jobclosure.shared.calendar.GoogleTokenResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Drives Google's "installed app" OAuth2 flow with PKCE: opens the user's system browser to
 * Google's consent screen, catches the redirect on a local loopback HTTP server (no fixed port
 * needed, and nothing but this machine ever sees the redirect), then exchanges the resulting code
 * for tokens via [GoogleOAuthTokenService]. `access_type=offline` + `prompt=consent` guarantee a
 * refresh_token comes back, so the user only has to do this once per machine.
 */
class GoogleAuthManager(private val tokenService: GoogleOAuthTokenService) {

    suspend fun signIn(clientId: String, clientSecret: String): GoogleTokenResponse {
        val codeVerifier = randomUrlSafeString(64)
        val codeChallenge = codeChallengeS256(codeVerifier)
        val state = randomUrlSafeString(16)

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val redirectUri = "http://127.0.0.1:${server.address.port}"
        val codeDeferred = CompletableDeferred<String>()

        server.createContext("/") { exchange ->
            val params = parseQuery(exchange.requestURI.query.orEmpty())
            val code = params["code"]
            val responseHtml = if (code != null && params["state"] == state) {
                codeDeferred.complete(code)
                SUCCESS_HTML
            } else {
                codeDeferred.completeExceptionally(IllegalStateException(describeAuthError(params["error"])))
                ERROR_HTML
            }
            val bytes = responseHtml.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()

        return try {
            val authUrl = buildString {
                append(GoogleOAuthTokenService.AUTH_ENDPOINT)
                append("?client_id=").append(clientId.urlEncode())
                append("&redirect_uri=").append(redirectUri.urlEncode())
                append("&response_type=code")
                append("&scope=").append(GoogleOAuthTokenService.CALENDAR_SCOPE.urlEncode())
                append("&code_challenge=").append(codeChallenge)
                append("&code_challenge_method=S256")
                append("&access_type=offline")
                append("&prompt=consent")
                append("&state=").append(state)
            }
            Desktop.getDesktop().browse(URI(authUrl))

            // Without a timeout, closing the browser tab or cancelling on Google's consent screen
            // without it ever redirecting back (as opposed to clicking "Deny", which does redirect
            // with an error and completes codeDeferred exceptionally) leaves this suspended
            // forever - the sign-in window would then be stuck showing a loading spinner until the
            // app is force-quit.
            val code = try {
                withTimeout(SIGN_IN_TIMEOUT_MILLIS) { codeDeferred.await() }
            } catch (e: TimeoutCancellationException) {
                throw IllegalStateException("Η σύνδεση ακυρώθηκε ή έληξε ο χρόνος αναμονής - δοκίμασε ξανά.")
            }
            tokenService.exchangeAuthorizationCode(
                clientId = clientId,
                clientSecret = clientSecret,
                code = code,
                redirectUri = redirectUri,
                codeVerifier = codeVerifier,
            )
        } finally {
            server.stop(0)
        }
    }

    /**
     * Google reports these as bare codes in the redirect, and "access_denied" in particular is far
     * more often a consent screen still in Testing without this account listed as a tester than an
     * actual refusal - a distinction worth spelling out, since the two need opposite fixes.
     */
    private fun describeAuthError(code: String?): String = when (code) {
        "access_denied" ->
            "Η Google απέρριψε τη σύνδεση. Αν η εφαρμογή είναι σε κατάσταση Testing, πρόσθεσε " +
                "τον λογαριασμό σου στους Test users - ή δημοσίευσε την (Publish app), που " +
                "σταματά και τη λήξη της σύνδεσης κάθε 7 ημέρες."
        "admin_policy_enforced" ->
            "Ο διαχειριστής του λογαριασμού Google μπλοκάρει την πρόσβαση σε αυτή την εφαρμογή."
        "redirect_uri_mismatch" ->
            "Το OAuth client πρέπει να είναι τύπου \"Desktop app\" - ένα \"Web application\" " +
                "client δεν δέχεται τη διεύθυνση επιστροφής που χρησιμοποιεί η εφαρμογή."
        null -> "Άγνωστο σφάλμα σύνδεσης"
        else -> "Σφάλμα σύνδεσης από τη Google: $code"
    }

    /** Returns a fresh access token, refreshing it via the stored refresh token. */
    suspend fun refresh(clientId: String, clientSecret: String, refreshToken: String): GoogleTokenResponse =
        tokenService.refreshAccessToken(clientId, clientSecret, refreshToken)

    private fun randomUrlSafeString(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun codeChallengeS256(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

    private fun parseQuery(query: String): Map<String, String> =
        query.split("&")
            .filter { it.isNotBlank() }
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                val key = parts.getOrNull(0) ?: return@mapNotNull null
                val value = parts.getOrNull(1) ?: ""
                URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
            }
            .toMap()

    private companion object {
        const val SIGN_IN_TIMEOUT_MILLIS = 3 * 60 * 1000L
        const val SUCCESS_HTML = "<html><body><h2>Η σύνδεση με το Google Calendar ολοκληρώθηκε.</h2>" +
            "<p>Μπορείτε να κλείσετε αυτή την καρτέλα και να επιστρέψετε στο JobClosure.</p></body></html>"
        const val ERROR_HTML = "<html><body><h2>Η σύνδεση απέτυχε.</h2>" +
            "<p>Επιστρέψτε στο JobClosure και δοκιμάστε ξανά.</p></body></html>"
    }
}
