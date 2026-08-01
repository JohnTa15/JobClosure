package gr.gtar.jobclosure.desktop.ui

import gr.gtar.jobclosure.desktop.auth.DesktopSettings
import gr.gtar.jobclosure.desktop.auth.DesktopSettingsStore
import gr.gtar.jobclosure.desktop.auth.EmbeddedGoogleCredentials
import gr.gtar.jobclosure.desktop.auth.GoogleAuthManager
import gr.gtar.jobclosure.desktop.update.DesktopUpdateChecker
import gr.gtar.jobclosure.desktop.update.UpdateCheckResult
import gr.gtar.jobclosure.shared.calendar.GCalCalendarListEntry
import gr.gtar.jobclosure.shared.changelog.CHANGELOG_HISTORY
import gr.gtar.jobclosure.shared.changelog.ChangelogEntry
import gr.gtar.jobclosure.shared.changelog.CURRENT_CHANGELOG_ID
import gr.gtar.jobclosure.shared.calendar.GoogleCalendarRepository
import gr.gtar.jobclosure.shared.calendar.GoogleOAuthTokenService
import gr.gtar.jobclosure.shared.model.Booking
import gr.gtar.jobclosure.shared.model.findConflicts
import gr.gtar.jobclosure.shared.network.HttpClientFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinInstant
import java.time.Instant

sealed interface Screen {
    data object SignIn : Screen
    data object List : Screen
    data class Edit(val booking: Booking, val isNew: Boolean) : Screen
    data object Settings : Screen
}

data class AppUiState(
    val isLoading: Boolean = true,
    val settings: DesktopSettings = DesktopSettings(),
    val calendars: kotlin.collections.List<GCalCalendarListEntry> = emptyList(),
    val bookings: kotlin.collections.List<Booking> = emptyList(),
    val screen: Screen = Screen.SignIn,
    val errorMessage: String? = null,
    val pendingConflicts: kotlin.collections.List<Booking> = emptyList(),
    val pendingSave: Booking? = null,
    val unseenChangelogEntries: kotlin.collections.List<ChangelogEntry> = emptyList(),
    val isShowingChangelogHistory: Boolean = false,
    val updateCheckResult: UpdateCheckResult? = null,
    val isCheckingForUpdate: Boolean = false,
)

/**
 * Holds all desktop app state and talks to Google Calendar through the shared [GoogleCalendarRepository] -
 * the same sync backend the Android app writes to, so a booking made on either device shows up on
 * the other via Google's own calendar sync, with no server of this app's own involved.
 */
class AppViewModel(private val scope: CoroutineScope) {
    private val httpClient = HttpClientFactory.create()
    private val tokenService = GoogleOAuthTokenService(httpClient)
    private val authManager = GoogleAuthManager(tokenService)

    private var currentSettings: DesktopSettings = DesktopSettingsStore.load()
    private var cachedAccessToken: String? = null
    private var cachedAccessTokenExpiry: Instant = Instant.EPOCH

    private val repository = GoogleCalendarRepository(httpClient) { getAccessToken() }
    private val updateChecker = DesktopUpdateChecker(httpClient)

    private val _state = MutableStateFlow(
        AppUiState(
            settings = currentSettings,
            isLoading = false,
            unseenChangelogEntries = CHANGELOG_HISTORY.filter { it.id > currentSettings.changelogLastSeenId },
        ),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        if (currentSettings.refreshToken.isNotBlank() && currentSettings.calendarId.isNotBlank()) {
            _state.update { it.copy(screen = Screen.List) }
            loadBookings()
        }
        checkForUpdate()
    }

    fun updateSettingsFields(clientId: String, clientSecret: String, dronePartnerEmail: String) {
        currentSettings = currentSettings.copy(
            clientId = clientId,
            clientSecret = clientSecret,
            dronePartnerEmail = dronePartnerEmail,
        )
        DesktopSettingsStore.save(currentSettings)
        _state.update { it.copy(settings = currentSettings) }
    }

    fun signIn() {
        // A build with embedded credentials (see EmbeddedGoogleCredentials) never needs the user
        // to have typed their own - falls back to those only when nothing's been entered.
        val clientId = currentSettings.clientId.ifBlank { EmbeddedGoogleCredentials.clientId }
        val clientSecret = currentSettings.clientSecret.ifBlank { EmbeddedGoogleCredentials.clientSecret }
        if (clientId.isBlank() || clientSecret.isBlank()) {
            _state.update { it.copy(errorMessage = "Συμπληρώστε πρώτα το Client ID και το Client Secret.") }
            return
        }
        scope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { authManager.signIn(clientId, clientSecret) }
                .onSuccess { token ->
                    cachedAccessToken = token.access_token
                    cachedAccessTokenExpiry = Instant.now().plusSeconds((token.expires_in - 60).toLong().coerceAtLeast(30))
                    currentSettings = currentSettings.copy(
                        clientId = clientId,
                        clientSecret = clientSecret,
                        refreshToken = token.refresh_token ?: currentSettings.refreshToken,
                    )
                    DesktopSettingsStore.save(currentSettings)
                    loadCalendars()
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = "Η σύνδεση απέτυχε: ${error.message}") }
                }
        }
    }

    private fun loadCalendars() {
        scope.launch {
            runCatching { repository.listWritableCalendars() }
                .onSuccess { calendars ->
                    _state.update { it.copy(isLoading = false, calendars = calendars, settings = currentSettings) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = "Αποτυχία φόρτωσης ημερολογίων: ${error.message}") }
                }
        }
    }

    fun selectCalendar(calendarId: String) {
        currentSettings = currentSettings.copy(calendarId = calendarId)
        DesktopSettingsStore.save(currentSettings)
        _state.update { it.copy(settings = currentSettings, screen = Screen.List) }
        loadBookings()
    }

    fun loadBookings() {
        val calendarId = currentSettings.calendarId
        if (calendarId.isBlank()) return
        scope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.listBookings(calendarId) }
                .onSuccess { bookings -> _state.update { it.copy(isLoading = false, bookings = bookings) } }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = "Αποτυχία φόρτωσης κρατήσεων: ${error.message}") }
                }
        }
    }

    fun startNewBooking() {
        val blank = Booking(
            title = "",
            type = gr.gtar.jobclosure.shared.model.BookingType.WEDDING,
            ceremonyStart = Instant.now().plusSeconds(86_400).toKotlinInstant(),
        )
        _state.update { it.copy(screen = Screen.Edit(blank, isNew = true)) }
    }

    fun startEditBooking(booking: Booking) {
        _state.update { it.copy(screen = Screen.Edit(booking, isNew = false)) }
    }

    fun cancelEdit() {
        _state.update { it.copy(screen = Screen.List, pendingConflicts = emptyList(), pendingSave = null) }
    }

    fun dismissConflicts() {
        _state.update { it.copy(pendingConflicts = emptyList(), pendingSave = null) }
    }

    /** Checks for conflicts first; if none (or the caller already confirmed), saves to Google Calendar. */
    fun saveBooking(booking: Booking, ignoreConflicts: Boolean = false) {
        val calendarId = currentSettings.calendarId
        if (calendarId.isBlank()) return

        if (!ignoreConflicts) {
            val conflicts = findConflicts(booking, _state.value.bookings)
            if (conflicts.isNotEmpty()) {
                _state.update { it.copy(pendingConflicts = conflicts, pendingSave = booking) }
                return
            }
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, pendingConflicts = emptyList(), pendingSave = null) }
            runCatching { repository.saveBooking(calendarId, booking) }
                .onSuccess { _state.update { it.copy(isLoading = false, screen = Screen.List) } }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = "Αποτυχία αποθήκευσης: ${error.message}") }
                }
            loadBookings()
        }
    }

    fun deleteBooking(booking: Booking) {
        val calendarId = currentSettings.calendarId
        if (calendarId.isBlank()) return
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { repository.deleteBooking(calendarId, booking) }
                .onSuccess { _state.update { it.copy(isLoading = false, screen = Screen.List) } }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = "Αποτυχία διαγραφής: ${error.message}") }
                }
            loadBookings()
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun openSettings() {
        _state.update { it.copy(screen = Screen.Settings) }
    }

    fun closeSettings() {
        _state.update { it.copy(screen = Screen.List) }
    }

    fun setGitHubToken(token: String) {
        currentSettings = currentSettings.copy(gitHubToken = token)
        DesktopSettingsStore.save(currentSettings)
        _state.update { it.copy(settings = currentSettings) }
    }

    fun setDronePartnerEmail(email: String) {
        currentSettings = currentSettings.copy(dronePartnerEmail = email)
        DesktopSettingsStore.save(currentSettings)
        _state.update { it.copy(settings = currentSettings) }
    }

    fun checkForUpdate() {
        scope.launch {
            _state.update { it.copy(isCheckingForUpdate = true) }
            val result = updateChecker.checkForUpdate(currentSettings.gitHubToken)
            _state.update { it.copy(isCheckingForUpdate = false, updateCheckResult = result) }
        }
    }

    fun dismissUpdateNotice() {
        _state.update { it.copy(updateCheckResult = null) }
    }

    fun dismissChangelog() {
        currentSettings = currentSettings.copy(changelogLastSeenId = CURRENT_CHANGELOG_ID)
        DesktopSettingsStore.save(currentSettings)
        _state.update { it.copy(unseenChangelogEntries = emptyList(), isShowingChangelogHistory = false) }
    }

    fun showChangelogHistory() {
        _state.update { it.copy(unseenChangelogEntries = CHANGELOG_HISTORY, isShowingChangelogHistory = true) }
    }

    private suspend fun getAccessToken(): String {
        val cached = cachedAccessToken
        if (cached != null && Instant.now().isBefore(cachedAccessTokenExpiry)) return cached

        val settings = currentSettings
        check(settings.refreshToken.isNotBlank()) { "Δεν έχει γίνει σύνδεση με το Google Calendar." }
        val token = authManager.refresh(settings.clientId, settings.clientSecret, settings.refreshToken)
        cachedAccessToken = token.access_token
        cachedAccessTokenExpiry = Instant.now().plusSeconds((token.expires_in - 60).toLong().coerceAtLeast(30))
        return token.access_token
    }
}
