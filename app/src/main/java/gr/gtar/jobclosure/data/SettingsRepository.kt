package gr.gtar.jobclosure.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Which service resolves addresses/routes: Google Maps (needs an API key) or the free,
 *  key-less OpenStreetMap stack (Nominatim for geocoding, OSRM for routing). */
enum class MapsProvider {
    GOOGLE,
    OPENSTREETMAP,
}

data class AppSettings(
    val homeAddress: String = "",
    val mapsApiKey: String = "",
    val mapsProvider: MapsProvider = MapsProvider.OPENSTREETMAP,
    val reminderMinutesBefore: Int = 120,
    val defaultCalendarId: Long? = null,
    val dronePartnerEmail: String = "",
    val gitHubToken: String = "",
    val changelogLastSeenId: Int = 0,
    // Restyle + theme switcher (design_handoff_theme_switcher): off by default so the app keeps
    // its classic look until the user opts in from Settings; themeKey only matters once it's on.
    val useNewDesign: Boolean = false,
    val themeKey: String = "nocturne",
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val HOME_ADDRESS = stringPreferencesKey("home_address")
        val MAPS_API_KEY = stringPreferencesKey("maps_api_key")
        val MAPS_PROVIDER = stringPreferencesKey("maps_provider")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes_before")
        val DEFAULT_CALENDAR_ID = longPreferencesKey("default_calendar_id")
        val DRONE_PARTNER_EMAIL = stringPreferencesKey("drone_partner_email")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val CHANGELOG_LAST_SEEN_ID = intPreferencesKey("changelog_last_seen_id")
        val USE_NEW_DESIGN = booleanPreferencesKey("use_new_design")
        val THEME_KEY = stringPreferencesKey("theme_key")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val mapsApiKey = prefs[Keys.MAPS_API_KEY] ?: ""
        AppSettings(
            homeAddress = prefs[Keys.HOME_ADDRESS] ?: "",
            mapsApiKey = mapsApiKey,
            // No explicit choice yet: default to Google only if a key from before this setting
            // existed is already sitting there, otherwise OpenStreetMap works immediately with no
            // setup at all.
            mapsProvider = prefs[Keys.MAPS_PROVIDER]?.let { stored ->
                runCatching { MapsProvider.valueOf(stored) }.getOrNull()
            } ?: if (mapsApiKey.isNotBlank()) MapsProvider.GOOGLE else MapsProvider.OPENSTREETMAP,
            reminderMinutesBefore = prefs[Keys.REMINDER_MINUTES] ?: 120,
            defaultCalendarId = prefs[Keys.DEFAULT_CALENDAR_ID],
            dronePartnerEmail = prefs[Keys.DRONE_PARTNER_EMAIL] ?: "",
            gitHubToken = prefs[Keys.GITHUB_TOKEN] ?: "",
            changelogLastSeenId = prefs[Keys.CHANGELOG_LAST_SEEN_ID] ?: 0,
            useNewDesign = prefs[Keys.USE_NEW_DESIGN] ?: false,
            themeKey = prefs[Keys.THEME_KEY] ?: "nocturne",
        )
    }

    suspend fun setHomeAddress(address: String) {
        context.dataStore.edit { it[Keys.HOME_ADDRESS] = address }
    }

    suspend fun setMapsApiKey(key: String) {
        context.dataStore.edit { it[Keys.MAPS_API_KEY] = key }
    }

    suspend fun setMapsProvider(provider: MapsProvider) {
        context.dataStore.edit { it[Keys.MAPS_PROVIDER] = provider.name }
    }

    suspend fun setReminderMinutesBefore(minutes: Int) {
        context.dataStore.edit { it[Keys.REMINDER_MINUTES] = minutes }
    }

    suspend fun setDefaultCalendarId(calendarId: Long) {
        context.dataStore.edit { it[Keys.DEFAULT_CALENDAR_ID] = calendarId }
    }

    suspend fun setDronePartnerEmail(email: String) {
        context.dataStore.edit { it[Keys.DRONE_PARTNER_EMAIL] = email }
    }

    suspend fun setGitHubToken(token: String) {
        context.dataStore.edit { it[Keys.GITHUB_TOKEN] = token }
    }

    suspend fun markChangelogSeen(id: Int) {
        context.dataStore.edit { it[Keys.CHANGELOG_LAST_SEEN_ID] = id }
    }

    suspend fun setUseNewDesign(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_NEW_DESIGN] = enabled }
    }

    suspend fun setThemeKey(key: String) {
        context.dataStore.edit { it[Keys.THEME_KEY] = key }
    }
}
