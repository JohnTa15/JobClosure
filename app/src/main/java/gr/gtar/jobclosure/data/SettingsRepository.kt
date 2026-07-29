package gr.gtar.jobclosure.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val homeAddress: String = "",
    val mapsApiKey: String = "",
    val reminderMinutesBefore: Int = 120,
    val defaultCalendarId: Long? = null,
    val dronePartnerEmail: String = "",
    val gitHubToken: String = "",
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val HOME_ADDRESS = stringPreferencesKey("home_address")
        val MAPS_API_KEY = stringPreferencesKey("maps_api_key")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes_before")
        val DEFAULT_CALENDAR_ID = longPreferencesKey("default_calendar_id")
        val DRONE_PARTNER_EMAIL = stringPreferencesKey("drone_partner_email")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            homeAddress = prefs[Keys.HOME_ADDRESS] ?: "",
            mapsApiKey = prefs[Keys.MAPS_API_KEY] ?: "",
            reminderMinutesBefore = prefs[Keys.REMINDER_MINUTES] ?: 120,
            defaultCalendarId = prefs[Keys.DEFAULT_CALENDAR_ID],
            dronePartnerEmail = prefs[Keys.DRONE_PARTNER_EMAIL] ?: "",
            gitHubToken = prefs[Keys.GITHUB_TOKEN] ?: "",
        )
    }

    suspend fun setHomeAddress(address: String) {
        context.dataStore.edit { it[Keys.HOME_ADDRESS] = address }
    }

    suspend fun setMapsApiKey(key: String) {
        context.dataStore.edit { it[Keys.MAPS_API_KEY] = key }
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
}
