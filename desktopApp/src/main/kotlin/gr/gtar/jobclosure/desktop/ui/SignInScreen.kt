package gr.gtar.jobclosure.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.desktop.auth.EmbeddedGoogleCredentials
import gr.gtar.jobclosure.shared.calendar.GCalCalendarListEntry

/**
 * First-run screen: sign in via the system browser, then pick which calendar bookings should
 * sync through. When this build has an embedded Google OAuth Client ID/Secret (see
 * EmbeddedGoogleCredentials), that's all there is to it - the Client ID/Secret fields only show up
 * as a fallback on a build without one baked in, where the user has to supply their own.
 */
@Composable
fun SignInScreen(
    state: AppUiState,
    onSaveCredentials: (clientId: String, clientSecret: String, dronePartnerEmail: String) -> Unit,
    onSignIn: () -> Unit,
    onSelectCalendar: (String) -> Unit,
) {
    var clientId by remember { mutableStateOf(state.settings.clientId) }
    var clientSecret by remember { mutableStateOf(state.settings.clientSecret) }
    var dronePartnerEmail by remember { mutableStateOf(state.settings.dronePartnerEmail) }
    val needsManualCredentials = !EmbeddedGoogleCredentials.isConfigured

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(modifier = Modifier.widthIn(max = 480.dp)) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("JobClosure", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Συνδέστε το Google Calendar που χρησιμοποιεί ήδη το κινητό σας, ώστε οι κρατήσεις " +
                        "να συγχρονίζονται και στους δύο υπολογιστές.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (state.calendars.isEmpty()) {
                    if (needsManualCredentials) {
                        // Stated up front because getting it wrong fails late and unhelpfully: a Web
                        // client sends the user all the way to Google's consent page only to be
                        // refused with "Error 400: redirect_uri_mismatch". Sign-in here lands on
                        // http://127.0.0.1 with a port chosen at runtime, and only Desktop clients
                        // accept an arbitrary loopback port - a Web client would need that exact
                        // port registered in advance, which is impossible.
                        Text(
                            "Ο τύπος του OAuth client πρέπει να είναι \"Desktop app\". Ένα " +
                                "\"Web application\" client απορρίπτει τη σύνδεση με σφάλμα " +
                                "redirect_uri_mismatch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = clientId,
                            onValueChange = { clientId = it },
                            label = { Text("Google OAuth Client ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = clientSecret,
                            onValueChange = { clientSecret = it },
                            label = { Text("Google OAuth Client Secret") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = dronePartnerEmail,
                        onValueChange = { dronePartnerEmail = it },
                        label = { Text("Email συνεργάτη drone (προαιρετικό)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Button(
                        onClick = {
                            onSaveCredentials(clientId.trim(), clientSecret.trim(), dronePartnerEmail.trim())
                            onSignIn()
                        },
                        enabled = !state.isLoading && (!needsManualCredentials || (clientId.isNotBlank() && clientSecret.isNotBlank())),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.isLoading) "Σύνδεση..." else "Σύνδεση με Google")
                    }
                } else {
                    Text("Επιλέξτε ημερολόγιο συγχρονισμού:", style = MaterialTheme.typography.titleMedium)
                    CalendarDropdown(
                        calendars = state.calendars,
                        selectedId = state.settings.calendarId,
                        onSelect = onSelectCalendar,
                    )
                }

                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                }

                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CalendarDropdown(
    calendars: List<GCalCalendarListEntry>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = calendars.firstOrNull { it.id == selectedId }?.summary ?: "Επιλέξτε ημερολόγιο"

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Ημερολόγιο") },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            calendars.forEach { calendar ->
                DropdownMenuItem(
                    text = { Text(calendar.summary) },
                    onClick = {
                        expanded = false
                        onSelect(calendar.id)
                    },
                )
            }
        }
    }
}
