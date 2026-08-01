package gr.gtar.jobclosure.desktop.auth

import java.io.File
import java.util.Properties

/**
 * Plain-properties-file settings store, kept in the user's home directory. There's no Android
 * DataStore equivalent needed here - this is the desktop app's only user, on their own machine, so
 * a simple file is enough and keeps the desktop app dependency-free of any extra persistence
 * library.
 */
data class DesktopSettings(
    val clientId: String = "",
    val clientSecret: String = "",
    val refreshToken: String = "",
    val calendarId: String = "",
    val dronePartnerEmail: String = "",
    val changelogLastSeenId: Int = 0,
    // Only needed while the JobClosure repo stays private - lets the update check authenticate
    // the same way the Android app's does. Not needed at all once/if the repo goes public.
    val gitHubToken: String = "",
)

object DesktopSettingsStore {
    private val configDir = File(System.getProperty("user.home"), ".jobclosure")
    private val configFile = File(configDir, "config.properties")

    fun load(): DesktopSettings {
        if (!configFile.exists()) return DesktopSettings()
        val props = Properties()
        configFile.inputStream().use { props.load(it) }
        return DesktopSettings(
            clientId = props.getProperty("clientId", ""),
            clientSecret = props.getProperty("clientSecret", ""),
            refreshToken = props.getProperty("refreshToken", ""),
            calendarId = props.getProperty("calendarId", ""),
            dronePartnerEmail = props.getProperty("dronePartnerEmail", ""),
            changelogLastSeenId = props.getProperty("changelogLastSeenId", "0").toIntOrNull() ?: 0,
            gitHubToken = props.getProperty("gitHubToken", ""),
        )
    }

    fun save(settings: DesktopSettings) {
        configDir.mkdirs()
        val props = Properties()
        props.setProperty("clientId", settings.clientId)
        props.setProperty("clientSecret", settings.clientSecret)
        props.setProperty("refreshToken", settings.refreshToken)
        props.setProperty("calendarId", settings.calendarId)
        props.setProperty("dronePartnerEmail", settings.dronePartnerEmail)
        props.setProperty("changelogLastSeenId", settings.changelogLastSeenId.toString())
        props.setProperty("gitHubToken", settings.gitHubToken)
        configFile.outputStream().use { props.store(it, "JobClosure desktop settings") }
    }
}
