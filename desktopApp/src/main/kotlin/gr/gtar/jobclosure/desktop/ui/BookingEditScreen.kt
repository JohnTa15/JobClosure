package gr.gtar.jobclosure.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.shared.model.Booking
import gr.gtar.jobclosure.shared.model.BookingType
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingEditScreen(
    booking: Booking,
    isNew: Boolean,
    conflicts: List<Booking>,
    onSave: (Booking, ignoreConflicts: Boolean) -> Unit,
    onDismissConflicts: () -> Unit,
    onDelete: (Booking) -> Unit,
    onCancel: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    var title by remember { mutableStateOf(booking.title) }
    var type by remember { mutableStateOf(booking.type) }
    var notes by remember { mutableStateOf(booking.notes) }
    var hasDrone by remember { mutableStateOf(booking.hasDrone) }
    var churchName by remember { mutableStateOf(booking.churchName) }
    var churchAddress by remember { mutableStateOf(booking.churchAddress) }
    val ceremonyLocal = booking.ceremonyStart.toJavaInstant().atZone(zone).toLocalDateTime()
    var ceremonyDate by remember { mutableStateOf(ceremonyLocal.format(dateFormatter)) }
    var ceremonyTime by remember { mutableStateOf(ceremonyLocal.format(timeFormatter)) }
    var ceremonyDuration by remember { mutableStateOf(booking.ceremonyDurationMinutes.toString()) }
    var hasReception by remember { mutableStateOf(booking.hasReception) }
    var receptionVenueName by remember { mutableStateOf(booking.receptionVenueName) }
    var receptionVenueAddress by remember { mutableStateOf(booking.receptionVenueAddress) }
    val receptionLocal = (booking.receptionStart ?: booking.ceremonyStart.plus(3600.seconds))
        .toJavaInstant().atZone(zone).toLocalDateTime()
    var receptionDate by remember { mutableStateOf(receptionLocal.format(dateFormatter)) }
    var receptionTime by remember { mutableStateOf(receptionLocal.format(timeFormatter)) }
    var receptionDuration by remember { mutableStateOf(booking.receptionDurationMinutes.toString()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun buildBooking(): Booking? {
        val ceremonyStart = parseDateTime(ceremonyDate, ceremonyTime, zone)
        if (ceremonyStart == null) {
            validationError = "Μη έγκυρη ημερομηνία/ώρα τελετής (yyyy-MM-dd, HH:mm)."
            return null
        }
        val receptionStart = if (hasReception) {
            parseDateTime(receptionDate, receptionTime, zone) ?: run {
                validationError = "Μη έγκυρη ημερομηνία/ώρα δεξίωσης (yyyy-MM-dd, HH:mm)."
                return null
            }
        } else {
            null
        }
        validationError = null
        return booking.copy(
            title = title.trim(),
            type = type,
            notes = notes,
            hasDrone = hasDrone,
            churchName = churchName.trim(),
            churchAddress = churchAddress.trim(),
            ceremonyStart = ceremonyStart,
            ceremonyDurationMinutes = ceremonyDuration.toIntOrNull() ?: 60,
            hasReception = hasReception,
            receptionVenueName = receptionVenueName.trim(),
            receptionVenueAddress = receptionVenueAddress.trim(),
            receptionStart = receptionStart,
            receptionDurationMinutes = receptionDuration.toIntOrNull() ?: 240,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isNew) "Νέα κράτηση" else "Επεξεργασία κράτησης") })
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Τίτλος") }, modifier = Modifier.fillMaxWidth())

            TypeDropdown(selected = type, onSelect = { type = it })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasDrone, onCheckedChange = { hasDrone = it })
                Text("Χρήση drone")
            }

            if (type.isChurchSacrament) {
                OutlinedTextField(value = churchName, onValueChange = { churchName = it }, label = { Text("Όνομα εκκλησίας") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = churchAddress, onValueChange = { churchAddress = it }, label = { Text("Διεύθυνση εκκλησίας") }, modifier = Modifier.fillMaxWidth())
            }

            Text("Τελετή", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(value = ceremonyDate, onValueChange = { ceremonyDate = it }, label = { Text("Ημερομηνία (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = ceremonyTime, onValueChange = { ceremonyTime = it }, label = { Text("Ώρα (HH:mm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = ceremonyDuration, onValueChange = { ceremonyDuration = it }, label = { Text("Διάρκεια (λεπτά)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasReception, onCheckedChange = { hasReception = it })
                Text("Δεξίωση")
            }

            if (hasReception) {
                OutlinedTextField(value = receptionVenueName, onValueChange = { receptionVenueName = it }, label = { Text("Χώρος δεξίωσης") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = receptionVenueAddress, onValueChange = { receptionVenueAddress = it }, label = { Text("Διεύθυνση δεξίωσης") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = receptionDate, onValueChange = { receptionDate = it }, label = { Text("Ημερομηνία (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = receptionTime, onValueChange = { receptionTime = it }, label = { Text("Ώρα (HH:mm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = receptionDuration, onValueChange = { receptionDuration = it }, label = { Text("Διάρκεια (λεπτά)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Σημειώσεις") }, modifier = Modifier.fillMaxWidth())

            validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { buildBooking()?.let { onSave(it, false) } }) { Text("Αποθήκευση") }
                OutlinedButton(onClick = onCancel) { Text("Άκυρο") }
                if (!isNew) {
                    TextButton(onClick = { onDelete(booking) }) { Text("Διαγραφή", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (conflicts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismissConflicts,
            title = { Text("Σύγκρουση ραντεβού") },
            text = {
                Column {
                    Text("Υπάρχει ήδη κράτηση αυτό το διάστημα:")
                    conflicts.forEach {
                        val start = it.ceremonyStart.toJavaInstant().atZone(zone)
                        Text("• ${it.title} (${start.format(dateFormatter)} ${start.format(timeFormatter)})")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { buildBooking()?.let { onSave(it, true) } }) { Text("Αποθήκευση ούτως ή άλλως") }
            },
            dismissButton = {
                TextButton(onClick = onDismissConflicts) { Text("Άκυρο") }
            },
        )
    }
}

@Composable
private fun TypeDropdown(selected: BookingType, onSelect: (BookingType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Τύπος") },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier.matchParentSize().clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BookingType.entries.forEach { candidate ->
                DropdownMenuItem(
                    text = { Text(candidate.displayName) },
                    onClick = {
                        expanded = false
                        onSelect(candidate)
                    },
                )
            }
        }
    }
}

private fun parseDateTime(date: String, time: String, zone: ZoneId): kotlinx.datetime.Instant? = runCatching {
    LocalDateTime.parse("${date.trim()}T${time.trim()}").atZone(zone).toInstant().toKotlinInstant()
}.getOrNull()
