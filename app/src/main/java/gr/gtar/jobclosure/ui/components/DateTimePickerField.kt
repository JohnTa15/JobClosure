package gr.gtar.jobclosure.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Read-only field that opens a date picker then a time picker, and reports the combined result. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerField(
    label: String,
    dateTime: LocalDateTime?,
    onDateTimeChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    OutlinedTextField(
        value = dateTime?.format(displayFormatter) ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            TextButton(onClick = { showDatePicker = true }) { Text("Επιλογή") }
        },
        modifier = modifier.fillMaxWidth(),
    )

    if (showDatePicker) {
        val initialMillis = (dateTime ?: LocalDateTime.now())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        pendingDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        showDatePicker = false
                        showTimePicker = true
                    } else {
                        showDatePicker = false
                    }
                }) { Text("Επόμενο") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Άκυρο") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialTime = dateTime?.toLocalTime() ?: LocalTime.of(12, 0)
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val date = pendingDate ?: dateTime?.toLocalDate() ?: LocalDate.now()
                    onDateTimeChange(
                        LocalDateTime.of(date, LocalTime.of(timePickerState.hour, timePickerState.minute))
                    )
                    showTimePicker = false
                }) { Text("Εντάξει") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Άκυρο") }
            },
            text = {
                TimePicker(state = timePickerState, modifier = Modifier.padding(8.dp))
            },
        )
    }
}
