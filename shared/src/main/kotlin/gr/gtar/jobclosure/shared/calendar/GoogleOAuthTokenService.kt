package gr.gtar.jobclosure.shared.calendar

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import kotlinx.serialization.Serializable

@Serializable
data class GoogleTokenResponse(
    val access_token: String,
    val expires_in: Int,
    val refresh_token: String? = null,
    val token_type: String = "Bearer",
)

/**
 * Talks to Google's OAuth2 token endpoint. The actual browser/redirect part of the "installed
 * app" flow is platform-specific (opens a system browser, catches a local redirect) and lives in
 * the desktop app; this part - exchanging a code or refresh token for an access token - is plain
 * HTTP and fully portable.
 */
class GoogleOAuthTokenService(private val httpClient: HttpClient) {

    suspend fun exchangeAuthorizationCode(
        clientId: String,
        clientSecret: String,
        code: String,
        redirectUri: String,
        codeVerifier: String,
    ): GoogleTokenResponse = httpClient.submitForm(
        url = TOKEN_ENDPOINT,
        formParameters = Parameters.build {
            append("client_id", clientId)
            append("client_secret", clientSecret)
            append("code", code)
            append("code_verifier", codeVerifier)
            append("grant_type", "authorization_code")
            append("redirect_uri", redirectUri)
        },
    ).body()

    suspend fun refreshAccessToken(
        clientId: String,
        clientSecret: String,
        refreshToken: String,
    ): GoogleTokenResponse = httpClient.submitForm(
        url = TOKEN_ENDPOINT,
        formParameters = Parameters.build {
            append("client_id", clientId)
            append("client_secret", clientSecret)
            append("refresh_token", refreshToken)
            append("grant_type", "refresh_token")
        },
    ).body()

    companion object {
        private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        const val CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar"
    }
}
