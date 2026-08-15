package gr.gtar.jobclosure.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import gr.gtar.jobclosure.desktop.ui.AppViewModel
import gr.gtar.jobclosure.desktop.ui.BookingEditScreen
import gr.gtar.jobclosure.desktop.ui.BookingListScreen
import gr.gtar.jobclosure.desktop.ui.DesktopSettingsScreen
import gr.gtar.jobclosure.desktop.ui.Screen
import gr.gtar.jobclosure.desktop.ui.SignInScreen
import gr.gtar.jobclosure.desktop.ui.theme.NewUiColors
import gr.gtar.jobclosure.desktop.update.UpdateCheckResult
import gr.gtar.jobclosure.desktop.util.openInBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

fun main() = application {
    val scope = remember { CoroutineScope(SupervisorJob()) }
    val viewModel = remember { AppViewModel(scope) }
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // The restyled UI is dark-only (see NewUiColors) - give Material3's own components (dialogs,
    // snackbars, menus) a matching dark scheme so nothing white flashes around them.
    val darkColors = darkColorScheme(
        primary = NewUiColors.onGroundMuted,
        onPrimary = NewUiColors.ground,
        background = NewUiColors.ground,
        onBackground = NewUiColors.onGround,
        surface = NewUiColors.surface,
        onSurface = NewUiColors.onGround,
        surfaceVariant = NewUiColors.surfaceLow,
        onSurfaceVariant = NewUiColors.onGroundMuted,
        outline = NewUiColors.outline,
        error = Color(0xFFFF6B6B),
        onError = NewUiColors.ground,
    )

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Notifies once per fresh check result - doesn't download or install anything itself, just
    // points at the release so a manual install can happen the same way it always has.
    LaunchedEffect(state.updateCheckResult) {
        val result = state.updateCheckResult
        if (result is UpdateCheckResult.UpdateAvailable) {
            val action = snackbarHostState.showSnackbar(
                message = "Διαθέσιμη νέα έκδοση: ${result.versionName}",
                actionLabel = "Λήψη",
                duration = SnackbarDuration.Long,
            )
            if (action == SnackbarResult.ActionPerformed) {
                openInBrowser(result.downloadUrl)
            }
            viewModel.dismissUpdateNotice()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "JobClosure",
        state = rememberWindowState(width = 960.dp, height = 720.dp),
    ) {
        MaterialTheme(colorScheme = darkColors) {
            Surface(modifier = Modifier) {
                when (val screen = state.screen) {
                    is Screen.SignIn -> SignInScreen(
                        state = state,
                        onSaveCredentials = { clientId, clientSecret, dronePartnerEmail ->
                            viewModel.updateSettingsFields(clientId, clientSecret, dronePartnerEmail)
                        },
                        onSignIn = { viewModel.signIn() },
                        onSelectCalendar = { viewModel.selectCalendar(it) },
                    )
                    is Screen.List -> BookingListScreen(
                        state = state,
                        onAddBooking = { viewModel.startNewBooking() },
                        onOpenBooking = { viewModel.startEditBooking(it) },
                        onOpenSettings = { viewModel.openSettings() },
                        onRefresh = { viewModel.loadBookings() },
                        onSetFilter = { viewModel.setFilter(it) },
                        onSetThemeKey = { viewModel.setThemeKey(it) },
                        onRequestDelete = { viewModel.requestDelete(it) },
                        onDismissDeleteRequest = { viewModel.dismissDeleteRequest() },
                        onConfirmDelete = { viewModel.confirmDelete() },
                    )
                    is Screen.Settings -> DesktopSettingsScreen(
                        state = state,
                        onSetThemeKey = { viewModel.setThemeKey(it) },
                        onSaveSettings = { gitHubToken, dronePartnerEmail ->
                            viewModel.saveSettings(gitHubToken, dronePartnerEmail)
                        },
                        onChangeCalendar = { viewModel.changeCalendar() },
                        onSignOut = { viewModel.signOut() },
                        onCheckForUpdate = { viewModel.checkForUpdate() },
                        onShowChangelogHistory = { viewModel.showChangelogHistory() },
                        onBack = { viewModel.closeSettings() },
                    )
                    is Screen.Edit -> BookingEditScreen(
                        booking = screen.booking,
                        isNew = screen.isNew,
                        conflicts = state.pendingConflicts,
                        themeKey = state.settings.themeKey,
                        onSave = { booking, ignoreConflicts -> viewModel.saveBooking(booking, ignoreConflicts) },
                        onDismissConflicts = { viewModel.dismissConflicts() },
                        onDelete = { viewModel.deleteBooking(it) },
                        onCancel = { viewModel.cancelEdit() },
                    )
                }
            }
            // Anchored to the bottom of the window: left free-standing it laid out at the top-left
            // corner, covering the screen header it was supposed to sit clear of.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(16.dp))
            }

            if (state.unseenChangelogEntries.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissChangelog() },
                    icon = { Icon(Icons.Filled.NewReleases, contentDescription = null) },
                    title = { Text(if (state.isShowingChangelogHistory) "Ιστορικό ενημερώσεων" else "Τι νέο υπάρχει") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                        ) {
                            state.unseenChangelogEntries.sortedByDescending { it.id }.forEach { entry ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (state.unseenChangelogEntries.size > 1) {
                                        Text(
                                            "Ενημέρωση #${entry.id}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                    entry.items.forEach { item ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                            )
                                            Text(item, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissChangelog() }) { Text("Το κατάλαβα") }
                    },
                )
            }
        }
    }
}
