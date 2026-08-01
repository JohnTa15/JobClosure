package gr.gtar.jobclosure.desktop.auth

import java.util.Properties

/**
 * A Google Cloud OAuth Client ID/Secret baked into this build (from GitHub Actions secrets, via
 * desktopApp/build.gradle.kts's generateVersionProperties task) - present only when the person
 * building the app configured GOOGLE_OAUTH_CLIENT_ID/GOOGLE_OAUTH_CLIENT_SECRET. When present,
 * SignInScreen skips asking the user to paste their own credentials and goes straight to a plain
 * "Sign in with Google" button; when absent (the default), it falls back to the manual-entry form.
 */
object EmbeddedGoogleCredentials {
    val clientId: String by lazy { readProperty("embeddedGoogleClientId") }
    val clientSecret: String by lazy { readProperty("embeddedGoogleClientSecret") }

    val isConfigured: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    private fun readProperty(key: String): String {
        val props = Properties()
        Thread.currentThread().contextClassLoader
            ?.getResourceAsStream("version.properties")
            ?.use { props.load(it) }
        return props.getProperty(key, "")
    }
}
