package gr.gtar.jobclosure.desktop.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.gtar.jobclosure.desktop.ui.components.AmbientBackground
import gr.gtar.jobclosure.desktop.ui.components.GlowBox
import gr.gtar.jobclosure.desktop.ui.components.NewIconButton
import gr.gtar.jobclosure.desktop.ui.components.NewSectionLabel
import gr.gtar.jobclosure.desktop.ui.components.NewSwitch
import gr.gtar.jobclosure.desktop.ui.theme.AccentPalette
import gr.gtar.jobclosure.desktop.ui.theme.AppTheme
import gr.gtar.jobclosure.desktop.ui.theme.AppThemePalettes
import gr.gtar.jobclosure.desktop.ui.theme.NewUiColors
import gr.gtar.jobclosure.desktop.ui.theme.typeColors
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

/** Restyled booking edit form - desktop port of the Android "NewBookingEditScreen" ("Screen 3"). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingEditScreen(
    booking: Booking,
    isNew: Boolean,
    conflicts: List<Booking>,
    themeKey: String,
    onSave: (Booking, ignoreConflicts: Boolean) -> Unit,
    onDismissConflicts: () -> Unit,
    onDelete: (Booking) -> Unit,
    onCancel: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val activeTheme = AppTheme.fromKey(themeKey)
    val palette = AppThemePalettes.getValue(activeTheme)

    var title by remember { mutableStateOf(booking.title) }
    var type by remember { mutableStateOf(booking.type) }
    var notes by remember { mutableStateOf(booking.notes) }
    var hasDrone by remember { mutableStateOf(booking.hasDrone) }
    var isConfirmed by remember { mutableStateOf(booking.isConfirmed) }
    var price by remember { mutableStateOf(if (booking.price == 0.0) "" else booking.price.toString()) }
    var clientPhone by remember { mutableStateOf(booking.clientPhone) }
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
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
            isConfirmed = isConfirmed,
            price = price.toDoubleOrNull() ?: 0.0,
            clientPhone = clientPhone.trim(),
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

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(palette = palette, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                NewIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Πίσω", onClick = onCancel, size = 42.dp)
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
                        onClick = { showDeleteConfirmation = true },
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
                    value = title,
                    onValueChange = { title = it },
                    leadingIcon = Icons.Filled.Person,
                    accent = palette.accent,
                )
                NewTextField(
                    label = "Τηλέφωνο",
                    value = clientPhone,
                    onValueChange = { clientPhone = it },
                    leadingIcon = Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone,
                    accent = palette.accent,
                )

                Column {
                    NewSectionLabel(text = "Τύπος", modifier = Modifier.padding(bottom = 7.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BookingType.entries.forEach { candidate ->
                            TypeChip(
                                type = candidate,
                                selected = type == candidate,
                                onClick = { type = candidate },
                            )
                        }
                    }
                }

                if (type.isChurchSacrament) {
                    NewTextField(
                        label = "Όνομα εκκλησίας",
                        value = churchName,
                        onValueChange = { churchName = it },
                        accent = palette.accent,
                    )
                    NewTextField(
                        label = "Διεύθυνση εκκλησίας",
                        value = churchAddress,
                        onValueChange = { churchAddress = it },
                        accent = palette.accent,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NewTextField(
                        label = "Ημερομηνία (yyyy-MM-dd)",
                        value = ceremonyDate,
                        onValueChange = { ceremonyDate = it },
                        leadingIcon = Icons.Filled.Schedule,
                        accent = palette.accent,
                        modifier = Modifier.weight(1f),
                    )
                    NewTextField(
                        label = "Ώρα (HH:mm)",
                        value = ceremonyTime,
                        onValueChange = { ceremonyTime = it },
                        accent = palette.accent,
                        modifier = Modifier.width(120.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NewTextField(
                        label = "Διάρκεια (λεπτά)",
                        value = ceremonyDuration,
                        onValueChange = { ceremonyDuration = it },
                        keyboardType = KeyboardType.Number,
                        accent = palette.accent,
                        modifier = Modifier.width(160.dp),
                    )
                }

                SwitchGroup(
                    hasDrone = hasDrone,
                    onHasDroneChange = { hasDrone = it },
                    hasReception = hasReception,
                    onHasReceptionChange = { hasReception = it },
                    isConfirmed = isConfirmed,
                    onIsConfirmedChange = { isConfirmed = it },
                )

                if (hasReception) {
                    NewTextField(
                        label = "Χώρος δεξίωσης",
                        value = receptionVenueName,
                        onValueChange = { receptionVenueName = it },
                        accent = palette.accent,
                    )
                    NewTextField(
                        label = "Διεύθυνση δεξίωσης",
                        value = receptionVenueAddress,
                        onValueChange = { receptionVenueAddress = it },
                        accent = palette.accent,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NewTextField(
                            label = "Ημερομηνία (yyyy-MM-dd)",
                            value = receptionDate,
                            onValueChange = { receptionDate = it },
                            leadingIcon = Icons.Filled.Schedule,
                            accent = palette.accent,
                            modifier = Modifier.weight(1f),
                        )
                        NewTextField(
                            label = "Ώρα (HH:mm)",
                            value = receptionTime,
                            onValueChange = { receptionTime = it },
                            accent = palette.accent,
                            modifier = Modifier.width(120.dp),
                        )
                    }
                    NewTextField(
                        label = "Διάρκεια (λεπτά)",
                        value = receptionDuration,
                        onValueChange = { receptionDuration = it },
                        keyboardType = KeyboardType.Number,
                        accent = palette.accent,
                    )
                }

                NewTextField(
                    label = "Τιμή",
                    value = price,
                    onValueChange = { price = it },
                    leadingIcon = Icons.Filled.Euro,
                    keyboardType = KeyboardType.Decimal,
                    accent = palette.accent,
                )

                NewTextField(
                    label = "Σημειώσεις",
                    value = notes,
                    onValueChange = { notes = it },
                    minLines = 2,
                    accent = palette.accent,
                )

                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                SaveButtonWithSheen(
                    text = "Αποθήκευση",
                    onClick = { buildBooking()?.let { onSave(it, false) } },
                    accent = palette,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Text(
                    "Ελέγχεται αυτόματα αν πέφτει πάνω σε δουλειά που έχεις ήδη κλείσει.",
                    color = NewUiColors.onGroundFaint,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
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

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Διαγραφή δουλειάς") },
            text = { Text("Είσαι σίγουρος ότι θέλεις να διαγράψεις οριστικά αυτή τη δουλειά;") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete(booking)
                }) {
                    Text("Διαγραφή", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Άκυρο") }
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
private fun SwitchGroup(
    hasDrone: Boolean,
    onHasDroneChange: (Boolean) -> Unit,
    hasReception: Boolean,
    onHasReceptionChange: (Boolean) -> Unit,
    isConfirmed: Boolean,
    onIsConfirmedChange: (Boolean) -> Unit,
) {
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
            checked = hasDrone,
            onColor = NewUiColors.droneChip,
            onCheckedChange = onHasDroneChange,
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NewUiColors.outlineSoft))
        SwitchRow(
            icon = Icons.Filled.Celebration,
            label = "Δεξίωση μετά",
            checked = hasReception,
            onColor = NewUiColors.receptionChip,
            onCheckedChange = onHasReceptionChange,
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NewUiColors.outlineSoft))
        SwitchRow(
            icon = Icons.Filled.Verified,
            label = "Επιβεβαιωμένη",
            checked = isConfirmed,
            onColor = Color(0xFFFFD166),
            onCheckedChange = onIsConfirmedChange,
        )
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

@Composable
private fun SaveButtonWithSheen(
    text: String,
    onClick: () -> Unit,
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
                .clickable(onClick = onClick),
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

private fun parseDateTime(date: String, time: String, zone: ZoneId): kotlinx.datetime.Instant? = runCatching {
    LocalDateTime.parse("${date.trim()}T${time.trim()}").atZone(zone).toInstant().toKotlinInstant()
}.getOrNull()
