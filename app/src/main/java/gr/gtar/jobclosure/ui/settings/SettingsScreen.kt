package gr.gtar.jobclosure.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val current = viewModel.settings.first()
        homeAddress = current.homeAddress
        mapsApiKey = current.mapsApiKey
        reminderMinutes = current.reminderMinutesBefore.toString()
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
