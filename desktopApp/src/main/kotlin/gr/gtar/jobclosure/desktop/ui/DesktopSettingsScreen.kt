package gr.gtar.jobclosure.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.desktop.update.AppVersion
import gr.gtar.jobclosure.desktop.update.UpdateCheckResult
import gr.gtar.jobclosure.desktop.util.openInBrowser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopSettingsScreen(
    state: AppUiState,
    onSaveGitHubToken: (String) -> Unit,
    onSaveDronePartnerEmail: (String) -> Unit,
    onCheckForUpdate: () -> Unit,
    onShowChangelogHistory: () -> Unit,
    onBack: () -> Unit,
) {
    var gitHubToken by remember(state.settings.gitHubToken) { mutableStateOf(state.settings.gitHubToken) }
    var dronePartnerEmail by remember(state.settings.dronePartnerEmail) { mutableStateOf(state.settings.dronePartnerEmail) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ρυθμίσεις") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Πίσω")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ενημερώσεις", style = MaterialTheme.typography.titleMedium)
                    Text("Τρέχουσα έκδοση: ${AppVersion.current}", style = MaterialTheme.typography.bodyMedium)

                    when (val result = state.updateCheckResult) {
                        is UpdateCheckResult.UpdateAvailable -> {
                            Text(
                                "Διαθέσιμη νέα έκδοση: ${result.versionName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                            Button(
                                onClick = { openInBrowser(result.downloadUrl) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Λήψη νέας έκδοσης")
                            }
                        }
                        UpdateCheckResult.UpToDate -> Text(
                            "Έχεις την πιο πρόσφατη έκδοση.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        is UpdateCheckResult.Error -> Text(
                            result.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        null -> Text(
                            "Δεν έχει γίνει έλεγχος ακόμα.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    TextButton(onClick = onCheckForUpdate, enabled = !state.isCheckingForUpdate) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(if (state.isCheckingForUpdate) "Έλεγχος..." else "Έλεγχος για ενημέρωση τώρα")
                    }
                    TextButton(onClick = onShowChangelogHistory) {
                        Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Ιστορικό ενημερώσεων")
                    }

                    OutlinedTextField(
                        value = gitHubToken,
                        onValueChange = {
                            gitHubToken = it
                            onSaveGitHubToken(it)
                        },
                        label = { Text("GitHub token (για ιδιωτικό repo)") },
                        supportingText = {
                            Text(
                                "Χρειάζεται μόνο αν το repository του project παραμένει private - " +
                                    "fine-grained personal access token, scoped μόνο σε αυτό το repo, με " +
                                    "δικαίωμα 'Contents: Read-only'. Αν το repo γίνει public, δεν χρειάζεται καθόλου."
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Drone", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = dronePartnerEmail,
                        onValueChange = {
                            dronePartnerEmail = it
                            onSaveDronePartnerEmail(it)
                        },
                        label = { Text("Email συνεργάτη για Drone") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
