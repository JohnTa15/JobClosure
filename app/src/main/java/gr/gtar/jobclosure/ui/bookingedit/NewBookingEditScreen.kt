package gr.gtar.jobclosure.ui.bookingedit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.gtar.jobclosure.calendar.CalendarInfo
import gr.gtar.jobclosure.data.BookingType
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import gr.gtar.jobclosure.ui.components.AmbientBackground
import gr.gtar.jobclosure.ui.components.AutocompleteAddressField
import gr.gtar.jobclosure.ui.components.GlowBox
import gr.gtar.jobclosure.ui.components.NewIconButton
import gr.gtar.jobclosure.ui.components.NewSectionLabel
import gr.gtar.jobclosure.ui.components.NewSwitch
import gr.gtar.jobclosure.ui.theme.AccentPalette
import gr.gtar.jobclosure.ui.theme.AppTheme
import gr.gtar.jobclosure.ui.theme.AppThemePalettes
import gr.gtar.jobclosure.ui.theme.NewUiColors
import gr.gtar.jobclosure.ui.theme.typeColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val newDateTimeDisplayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Restyled booking edit form - see design_handoff_theme_switcher/README.md "Screen 3". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewBookingEditScreen(
    viewModel: BookingEditViewModel,
    isNew: Boolean,
    onDone: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val activeTheme = AppTheme.fromKey(state.themeKey)
    val palette = AppThemePalettes.getValue(activeTheme)

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(palette = palette, modifier = Modifier.fillMaxSize())

        if (state.isLoading) return@Box

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                NewIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Πίσω", onClick = onDone, size = 42.dp)
                Text(
                    if (isNew) "Νέα δουλειά" else "Επεξεργασία",
                    color = NewUiColors.onGround,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                if (!isNew) {
                    NewIconButton(
                        icon = Icons.Filled.Delete,
                        contentDescription = "Διαγραφή",
                        onClick = { viewModel.requestDelete() },
                        size = 42.dp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NewTextField(
                    label = "Πελάτης",
                    value = state.title,
                    onValueChange = { value -> viewModel.update { it.copy(title = value) } },
                    leadingIcon = Icons.Filled.Person,
                    accent = palette.accent,
                )
                NewTextField(
                    label = "Τηλέφωνο",
                    value = state.clientPhone,
                    onValueChange = { value -> viewModel.update { it.copy(clientPhone = value) } },
                    leadingIcon = Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone,
                    accent = palette.accent,
                )

                Column {
                    NewSectionLabel(text = "Τύπος", modifier = Modifier.padding(bottom = 7.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BookingType.entries.forEach { type ->
                            TypeChip(
                                type = type,
                                selected = state.type == type,
                                onClick = { viewModel.update { it.copy(type = type) } },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NewDateTimePickerField(
                        label = "Ώρα μυστηρίου",
                        dateTime = state.ceremonyStart,
                        onDateTimeChange = { dt -> viewModel.update { it.copy(ceremonyStart = dt) } },
                        accent = palette.accent,
                        modifier = Modifier.weight(1f),
                    )
                    NewTextField(
                        label = "Διάρκεια",
                        value = state.ceremonyDurationMinutes.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { minutes -> viewModel.update { it.copy(ceremonyDurationMinutes = minutes) } } },
                        leadingIcon = Icons.Filled.Schedule,
                        keyboardType = KeyboardType.Number,
                        accent = palette.accent,
                        modifier = Modifier.width(104.dp),
                    )
                }

                AutocompleteAddressField(
                    value = state.churchName,
                    onValueChange = { value -> viewModel.update { it.copy(churchName = value) } },
                    label = "Όνομα εκκλησίας",
                    provider = state.mapsProvider,
                    googleApiKey = state.mapsApiKey,
                    placeSearchRepository = viewModel.placeSearchRepository,
                    onSuggestionSelected = { suggestion -> viewModel.update { it.copy(churchName = suggestion.name, churchAddress = suggestion.fullText) } },
                )
                AutocompleteAddressField(
                    value = state.churchAddress,
                    onValueChange = { value -> viewModel.update { it.copy(churchAddress = value) } },
                    label = "Διεύθυνση εκκλησίας",
                    provider = state.mapsProvider,
                    googleApiKey = state.mapsApiKey,
                    placeSearchRepository = viewModel.placeSearchRepository,
                )

                SwitchGroup(state = state, viewModel = viewModel)

                if (state.hasReception) {
                    AutocompleteAddressField(
                        value = state.receptionVenueName,
                        onValueChange = { value -> viewModel.update { it.copy(receptionVenueName = value) } },
                        label = "Όνομα κέντρου δεξίωσης",
                        provider = state.mapsProvider,
                        googleApiKey = state.mapsApiKey,
                        placeSearchRepository = viewModel.placeSearchRepository,
                        onSuggestionSelected = { suggestion -> viewModel.update { it.copy(receptionVenueName = suggestion.name, receptionVenueAddress = suggestion.fullText) } },
                    )
                    AutocompleteAddressField(
                        value = state.receptionVenueAddress,
                        onValueChange = { value -> viewModel.update { it.copy(receptionVenueAddress = value) } },
                        label = "Διεύθυνση κέντρου",
                        provider = state.mapsProvider,
                        googleApiKey = state.mapsApiKey,
                        placeSearchRepository = viewModel.placeSearchRepository,
                    )
                    NewDateTimePickerField(
                        label = "Ώρα δεξίωσης",
                        dateTime = state.receptionStart,
                        onDateTimeChange = { dt -> viewModel.update { it.copy(receptionStart = dt) } },
                        accent = palette.accent,
                    )
                    NewTextField(
                        label = "Διάρκεια δεξίωσης (λεπτά)",
                        value = state.receptionDurationMinutes.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { minutes -> viewModel.update { it.copy(receptionDurationMinutes = minutes) } } },
                        leadingIcon = Icons.Filled.Schedule,
                        keyboardType = KeyboardType.Number,
                        accent = palette.accent,
                    )
                    Text(
                        String.format(
                            Locale.US,
                            "Ενδεικτική τιμή δεξίωσης: %.0f€ (%.1f ώρες x %.0f€/ώρα, χωρίς drone)",
                            state.suggestedReceptionPrice,
                            state.receptionDurationMinutes / 60.0,
                            BookingEditUiState.RECEPTION_EURO_PER_HOUR,
                        ),
                        color = NewUiColors.onGroundFaint,
                        fontSize = 11.sp,
                    )
                }

                NewTextField(
                    label = "Τιμή",
                    value = if (state.price == 0.0) "" else state.price.toString(),
                    onValueChange = { value ->
                        if (value.isBlank()) {
                            viewModel.update { it.copy(price = 0.0) }
                        } else {
                            value.toDoubleOrNull()?.let { amount -> viewModel.update { it.copy(price = amount) } }
                        }
                    },
                    leadingIcon = Icons.Filled.Euro,
                    keyboardType = KeyboardType.Decimal,
                    accent = palette.accent,
                )

                NewTextField(
                    label = "Σημειώσεις",
                    value = state.notes,
                    onValueChange = { value -> viewModel.update { it.copy(notes = value) } },
                    minLines = 2,
                    accent = palette.accent,
                )

                CalendarSection(state = state, viewModel = viewModel, accent = palette.accent)

                SaveButtonWithSheen(
                    text = if (state.isSaving) "Αποθήκευση..." else "Αποθήκευση",
                    onClick = { viewModel.save() },
                    enabled = !state.isSaving,
                    accent = palette,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Text(
                    "Ελέγχεται αυτόματα αν πέφτει πάνω σε δουλειά που έχεις ήδη κλείσει.",
                    color = NewUiColors.onGroundFaint,
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    state.conflicts.forEach { conflict -> Text("• ${conflict.title} (${conflict.type.displayName})") }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.save(ignoreConflicts = true) }) { Text("Αποθήκευση όπως και να 'χει") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConflicts() }) { Text("Άκυρο") }
            },
        )
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmation() },
            title = { Text("Διαγραφή δουλειάς") },
            text = { Text("Είσαι σίγουρος ότι θέλεις να διαγράψεις οριστικά αυτή τη δουλειά;") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAndFinish() }) {
                    Text("Διαγραφή", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) { Text("Άκυρο") }
            },
        )
    }
}

@Composable
private fun TypeChip(type: BookingType, selected: Boolean, onClick: () -> Unit) {
    val colors = typeColors(type)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.light else colors.chipFill)
            .then(if (!selected) Modifier.border(1.dp, colors.chipBorder, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Text(
            type.displayName,
            color = if (selected) NewUiColors.ground else colors.light,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SwitchGroup(state: BookingEditUiState, viewModel: BookingEditViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x73232532))
            .border(1.dp, NewUiColors.outlineSoft, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
    ) {
        SwitchRow(
            icon = Icons.Filled.FlightTakeoff,
            label = "Χρήση drone",
            checked = state.hasDrone,
            onColor = NewUiColors.droneChip,
            onCheckedChange = { checked -> viewModel.update { it.copy(hasDrone = checked) } },
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NewUiColors.outlineSoft))
        SwitchRow(
            icon = Icons.Filled.Celebration,
            label = "Δεξίωση μετά",
            checked = state.hasReception,
            onColor = NewUiColors.receptionChip,
            onCheckedChange = { checked ->
                viewModel.update {
                    it.copy(
                        hasReception = checked,
                        receptionStart = if (checked && it.receptionStart == null) it.ceremonyStart.plusHours(1) else it.receptionStart,
                    )
                }
            },
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NewUiColors.outlineSoft))
        SwitchRow(
            icon = Icons.Filled.Verified,
            label = "Επιβεβαιωμένη",
            checked = state.isConfirmed,
            onColor = Color(0xFFFFD166),
            onCheckedChange = { checked -> viewModel.update { it.copy(isConfirmed = checked) } },
        )
    }
}

@Composable
private fun CalendarSection(state: BookingEditUiState, viewModel: BookingEditViewModel, accent: Color) {
    Column {
        NewSectionLabel(text = "Ημερολόγιο συσκευής", modifier = Modifier.padding(bottom = 7.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x73232532))
                .border(1.dp, NewUiColors.outlineSoft, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            Text("Προσθήκη στο ημερολόγιο", color = NewUiColors.onGround, fontSize = 14.sp, modifier = Modifier.weight(1f))
            NewSwitch(
                checked = state.addToCalendar,
                onColor = accent,
                onCheckedChange = { checked -> viewModel.update { it.copy(addToCalendar = checked) } },
            )
        }
        if (state.addToCalendar) {
            if (state.availableCalendars.isEmpty()) {
                Text(
                    "Δεν βρέθηκαν εγγράψιμα ημερολόγια. Δώσε άδεια Ημερολογίου στην εφαρμογή και βεβαιώσου " +
                        "ότι έχεις λογαριασμό Google ή Samsung με ημερολόγιο στη συσκευή.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                NewCalendarDropdown(
                    calendars = state.availableCalendars,
                    selectedId = state.selectedCalendarId,
                    onSelected = { id -> viewModel.update { it.copy(selectedCalendarId = id) } },
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewCalendarDropdown(
    calendars: List<CalendarInfo>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCalendar = calendars.firstOrNull { it.id == selectedId } ?: calendars.first()
    LaunchedEffect(calendars) {
        if (selectedId == null || calendars.none { it.id == selectedId }) {
            onSelected(calendars.first().id)
        }
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x80232532))
                .border(1.dp, NewUiColors.outline, RoundedCornerShape(12.dp))
                .padding(14.dp),
        ) {
            Text(
                "${selectedCalendar.displayName} (${selectedCalendar.accountName})",
                color = NewUiColors.onGround,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = NewUiColors.onGroundFaint)
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            calendars.forEach { calendar ->
                DropdownMenuItem(
                    text = { Text("${calendar.displayName} (${calendar.accountName})") },
                    onClick = {
                        expanded = false
                        onSelected(calendar.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, label: String, checked: Boolean, onColor: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (checked) onColor else NewUiColors.onGroundFaint, modifier = Modifier.padding(end = 10.dp))
        Text(label, color = NewUiColors.onGround, fontSize = 14.sp, modifier = Modifier.weight(1f))
        NewSwitch(checked = checked, onColor = onColor, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NewTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    Column(modifier = modifier) {
        NewSectionLabel(text = label, modifier = Modifier.padding(bottom = 7.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = NewUiColors.onGround, fontSize = 15.sp),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x80232532))
                        .border(1.dp, NewUiColors.outline, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, contentDescription = null, tint = accent, modifier = Modifier.padding(end = 10.dp))
                    }
                    Box(modifier = Modifier.weight(1f)) { innerTextField() }
                }
            },
        )
    }
}

/** Same visual language as [NewTextField] (label above, dark rounded box) instead of the shared
 *  [gr.gtar.jobclosure.ui.components.DateTimePickerField]'s plain Material3 OutlinedTextField look,
 *  which clashed with the rest of this screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewDateTimePickerField(
    label: String,
    dateTime: LocalDateTime?,
    onDateTimeChange: (LocalDateTime) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier = modifier) {
        NewSectionLabel(text = label, modifier = Modifier.padding(bottom = 7.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x80232532))
                .border(1.dp, NewUiColors.outline, RoundedCornerShape(12.dp))
                .clickable { showDatePicker = true }
                .padding(14.dp),
        ) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = accent, modifier = Modifier.padding(end = 10.dp))
            Text(
                dateTime?.format(newDateTimeDisplayFormatter) ?: "",
                color = NewUiColors.onGround,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
            Text("Επιλογή", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }

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
                    onDateTimeChange(LocalDateTime.of(date, LocalTime.of(timePickerState.hour, timePickerState.minute)))
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

@Composable
private fun SaveButtonWithSheen(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    accent: AccentPalette,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val infinite = rememberInfiniteTransition(label = "save-sheen")
    val sheenPhase by infinite.animateFloat(
        initialValue = -1.2f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(animation = tween(3400, easing = LinearEasing)),
        label = "sheen",
    )

    GlowBox(glowColor = accent.accentGlow, blurRadius = 30.dp, shape = shape, modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .background(accent.accentContainer)
                .border(1.dp, accent.accentBorder, shape)
                .clickable(enabled = enabled, onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { translationX = sheenPhase * size.width }
                    .background(
                        Brush.linearGradient(
                            listOf(Color.Transparent, accent.onAccentContainer.copy(alpha = 0.14f), Color.Transparent),
                        ),
                    ),
            )
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, tint = accent.onAccentContainer, modifier = Modifier.padding(end = 8.dp))
                Text(text, color = accent.onAccentContainer, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
