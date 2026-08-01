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
