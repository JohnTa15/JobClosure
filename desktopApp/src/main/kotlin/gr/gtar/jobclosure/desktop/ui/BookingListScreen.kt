package gr.gtar.jobclosure.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.gtar.jobclosure.desktop.ui.components.AccentButton
import gr.gtar.jobclosure.desktop.ui.components.AmbientBackground
import gr.gtar.jobclosure.desktop.ui.components.NewChip
import gr.gtar.jobclosure.desktop.ui.components.NewIconButton
import gr.gtar.jobclosure.desktop.ui.components.NewListEntrance
import gr.gtar.jobclosure.desktop.ui.components.NewSectionLabel
import gr.gtar.jobclosure.desktop.ui.components.NewSelectableSwatch
import gr.gtar.jobclosure.desktop.ui.theme.AppTheme
import gr.gtar.jobclosure.desktop.ui.theme.AppThemePalettes
import gr.gtar.jobclosure.desktop.ui.theme.NewUiColors
import gr.gtar.jobclosure.desktop.ui.theme.typeColors
import gr.gtar.jobclosure.shared.model.Booking
import kotlinx.datetime.toJavaInstant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val newMonthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("el", "GR"))
private val newDayHeaderFormatter = DateTimeFormatter.ofPattern("EEE d MMMM", Locale("el", "GR"))
private val newDayHeaderWithYearFormatter = DateTimeFormatter.ofPattern("EEE d MMMM yyyy", Locale("el", "GR"))
private val newTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val zone = ZoneId.systemDefault()

/** "ΑΥΓΟΥΣΤΟΣ 2026" while everything sits in one month, "2016 - 2026" once it doesn't. */
private fun listSpanLabel(bookings: List<Booking>): String {
    val dates = bookings.map { it.ceremonyStart.toJavaInstant().atZone(zone).toLocalDate() }
    val first = dates.minOrNull() ?: return LocalDate.now().format(newMonthYearFormatter)
    val last = dates.maxOrNull() ?: first
    return if (first.year == last.year && first.month == last.month) {
        first.format(newMonthYearFormatter)
    } else if (first.year == last.year) {
        first.year.toString()
    } else {
        "${first.year} - ${last.year}"
    }
}

/**
 * Day headers carry the year for anything outside the current one. Once a decade of past jobs has
 * been imported the list spans many years, and "ΤΡΙ 7 ΑΥΓΟΥΣΤΟΥ" on its own could be any of them.
 * Today and tomorrow are named instead, since those are the two the user is actually working from.
 */
private fun dayHeaderLabel(date: LocalDate): String {
    val today = LocalDate.now()
    val formatter = if (date.year == today.year) newDayHeaderFormatter else newDayHeaderWithYearFormatter
    val formatted = date.format(formatter).uppercase(Locale("el", "GR"))
    return when (date) {
        today -> "ΣΗΜΕΡΑ · $formatted"
        today.plusDays(1) -> "ΑΥΡΙΟ · $formatted"
        else -> formatted
    }
}

/** Restyled booking list - desktop port of the Android "NewBookingListScreen" ("Screen 1"). */
@Composable
fun BookingListScreen(
    state: AppUiState,
    onAddBooking: () -> Unit,
    onOpenBooking: (Booking) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onSetFilter: (BookingFilter) -> Unit,
    onSetThemeKey: (String) -> Unit,
    onRequestDelete: (Booking) -> Unit,
    onDismissDeleteRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val bookings = state.filteredBookings
    val filter = state.filter
    val pendingDelete = state.pendingDelete
    val activeTheme = AppTheme.fromKey(state.settings.themeKey)
    val palette = AppThemePalettes.getValue(activeTheme)

    var themeSheetExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(palette = palette, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        NewSectionLabel(text = listSpanLabel(bookings))
                        Text(
                            "Δουλειές",
                            color = NewUiColors.onGround,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Text(
                            summaryLine(bookings),
                            color = NewUiColors.onGroundDim,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    NewIconButton(
                        icon = Icons.Filled.Palette,
                        contentDescription = "Θέμα εφαρμογής",
                        onClick = { themeSheetExpanded = !themeSheetExpanded },
                        borderColor = palette.accentBorder,
                        containerColor = palette.accentContainer,
                        iconColor = palette.onAccentContainer,
                        iconSize = 20.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    // A booking added on the phone reaches this machine through Google Calendar,
                    // not through the app - so without a way to re-fetch, the desktop list stayed
                    // on whatever it read at startup until the app was restarted.
                    NewIconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = "Ανανέωση",
                        onClick = onRefresh,
                        iconSize = 21.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    NewIconButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = "Ρυθμίσεις",
                        onClick = onOpenSettings,
                        iconSize = 21.dp,
                    )
                }
            }

            AnimatedVisibility(
                visible = themeSheetExpanded,
                enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { -it / 6 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 6 },
            ) {
                ThemePickerSheet(
                    selected = activeTheme,
                    onSelect = { onSetThemeKey(it.key) },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                )
            }

            FilterChipsRow(
                filter = filter,
                accent = palette.accent,
                onSelect = onSetFilter,
            )

            when {
                state.isLoading && bookings.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.accent)
                    }
                }
                bookings.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Δεν υπάρχουν κλεισμένες δουλειές ακόμα.\nΠάτησε + για να προσθέσεις μία.",
                            textAlign = TextAlign.Center,
                            color = NewUiColors.onGroundDim,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }
                else -> {
                    val grouped = bookings.groupBy { it.ceremonyStart.toJavaInstant().atZone(zone).toLocalDate() }
                    // Position in the flattened list, resolved up front. Counting with a var read
                    // from inside the item lambdas instead would count *compositions*, so every
                    // scroll back and forth would hand the same card a different entrance delay.
                    val entranceIndex = bookings.withIndex().associate { (index, booking) -> booking.bookingId to index }
                    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp)) {
                        grouped.forEach { (date, dayBookings) ->
                            item(key = "header-$date") {
                                DayHeader(dayHeaderLabel(date), palette.accent, palette.accentDim)
                            }
                            items(dayBookings, key = { it.bookingId }) { booking ->
                                val index = entranceIndex[booking.bookingId] ?: 0
                                NewListEntrance(index = index, modifier = Modifier.padding(bottom = 12.dp)) {
                                    NewBookingCard(
                                        booking = booking,
                                        onClick = { onOpenBooking(booking) },
                                        onLongPress = { onRequestDelete(booking) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AccentButton(
            text = "Νέα δουλειά",
            onClick = onAddBooking,
            icon = Icons.Filled.Add,
            shape = RoundedCornerShape(999.dp),
            borderColor = palette.accentBorder,
            containerColor = palette.accentContainer,
            contentColor = palette.onAccentContainer,
            glowColor = palette.accentGlow,
            height = 52.dp,
            fillWidth = false,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 26.dp),
        )
    }

    pendingDelete?.let { booking ->
        AlertDialog(
            onDismissRequest = onDismissDeleteRequest,
            title = { Text("Διαγραφή δουλειάς") },
            text = { Text("Είσαι σίγουρος ότι θέλεις να διαγράψεις οριστικά τη δουλειά \"${booking.title}\";") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text("Διαγραφή", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteRequest) { Text("Άκυρο") }
            },
        )
    }
}

private fun summaryLine(bookings: List<Booking>): String {
    val total = bookings.size
    val drone = bookings.count { it.hasDrone }
    val unconfirmed = bookings.count { !it.isConfirmed }
    val parts = mutableListOf("$total κλεισμένες")
    if (drone > 0) parts += "$drone με drone"
    if (unconfirmed > 0) parts += "$unconfirmed χωρίς επιβεβαίωση"
    return parts.joinToString(" · ")
}

@Composable
private fun ThemePickerSheet(
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xB80D0E17))
            .border(1.dp, NewUiColors.outlineSoft, RoundedCornerShape(16.dp))
            .padding(13.dp),
    ) {
        NewSectionLabel(text = "Θέμα εφαρμογής")
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppTheme.entries.forEach { theme ->
                val themePalette = AppThemePalettes.getValue(theme)
                NewSelectableSwatch(
                    label = theme.label,
                    selected = theme == selected,
                    swatchBrush = Brush.linearGradient(listOf(themePalette.swatchStart, themePalette.swatchEnd)),
                    accentColor = themePalette.accent,
                    accentBorder = themePalette.accentBorder,
                    accentContainer = themePalette.accentContainer,
                    onClick = { onSelect(theme) },
                )
            }
        }
        Text(
            "Αλλάζει το φως και το βασικό χρώμα. Τα χρώματα ανά τύπο δουλειάς μένουν ίδια.",
            color = NewUiColors.onGroundFaint,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun FilterChipsRow(filter: BookingFilter, accent: Color, onSelect: (BookingFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BookingFilter.entries.forEach { option ->
            val underlineColor = when (option) {
                BookingFilter.ALL -> accent
                BookingFilter.WEDDING_BAPTISM -> NewUiColors.unconfirmedMarker
                BookingFilter.DRONE -> NewUiColors.droneChip
                BookingFilter.RECEPTION -> NewUiColors.receptionChip
            }
            FilterPill(
                label = option.label,
                selected = filter == option,
                underlineColor = underlineColor,
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, underlineColor: Color, onClick: () -> Unit) {
    val underlineAlpha by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "chip-underline")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xB3232532))
            .border(1.dp, NewUiColors.outline, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            color = if (selected) NewUiColors.onGround else NewUiColors.onGroundMuted,
            fontSize = 13.sp,
        )
        // matchParentSize, not align+fillMaxWidth: the row of chips scrolls horizontally, so the
        // width constraint reaching this Box is unbounded and fillMaxWidth() would collapse the
        // underline to nothing. Matching the parent's measured size gives it the chip's width.
        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color.Transparent, underlineColor.copy(alpha = underlineAlpha), Color.Transparent)),
                    ),
            )
        }
    }
}

@Composable
private fun DayHeader(label: String, accent: Color, accentDim: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp),
    ) {
        Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(accentDim, Color.Transparent))),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewBookingCard(booking: Booking, onClick: () -> Unit, onLongPress: () -> Unit) {
    val colors = typeColors(booking.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The card's height comes from its text column, and a LazyColumn hands its items an
            // unbounded height constraint - under which fillMaxHeight() is a no-op, so the coloured
            // type bar below measured 0dp tall and never appeared. Min-intrinsic height gives the
            // row a concrete height for the bar to match.
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(NewUiColors.cardGradient))
            .border(1.dp, NewUiColors.outlineSoft, RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(colors.barBrush))
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    booking.ceremonyStart.toJavaInstant().atZone(zone).format(newTimeFormatter),
                    color = colors.light,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    booking.title,
                    color = NewUiColors.onGround,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                if (!booking.isConfirmed) {
                    UnconfirmedMarker()
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                NewChip(
                    text = booking.type.displayName.uppercase(Locale("el", "GR")),
                    textColor = colors.chipText,
                    borderColor = colors.chipBorder,
                    fillColor = colors.chipFill,
                )
                if (booking.churchName.isNotBlank()) {
                    Text(booking.churchName, color = NewUiColors.onGroundDim, fontSize = 12.sp)
                }
            }
            val hasAttributeChips = booking.hasDrone || booking.hasReception
            if (hasAttributeChips) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 11.dp),
                ) {
                    if (booking.hasDrone) {
                        NewChip(
                            text = "Drone",
                            icon = Icons.Filled.FlightTakeoff,
                            textColor = NewUiColors.droneChip,
                            borderColor = NewUiColors.droneChip.copy(alpha = 0.30f),
                            fillColor = NewUiColors.droneChip.copy(alpha = 0.10f),
                        )
                    }
                    if (booking.hasReception) {
                        val receptionTime = booking.receptionStart?.toJavaInstant()?.atZone(zone)?.format(newTimeFormatter)
                        NewChip(
                            text = if (receptionTime != null) "Δεξίωση $receptionTime" else "Δεξίωση",
                            icon = Icons.Filled.Celebration,
                            textColor = Color(0xFFFF8FB0),
                            borderColor = NewUiColors.receptionChip.copy(alpha = 0.30f),
                            fillColor = NewUiColors.receptionChip.copy(alpha = 0.10f),
                        )
                    }
                }
            } else if (!booking.isConfirmed) {
                Text(
                    "Περιμένει επιβεβαίωση από τον πελάτη",
                    color = NewUiColors.unconfirmedMarker,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun UnconfirmedMarker() {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(NewUiColors.unconfirmedMarker),
        contentAlignment = Alignment.Center,
    ) {
        Text("?", color = NewUiColors.ground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
