package gr.gtar.jobclosure.ui.bookingedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.calendar.CalendarInfo
import gr.gtar.jobclosure.data.BookingType
import gr.gtar.jobclosure.ui.components.DateTimePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingEditScreen(
    viewModel: BookingEditViewModel,
    isNew: Boolean,
    onDone: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Νέα δουλειά" else "Επεξεργασία") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Πίσω")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { viewModel.deleteAndFinish() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Διαγραφή")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = { value -> viewModel.update { it.copy(title = value) } },
                label = { Text("Πελάτης / Τίτλος") },
                modifier = Modifier.fillMaxWidth(),
            )

            BookingTypeDropdown(
                selected = state.type,
                onSelected = { type -> viewModel.update { it.copy(type = type) } },
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Χρήση Drone", modifier = Modifier.weight(1f))
                Switch(
                    checked = state.hasDrone,
                    onCheckedChange = { checked -> viewModel.update { it.copy(hasDrone = checked) } },
                )
            }

            HorizontalDivider()
            Text("Εκκλησία / Μυστήριο", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = state.churchName,
                onValueChange = { value -> viewModel.update { it.copy(churchName = value) } },
                label = { Text("Όνομα εκκλησίας") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.churchAddress,
                onValueChange = { value -> viewModel.update { it.copy(churchAddress = value) } },
                label = { Text("Διεύθυνση εκκλησίας") },
                modifier = Modifier.fillMaxWidth(),
            )
            DateTimePickerField(
                label = "Ώρα μυστηρίου",
                dateTime = state.ceremonyStart,
                onDateTimeChange = { dt -> viewModel.update { it.copy(ceremonyStart = dt) } },
            )
            OutlinedTextField(
                value = state.ceremonyDurationMinutes.toString(),
                onValueChange = { value ->
                    val minutes = value.toIntOrNull()
                    if (minutes != null) viewModel.update { it.copy(ceremonyDurationMinutes = minutes) }
                },
                label = { Text("Διάρκεια μυστηρίου (λεπτά)") },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Υπάρχει δεξίωση μετά", modifier = Modifier.weight(1f))
                Switch(
                    checked = state.hasReception,
                    onCheckedChange = { checked ->
                        viewModel.update {
                            it.copy(
                                hasReception = checked,
                                receptionStart = if (checked && it.receptionStart == null) {
                                    it.ceremonyStart.plusHours(1)
                                } else {
                                    it.receptionStart
                                },
                            )
                        }
                    },
                )
            }

            if (state.hasReception) {
                OutlinedTextField(
                    value = state.receptionVenueName,
                    onValueChange = { value -> viewModel.update { it.copy(receptionVenueName = value) } },
                    label = { Text("Όνομα κέντρου δεξίωσης") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.receptionVenueAddress,
                    onValueChange = { value -> viewModel.update { it.copy(receptionVenueAddress = value) } },
                    label = { Text("Διεύθυνση κέντρου") },
                    modifier = Modifier.fillMaxWidth(),
                )
                DateTimePickerField(
                    label = "Ώρα δεξίωσης",
                    dateTime = state.receptionStart,
                    onDateTimeChange = { dt -> viewModel.update { it.copy(receptionStart = dt) } },
                )
                OutlinedTextField(
                    value = state.receptionDurationMinutes.toString(),
                    onValueChange = { value ->
                        val minutes = value.toIntOrNull()
                        if (minutes != null) viewModel.update { it.copy(receptionDurationMinutes = minutes) }
                    },
                    label = { Text("Διάρκεια δεξίωσης (λεπτά)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider()
            OutlinedTextField(
                value = state.notes,
                onValueChange = { value -> viewModel.update { it.copy(notes = value) } },
                label = { Text("Σημειώσεις") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            HorizontalDivider()
            Text("Ημερολόγιο συσκευής", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Προσθήκη στο ημερολόγιο (Google/Samsung)", modifier = Modifier.weight(1f))
                Switch(
                    checked = state.addToCalendar,
                    onCheckedChange = { checked -> viewModel.update { it.copy(addToCalendar = checked) } },
                )
            }
            if (state.addToCalendar) {
                if (state.availableCalendars.isEmpty()) {
                    Text(
                        "Δεν βρέθηκαν εγγράψιμα ημερολόγια. Δώσε άδεια Ημερολογίου στην εφαρμογή και " +
                            "βεβαιώσου ότι έχεις λογαριασμό Google ή Samsung με ημερολόγιο στη συσκευή.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    CalendarDropdown(
                        calendars = state.availableCalendars,
                        selectedId = state.selectedCalendarId,
                        onSelected = { id -> viewModel.update { it.copy(selectedCalendarId = id) } },
                    )
                }
            }

            Button(
                onClick = { viewModel.save() },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Αποθήκευση..." else "Αποθήκευση")
            }
        }
    }

    if (state.conflicts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConflicts() },
            title = { Text("Υπάρχει ήδη δουλειά αυτή την ώρα") },
            text = {
                Column {
                    Text("Έχεις ήδη κλεισμένες αυτές τις δουλειές που συγκρούονται:")
                    state.conflicts.forEach { conflict ->
                        Text("• ${conflict.title} (${conflict.type.displayName})")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.save(ignoreConflicts = true) }) {
                    Text("Αποθήκευση όπως και να 'χει")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConflicts() }) { Text("Άκυρο") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingTypeDropdown(selected: BookingType, onSelected: (BookingType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Τύπος δουλειάς") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BookingType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDropdown(
    calendars: List<CalendarInfo>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCalendar = calendars.firstOrNull { it.id == selectedId } ?: calendars.first()
    LaunchedEffect(calendars) {
        if (selectedId == null || calendars.none { it.id == selectedId }) {
            onSelected(calendars.first().id)
        }
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "${selectedCalendar.displayName} (${selectedCalendar.accountName})",
            onValueChange = {},
            readOnly = true,
            label = { Text("Ημερολόγιο") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            calendars.forEach { calendar ->
                DropdownMenuItem(
                    text = { Text("${calendar.displayName} (${calendar.accountName})") },
                    onClick = {
                        onSelected(calendar.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
