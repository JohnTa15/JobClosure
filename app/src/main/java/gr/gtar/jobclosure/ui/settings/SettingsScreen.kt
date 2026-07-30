package gr.gtar.jobclosure.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.BuildConfig
import gr.gtar.jobclosure.data.MapsProvider
import gr.gtar.jobclosure.location.LocationHelper
import gr.gtar.jobclosure.location.LocationResult
import gr.gtar.jobclosure.ui.components.AutocompleteAddressField
import gr.gtar.jobclosure.update.ApkUpdateManager
import gr.gtar.jobclosure.update.DownloadResult
import gr.gtar.jobclosure.update.UpdateCheckResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    var homeAddress by remember { mutableStateOf("") }
    var mapsApiKey by remember { mutableStateOf("") }
    var mapsProvider by remember { mutableStateOf(MapsProvider.OPENSTREETMAP) }
    var reminderMinutes by remember { mutableStateOf("120") }
    var dronePartnerEmail by remember { mutableStateOf("") }
    var gitHubToken by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    suspend fun useCurrentLocation() {
        locationError = null
        isLocating = true
        when (val location = LocationHelper.getCurrentLocation(context)) {
            is LocationResult.Success -> {
                val suggestion = viewModel.placeSearchRepository.reverseGeocode(location.latitude, location.longitude)
                if (suggestion != null) {
                    homeAddress = suggestion.fullText
                    viewModel.setHomeAddress(suggestion.fullText)
                } else {
                    locationError = "Δεν βρέθηκε διεύθυνση για αυτή την τοποθεσία"
                }
            }
            is LocationResult.Error -> locationError = location.message
        }
        isLocating = false
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) {
            scope.launch { useCurrentLocation() }
        } else {
            locationError = "Χρειάζεται άδεια τοποθεσίας για αυτή τη λειτουργία"
        }
    }

    LaunchedEffect(Unit) {
        val current = viewModel.settings.first()
        homeAddress = current.homeAddress
        mapsApiKey = current.mapsApiKey
        mapsProvider = current.mapsProvider
        reminderMinutes = current.reminderMinutesBefore.toString()
        dronePartnerEmail = current.dronePartnerEmail
        gitHubToken = current.gitHubToken
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UpdateSection(viewModel)

            Row(verticalAlignment = Alignment.CenterVertically) {
                AutocompleteAddressField(
                    value = homeAddress,
                    onValueChange = {
                        homeAddress = it
                        viewModel.setHomeAddress(it)
                    },
                    label = "Διεύθυνση σπιτιού",
                    provider = mapsProvider,
                    googleApiKey = mapsApiKey,
                    placeSearchRepository = viewModel.placeSearchRepository,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    enabled = !isLocating,
                    onClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            scope.launch { useCurrentLocation() }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            )
                        }
                    },
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Χρήση τρέχουσας τοποθεσίας")
                    }
                }
            }
            locationError?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Text(
                "Πάτα το εικονίδιο τοποθεσίας για αυτόματη συμπλήρωση με τη τρέχουσα θέση σου, ή γράψε τη " +
                    "διεύθυνση με το χέρι. Χρησιμοποιείται ως αφετηρία για τον υπολογισμό χρόνου διαδρομής",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Πάροχος χαρτών / διαδρομών", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mapsProvider == MapsProvider.OPENSTREETMAP,
                    onClick = {
                        mapsProvider = MapsProvider.OPENSTREETMAP
                        viewModel.setMapsProvider(MapsProvider.OPENSTREETMAP)
                    },
                    label = { Text("OpenStreetMap (δωρεάν)") },
                )
                FilterChip(
                    selected = mapsProvider == MapsProvider.GOOGLE,
                    onClick = {
                        mapsProvider = MapsProvider.GOOGLE
                        viewModel.setMapsProvider(MapsProvider.GOOGLE)
                    },
                    label = { Text("Google Maps") },
                )
            }

            if (mapsProvider == MapsProvider.OPENSTREETMAP) {
                Text(
                    "Ο χρόνος διαδρομής και οι συνθήκες για Drone υπολογίζονται μέσω OpenStreetMap " +
                        "(Nominatim + OSRM) - δωρεάν, χωρίς κλειδί API.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                OutlinedTextField(
                    value = mapsApiKey,
                    onValueChange = {
                        mapsApiKey = it
                        viewModel.setMapsApiKey(it)
                    },
                    label = { Text("Κλειδί Google Maps API") },
                    supportingText = {
                        Text(
                            "Χρειάζεται ένα Google Cloud API key με ενεργοποιημένα τα Directions API και " +
                                "Geocoding API. Αποθηκεύεται μόνο τοπικά στη συσκευή."
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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

            val updateStatusForToken by viewModel.updateStatus.collectAsState()

            OutlinedTextField(
                value = gitHubToken,
                onValueChange = {
                    gitHubToken = it
                    viewModel.setGitHubToken(it)
                },
                label = { Text("GitHub token (για ιδιωτικό repo)") },
                supportingText = {
                    Text(
                        "Χρειάζεται μόνο αν το repository του project παραμένει private - " +
                            "fine-grained personal access token, scoped μόνο σε αυτό το repo, με " +
                            "δικαίωμα 'Contents: Read-only'. Χρησιμοποιείται για τον έλεγχο και τη " +
                            "λήψη ενημερώσεων. Αν το repo γίνει public, δεν χρειάζεται καθόλου."
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (gitHubToken.isNotBlank()) {
                when (updateStatusForToken) {
                    is UpdateCheckResult.UpdateAvailable, UpdateCheckResult.UpToDate -> Text(
                        "✓ Το token είναι αποθηκευμένο και επιβεβαιώθηκε ότι λειτουργεί.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    is UpdateCheckResult.Error -> Text(
                        "Το token είναι αποθηκευμένο, αλλά ο τελευταίος έλεγχος ενημέρωσης απέτυχε - " +
                            "έλεγξε αν είναι σωστό.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    null -> Text(
                        "Το token είναι αποθηκευμένο. Πάτα \"Έλεγχος για ενημέρωση τώρα\" για να " +
                            "επιβεβαιωθεί ότι λειτουργεί.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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
    val scope = rememberCoroutineScope()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
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
                                downloadError = null
                                isDownloading = true
                                scope.launch {
                                    when (
                                        val result = ApkUpdateManager.downloadUpdate(
                                            context, status.downloadUrl, settings.gitHubToken,
                                        )
                                    ) {
                                        is DownloadResult.Success -> {
                                            isDownloading = false
                                            ApkUpdateManager.promptInstall(context, result.apkUri)
                                        }
                                        is DownloadResult.Error -> {
                                            isDownloading = false
                                            downloadError = result.message
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isDownloading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(if (isDownloading) "Λήψη..." else "Λήψη & Εγκατάσταση")
                    }
                    downloadError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
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
                }) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Ρυθμίσεις")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPermissionDialog = false }) { Text("Άκυρο") }
            },
        )
    }
}
