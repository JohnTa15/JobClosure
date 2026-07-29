package gr.gtar.jobclosure.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.BuildConfig
import gr.gtar.jobclosure.update.ApkUpdateManager
import gr.gtar.jobclosure.update.UpdateCheckResult
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    var homeAddress by remember { mutableStateOf("") }
    var mapsApiKey by remember { mutableStateOf("") }
    var reminderMinutes by remember { mutableStateOf("120") }
    var dronePartnerEmail by remember { mutableStateOf("") }
    var openAipApiKey by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val current = viewModel.settings.first()
        homeAddress = current.homeAddress
        mapsApiKey = current.mapsApiKey
        reminderMinutes = current.reminderMinutesBefore.toString()
        dronePartnerEmail = current.dronePartnerEmail
        openAipApiKey = current.openAipApiKey
        loaded = true
    }

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
        if (!loaded) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UpdateSection(viewModel)

            OutlinedTextField(
                value = homeAddress,
                onValueChange = {
                    homeAddress = it
                    viewModel.setHomeAddress(it)
                },
                label = { Text("Διεύθυνση σπιτιού") },
                supportingText = { Text("Χρησιμοποιείται ως αφετηρία για τον υπολογισμό χρόνου διαδρομής") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = mapsApiKey,
                onValueChange = {
                    mapsApiKey = it
                    viewModel.setMapsApiKey(it)
                },
                label = { Text("Κλειδί Google Maps API") },
                supportingText = {
                    Text(
                        "Χρειάζεται ένα Google Cloud API key με ενεργοποιημένο το Directions API " +
                            "για να υπολογίζεται ο χρόνος διαδρομής. Αποθηκεύεται μόνο τοπικά στη συσκευή."
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = reminderMinutes,
                onValueChange = { value ->
                    reminderMinutes = value
                    value.toIntOrNull()?.let { viewModel.setReminderMinutesBefore(it) }
                },
                label = { Text("Υπενθύμιση πριν (λεπτά)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Προεπιλογή: 120 λεπτά (2 ώρες) πριν το μυστήριο/δεξίωση") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = dronePartnerEmail,
                onValueChange = {
                    dronePartnerEmail = it
                    viewModel.setDronePartnerEmail(it)
                },
                label = { Text("Email συνεργάτη για Drone") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                supportingText = {
                    Text(
                        "Όταν ενεργοποιείς το Drone σε μια δουλειά, αυτός ο συνεργάτης προστίθεται ως " +
                            "καλεσμένος στο event του ημερολογίου - έτσι το βλέπει και στο δικό του " +
                            "ημερολόγιο, και το Google Calendar του στέλνει αυτόματα email πρόσκλησης " +
                            "(χρειάζεται να διαλέξεις ημερολόγιο Google, όχι τοπικό, όταν αποθηκεύεις)."
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = openAipApiKey,
                onValueChange = {
                    openAipApiKey = it
                    viewModel.setOpenAipApiKey(it)
                },
                label = { Text("Κλειδί OpenAIP API") },
                supportingText = {
                    Text(
                        "Δωρεάν κλειδί από το openaip.net - χρησιμοποιείται για να ελέγχεται αν υπάρχουν " +
                            "καταχωρημένες ζώνες (περιορισμένες/απαγορευμένες/ελεγχόμενες) κοντά στην " +
                            "τοποθεσία όταν έχεις ενεργό το Drone. Ενημερωτικός έλεγχος μόνο - δεν " +
                            "αντικαθιστά επίσημη πηγή πριν πετάξεις."
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Η προσθήκη στο ημερολόγιο γίνεται μέσω του ημερολογίου της συσκευής, οπότε δουλεύει " +
                    "είτε χρησιμοποιείς Google Calendar είτε Samsung Calendar - επίλεξε το ημερολόγιο " +
                    "που θέλεις όταν αποθηκεύεις μια δουλειά.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UpdateSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val updateStatus by viewModel.updateStatus.collectAsState()
    var isDownloading by remember { mutableStateOf(false) }
    var showInstallPermissionDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Ενημερώσεις", style = MaterialTheme.typography.titleMedium)
            Text("Τρέχουσα έκδοση: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)

            when (val status = updateStatus) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Text(
                        "Διαθέσιμη νέα έκδοση: ${status.versionName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Button(
                        onClick = {
                            if (!ApkUpdateManager.canInstallUnknownApps(context)) {
                                showInstallPermissionDialog = true
                            } else {
                                isDownloading = true
                                ApkUpdateManager.downloadUpdate(context, status.downloadUrl) { uri ->
                                    isDownloading = false
                                    ApkUpdateManager.promptInstall(context, uri)
                                }
                            }
                        },
                        enabled = !isDownloading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (isDownloading) "Λήψη..." else "Λήψη & Εγκατάσταση")
                    }
                }
                UpdateCheckResult.UpToDate -> Text(
                    "Έχεις την πιο πρόσφατη έκδοση.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is UpdateCheckResult.Error -> Text(
                    status.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                null -> Text(
                    "Δεν έχει γίνει έλεγχος ακόμα.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TextButton(onClick = { viewModel.checkForUpdateNow() }) {
                Text("Έλεγχος για ενημέρωση τώρα")
            }
        }
    }

    if (showInstallPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showInstallPermissionDialog = false },
            title = { Text("Άδεια εγκατάστασης") },
            text = {
                Text(
                    "Για να εγκατασταθεί η ενημέρωση, χρειάζεται να επιτρέψεις στο JobClosure να " +
                        "εγκαθιστά εφαρμογές από άγνωστες πηγές."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showInstallPermissionDialog = false
                    ApkUpdateManager.openInstallPermissionSettings(context)
                }) { Text("Ρυθμίσεις") }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPermissionDialog = false }) { Text("Άκυρο") }
            },
        )
    }
}
