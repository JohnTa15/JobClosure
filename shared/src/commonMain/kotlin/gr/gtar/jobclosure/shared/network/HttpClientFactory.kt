package gr.gtar.jobclosure.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * No engine is named explicitly here (unlike a JVM-only client, which would pass CIO) - each
 * target only has one Ktor client engine on its classpath (CIO for jvm, Darwin for iOS, wired in
 * shared/build.gradle.kts's per-target source sets), so Ktor resolves the right one automatically
 * and this same code works unchanged on every platform.
 */
object HttpClientFactory {
    fun create(): HttpClient = HttpClient {
        // Without this, a 4xx/5xx from Google (e.g. "Calendar API not enabled", or an OAuth
        // token-endpoint error) doesn't throw - the error JSON then deserializes with
        // ignoreUnknownKeys into a default-empty response (GoogleCalendarDto's `items` defaults),
        // and sign-in silently acts as if the user has no calendars. With it, such responses throw
        // and the Google error text shows up in the UI instead of disappearing.
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
    }
}
