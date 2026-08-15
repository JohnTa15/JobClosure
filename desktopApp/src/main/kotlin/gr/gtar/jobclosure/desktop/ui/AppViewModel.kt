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

enum class BookingFilter(val label: String) {
    ALL("Όλα"),
    WEDDING_BAPTISM("Γάμοι/Βαφτίσεις"),
    DRONE("Με Drone"),
    RECEPTION("Με Δεξίωση"),
}

data class AppUiState(
    val isLoading: Boolean = true,
    val settings: DesktopSettings = DesktopSettings(),
    val calendars: kotlin.collections.List<GCalCalendarListEntry> = emptyList(),
    val bookings: kotlin.collections.List<Booking> = emptyList(),
    val screen: Screen = Screen.SignIn,
    /** What the spinner is currently waiting on. A bare spinner cannot distinguish "talking to
     *  Google" from "waiting for you to finish in the browser", and sign-in can sit on the latter
     *  for minutes - long enough to look like the app has hung. */
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val pendingConflicts: kotlin.collections.List<Booking> = emptyList(),
    val pendingSave: Booking? = null,
    val unseenChangelogEntries: kotlin.collections.List<ChangelogEntry> = emptyList(),
    val isShowingChangelogHistory: Boolean = false,
    val updateCheckResult: UpdateCheckResult? = null,
    val isCheckingForUpdate: Boolean = false,
    val filter: BookingFilter = BookingFilter.ALL,
    val pendingDelete: Booking? = null,
) {
    /** [bookings] narrowed down by the active list filter chip. */
    val filteredBookings: kotlin.collections.List<Booking>
        get() = when (filter) {
            BookingFilter.ALL -> bookings
            BookingFilter.WEDDING_BAPTISM -> bookings.filter { it.type.isChurchSacrament }
            BookingFilter.DRONE -> bookings.filter { it.hasDrone }
            BookingFilter.RECEPTION -> bookings.filter { it.hasReception }
        }
}

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
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    statusMessage = "Άνοιξε το πρόγραμμα περιήγησης - ολοκλήρωσε εκεί τη σύνδεση " +
                        "με τον λογαριασμό Google. Η αναμονή λήγει σε 3 λεπτά.",
                )
            }
            runCatching { authManager.signIn(clientId, clientSecret) }
                .onSuccess { token ->
                    _state.update { it.copy(statusMessage = "Φόρτωση ημερολογίων...") }
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
                    _state.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = null,
                            errorMessage = "Η σύνδεση απέτυχε: ${error.message}",
                        )
                    }
                }
        }
    }

    private fun loadCalendars() {
        scope.launch {
            runCatching { repository.listWritableCalendars() }
                .onSuccess { calendars ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = null,
                            calendars = calendars,
                            settings = currentSettings,
                        )
                    }
                }
                .onFailure { error -> reportFailure("Αποτυχία φόρτωσης ημερολογίων", error) }
        }
    }

    /**
     * Google invalidates the refresh token when the consent screen is still in Testing (after 7
     * days), when access is revoked, or when the OAuth client is changed - and it says so with
     * `invalid_grant`. Every call then fails identically, so without catching it here the app just
     * repeats "αποτυχία φόρτωσης" forever with no way out. Send the user back to sign-in instead,
     * which is the only thing that actually fixes it.
     */
    private fun reportFailure(prefix: String, error: Throwable) {
        val text = "${error.message}"
        if (text.contains("invalid_grant", ignoreCase = true) || text.contains("invalid_token", ignoreCase = true)) {
            cachedAccessToken = null
            currentSettings = currentSettings.copy(refreshToken = "")
            DesktopSettingsStore.save(currentSettings)
            _state.update {
                it.copy(
                    isLoading = false,
                    statusMessage = null,
                    calendars = emptyList(),
                    settings = currentSettings,
                    screen = Screen.SignIn,
                    errorMessage = "Η σύνδεση με τη Google έληξε - συνδέσου ξανά. (Αν η εφαρμογή " +
                        "είναι σε κατάσταση Testing στο Google Cloud, αυτό συμβαίνει κάθε 7 ημέρες " +
                        "μέχρι να την κάνεις Publish.)",
                )
            }
            return
        }
        _state.update { it.copy(isLoading = false, statusMessage = null, errorMessage = "$prefix: $text") }
    }

    fun selectCalendar(calendarId: String) {
        currentSettings = currentSettings.copy(calendarId = calendarId)
        DesktopSettingsStore.save(currentSettings)
        _state.update { it.copy(settings = currentSettings, screen = Screen.List) }
        loadBookings()
    }

    fun loadBookings() {
        val calendarId = currentSettings.calendarId
        // Also bails out with no session: save/delete call this straight after their own request,
        // and if that request was what revealed an expired token, retrying here would only bury
        // the "sign in again" message under a second, less useful failure.
        if (calendarId.isBlank() || currentSettings.refreshToken.isBlank()) return
        scope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.listBookings(calendarId) }
                .onSuccess { bookings -> _state.update { it.copy(isLoading = false, bookings = bookings) } }
                .onFailure { error -> reportFailure("Αποτυχία φόρτωσης κρατήσεων", error) }
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
                .onFailure { error -> reportFailure("Αποτυχία αποθήκευσης", error) }
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
                .onFailure { error -> reportFailure("Αποτυχία διαγραφής", error) }
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

    /**
     * Writes the settings-screen text fields in one go. They used to save on every keystroke, which
     * rewrote the settings file per character and pushed a state update through the whole screen;
     * an explicit "save and apply" is both cheaper and clearer about when a change has taken.
     */
    fun saveSettings(gitHubToken: String, dronePartnerEmail: String) {
        currentSettings = currentSettings.copy(
            gitHubToken = gitHubToken.trim(),
            dronePartnerEmail = dronePartnerEmail.trim(),
        )
        DesktopSettingsStore.save(currentSettings)
        _state.update { it.copy(settings = currentSettings) }
    }

    /** Re-opens the calendar picker on the sign-in screen, without dropping the Google session. */
    fun changeCalendar() {
        if (currentSettings.refreshToken.isBlank()) {
            _state.update { it.copy(screen = Screen.SignIn, calendars = emptyList()) }
            return
        }
        _state.update {
            it.copy(screen = Screen.SignIn, isLoading = true, errorMessage = null, statusMessage = "Φόρτωση ημερολογίων...")
        }
        loadCalendars()
    }

    /**
     * Forgets the Google session (but keeps the OAuth client details, which are a property of the
     * machine rather than the account) and returns to sign-in. Without this there was no way back
     * to the sign-in screen once a calendar had been picked - a dead end whenever the stored
     * refresh token stopped working.
     */
    fun signOut() {
        cachedAccessToken = null
        cachedAccessTokenExpiry = Instant.EPOCH
        currentSettings = currentSettings.copy(refreshToken = "", calendarId = "")
        DesktopSettingsStore.save(currentSettings)
        _state.update {
            it.copy(
                settings = currentSettings,
                screen = Screen.SignIn,
                calendars = emptyList(),
                bookings = emptyList(),
                isLoading = false,
                statusMessage = null,
                errorMessage = null,
            )
        }
    }

    fun setThemeKey(key: String) {
        currentSettings = currentSettings.copy(themeKey = key)
        DesktopSettingsStore.save(currentSettings)
        _state.update { it.copy(settings = currentSettings) }
    }

    fun setFilter(filter: BookingFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun requestDelete(booking: Booking) {
        _state.update { it.copy(pendingDelete = booking) }
    }

    fun dismissDeleteRequest() {
        _state.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val booking = _state.value.pendingDelete ?: return
        _state.update { it.copy(pendingDelete = null) }
        deleteBooking(booking)
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
