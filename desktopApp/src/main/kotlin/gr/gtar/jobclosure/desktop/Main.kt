package gr.gtar.jobclosure.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import gr.gtar.jobclosure.desktop.ui.AppViewModel
import gr.gtar.jobclosure.desktop.ui.BookingEditScreen
import gr.gtar.jobclosure.desktop.ui.BookingListScreen
import gr.gtar.jobclosure.desktop.ui.Screen
import gr.gtar.jobclosure.desktop.ui.SignInScreen
import gr.gtar.jobclosure.shared.changelog.CURRENT_CHANGELOG_ITEMS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

fun main() = application {
    val scope = remember { CoroutineScope(SupervisorJob()) }
    val viewModel = remember { AppViewModel(scope) }
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "JobClosure",
        state = rememberWindowState(width = 960.dp, height = 720.dp),
    ) {
        MaterialTheme {
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
                    )
                    is Screen.Edit -> BookingEditScreen(
                        booking = screen.booking,
                        isNew = screen.isNew,
                        conflicts = state.pendingConflicts,
                        onSave = { booking, ignoreConflicts -> viewModel.saveBooking(booking, ignoreConflicts) },
                        onDismissConflicts = { viewModel.dismissConflicts() },
                        onDelete = { viewModel.deleteBooking(it) },
                        onCancel = { viewModel.cancelEdit() },
                    )
                }
            }
            SnackbarHost(hostState = snackbarHostState)

            if (state.showChangelog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissChangelog() },
                    icon = { Icon(Icons.Filled.NewReleases, contentDescription = null) },
                    title = { Text("Τι νέο υπάρχει") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CURRENT_CHANGELOG_ITEMS.forEach { item ->
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
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissChangelog() }) { Text("Το κατάλαβα") }
                    },
                )
            }
        }
    }
}
