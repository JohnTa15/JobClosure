package gr.gtar.jobclosure.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
        }
    }
}
