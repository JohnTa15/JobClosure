package gr.gtar.jobclosure.ui.bookinglist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.gtar.jobclosure.data.Booking
import gr.gtar.jobclosure.ui.components.AccentButton
import gr.gtar.jobclosure.ui.components.AmbientBackground
import gr.gtar.jobclosure.ui.components.ChangelogDialog
import gr.gtar.jobclosure.ui.components.GlowBox
import gr.gtar.jobclosure.ui.components.NewChip
import gr.gtar.jobclosure.ui.components.NewDesignEasing
import gr.gtar.jobclosure.ui.components.NewIconButton
import gr.gtar.jobclosure.ui.components.NewListEntrance
import gr.gtar.jobclosure.ui.components.NewSectionLabel
import gr.gtar.jobclosure.ui.components.NewSelectableSwatch
import gr.gtar.jobclosure.ui.components.NewSwitch
import gr.gtar.jobclosure.ui.theme.AppTheme
import gr.gtar.jobclosure.ui.theme.AppThemePalettes
import gr.gtar.jobclosure.ui.theme.NewUiColors
import gr.gtar.jobclosure.ui.theme.typeColors
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.util.Locale

private val newMonthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("el", "GR"))
private val newDayHeaderFormatter = DateTimeFormatter.ofPattern("EEE d MMMM", Locale("el", "GR"))
private val newDayHeaderWithYearFormatter = DateTimeFormatter.ofPattern("EEE d MMMM yyyy", Locale("el", "GR"))
private val newTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val newPreviewDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("el", "GR"))

/** "ΑΥΓΟΥΣΤΟΣ 2026" while everything sits in one month, "2016 - 2026" once it doesn't. */
private fun listSpanLabel(bookings: List<gr.gtar.jobclosure.data.Booking>): String {
    val dates = bookings.map { it.ceremonyStart.toLocalDate() }
    val first = dates.minOrNull() ?: return java.time.LocalDate.now().format(newMonthYearFormatter)
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
private fun dayHeaderLabel(date: java.time.LocalDate): String {
    val today = java.time.LocalDate.now()
    val formatter = if (date.year == today.year) newDayHeaderFormatter else newDayHeaderWithYearFormatter
    val formatted = date.format(formatter).uppercase(Locale("el", "GR"))
    return when (date) {
        today -> "ΣΗΜΕΡΑ · $formatted"
        today.plusDays(1) -> "ΑΥΡΙΟ · $formatted"
        else -> formatted
    }
}

/** Restyled booking list - see design_handoff_theme_switcher/README.md "Screen 1". */
@Composable
fun NewBookingListScreen(
    viewModel: BookingListViewModel,
    onAddBooking: () -> Unit,
    onOpenBooking: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val bookings by viewModel.bookings.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val pendingDelete by viewModel.pendingDelete.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val pendingBulkDelete by viewModel.pendingBulkDelete.collectAsState()
    val deleteResult by viewModel.lastDeleteResult.collectAsState()
    val unseenChangelogEntries by viewModel.unseenChangelogEntries.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val activeTheme = AppTheme.fromKey(settings.themeKey)
    val palette = AppThemePalettes.getValue(activeTheme)

    var themeSheetExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(palette = palette, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        // The span of what's actually listed, rather than today's month - after an
                        // import the list is mostly history and today's month says nothing about it.
                        NewSectionLabel(text = listSpanLabel(bookings))
                        Text(
                            if (isSelectionMode) "${selectedIds.size} επιλεγμένες" else "Δουλειές",
                            color = NewUiColors.onGround,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Text(
                            if (isSelectionMode) {
                                "Πάτα για επιλογή · κράτα πατημένο για έξοδο"
                            } else {
                                summaryLine(bookings)
                            },
                            color = NewUiColors.onGroundDim,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    if (isSelectionMode) {
                        NewIconButton(
                            icon = Icons.Filled.SelectAll,
                            contentDescription = "Επιλογή όλων",
                            onClick = { viewModel.selectAllVisible() },
                            iconSize = 21.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        NewIconButton(
                            icon = Icons.Filled.Delete,
                            contentDescription = "Διαγραφή επιλεγμένων",
                            onClick = { viewModel.requestBulkDelete() },
                            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                            iconColor = MaterialTheme.colorScheme.error,
                            iconSize = 21.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        NewIconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = "Έξοδος από την επιλογή",
                            onClick = { viewModel.clearSelection() },
                            iconSize = 21.dp,
                        )
                    } else {
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
                        NewIconButton(
                            icon = Icons.Filled.Settings,
                            contentDescription = "Ρυθμίσεις",
                            onClick = onOpenSettings,
                            iconSize = 21.dp,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = themeSheetExpanded,
                enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { -it / 6 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 6 },
            ) {
                ThemePickerSheet(
                    selected = activeTheme,
                    onSelect = { viewModel.setThemeKey(it.key) },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                )
            }

            FilterChipsRow(
                filter = filter,
                accent = palette.accent,
                onSelect = { viewModel.setFilter(it) },
            )

            deleteResult?.let { result ->
                DeleteResultBanner(result = result, onDismiss = { viewModel.dismissDeleteResult() })
            }

            if (bookings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Δεν υπάρχουν κλεισμένες δουλειές ακόμα.\nΠάτησε + για να προσθέσεις μία.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = NewUiColors.onGroundDim,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            } else {
                val grouped = bookings.groupBy { it.ceremonyStart.toLocalDate() }
                // Position in the flattened list, resolved up front. Counting with a var read from
                // inside the item lambdas instead would count *compositions*, so every scroll back
                // and forth would hand the same card a different entrance delay.
                val entranceIndex = bookings.withIndex().associate { (index, booking) -> booking.id to index }
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp)) {
                    grouped.forEach { (date, dayBookings) ->
                        item(key = "header-$date") {
                            DayHeader(dayHeaderLabel(date), palette.accent, palette.accentDim)
                        }
                        items(dayBookings, key = { it.id }) { booking ->
                            val index = entranceIndex[booking.id] ?: 0
                            NewListEntrance(index = index, modifier = Modifier.padding(bottom = 12.dp)) {
                                NewBookingCard(
                                    booking = booking,
                                    selected = booking.id in selectedIds,
                                    onClick = {
                                        if (isSelectionMode) viewModel.toggleSelection(booking) else onOpenBooking(booking.id)
                                    },
                                    onLongPress = {
                                        if (isSelectionMode) viewModel.clearSelection() else viewModel.startSelection(booking)
                                    },
                                )
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
        val hasCalendarEvent = booking.churchCalendarEventId != null || booking.receptionCalendarEventId != null
        NewDeleteDialog(
            bookings = listOf(booking),
            calendarCount = if (hasCalendarEvent) 1 else 0,
            calendarPermissionGranted = true,
            onDismiss = { viewModel.dismissDeleteRequest() },
            onConfirm = { alsoCalendar -> viewModel.confirmDelete(alsoCalendar) },
        )
    }

    pendingBulkDelete?.let { request ->
        NewDeleteDialog(
            bookings = request.bookings,
            calendarCount = request.withCalendarEvents,
            calendarPermissionGranted = request.calendarPermissionGranted,
            onDismiss = { viewModel.dismissBulkDelete() },
            onConfirm = { alsoCalendar -> viewModel.confirmBulkDelete(alsoCalendar) },
        )
    }

    if (unseenChangelogEntries.isNotEmpty()) {
        ChangelogDialog(entries = unseenChangelogEntries, onDismiss = { viewModel.dismissChangelog() })
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
private fun NewBookingCard(
    booking: Booking,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = typeColors(booking.type)
    // Animated so entering and leaving selection reads as the same card changing state, rather than
    // the list redrawing itself under the finger.
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, tween(200), label = "card-border")
    val borderColor by animateColorAsState(
        if (selected) colors.light else NewUiColors.outlineSoft,
        tween(200),
        label = "card-border-color",
    )
    val tintAlpha by animateFloatAsState(if (selected) 1f else 0f, tween(220), label = "card-tint")

    Box(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Gives the row a concrete height (that of its text column) so the type bar can match it.
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(NewUiColors.cardGradient))
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.light.copy(alpha = 0.20f * tintAlpha),
                        colors.dark.copy(alpha = 0.12f * tintAlpha),
                    ),
                ),
            )
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        // fillMaxHeight() is a no-op under the unbounded height constraint a LazyColumn item gets,
        // which left the coloured type bar measuring 0dp tall - see the IntrinsicSize.Min above.
        Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(colors.barBrush))
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    booking.ceremonyStart.format(newTimeFormatter),
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
                        val receptionTime = booking.receptionStart?.format(newTimeFormatter)
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

        // Bottom-end rather than top: the top row already ends with the unconfirmed marker, and the
        // bottom-right of a card is empty whatever chips it carries.
        androidx.compose.animation.AnimatedVisibility(
            visible = selected,
            enter = androidx.compose.animation.scaleIn(tween(220, easing = NewDesignEasing), initialScale = 0.4f) + fadeIn(tween(180)),
            exit = androidx.compose.animation.scaleOut(tween(160), targetScale = 0.4f) + fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.light)
                    .border(2.dp, NewUiColors.ground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Επιλεγμένο",
                    tint = NewUiColors.ground,
                    modifier = Modifier.size(15.dp),
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



/**
 * The destructive confirmation, in the app's own design rather than a stock Material dialog
 * dropped into the middle of it.
 *
 * It lists what is about to go: on a bulk delete the whole point of the pause is being able to see
 * that the selection is the one you meant, and a count alone cannot show that. The calendar choice
 * is a switch with its own counts beside it, because deleting a booking used to take its calendar
 * entries unconditionally and silently - and a shared calendar entry may be the only copy other
 * people can see.
 */
@Composable
private fun NewDeleteDialog(
    bookings: List<Booking>,
    calendarCount: Int,
    calendarPermissionGranted: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (alsoDeleteFromCalendar: Boolean) -> Unit,
) {
    // Fixed rather than themed: a destructive action should read the same whichever palette is on.
    val danger = Color(0xFFFF6B6B)
    val canTouchCalendar = calendarPermissionGranted && calendarCount > 0
    var alsoCalendar by remember(bookings) { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlowBox(
            glowColor = danger.copy(alpha = 0.28f),
            blurRadius = 44.dp,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(24.dp).widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF20222F), NewUiColors.surfaceSunken)))
                    .border(1.dp, danger.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(22.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(danger.copy(alpha = 0.14f))
                            .border(1.dp, danger.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = danger, modifier = Modifier.size(21.dp))
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        NewSectionLabel(text = "Οριστική διαγραφή", color = danger)
                        Text(
                            if (bookings.size == 1) "1 δουλειά" else "${bookings.size} δουλειές",
                            color = NewUiColors.onGround,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NewUiColors.wellDark)
                        .border(1.dp, NewUiColors.outlineSoft, RoundedCornerShape(14.dp))
                        .padding(vertical = 4.dp),
                ) {
                    // A handful is enough to recognise the selection; a long list would push the
                    // buttons off screen, which is the last thing a destructive dialog should do.
                    bookings.take(PREVIEW_LIMIT).forEach { booking -> DeletePreviewRow(booking) }
                    if (bookings.size > PREVIEW_LIMIT) {
                        Text(
                            "+ ${bookings.size - PREVIEW_LIMIT} ακόμα",
                            color = NewUiColors.onGroundFaint,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 8.dp),
                        )
                    }
                }

                when {
                    calendarCount == 0 -> Text(
                        "Καμία από αυτές δεν έχει εγγραφή στο ημερολόγιο του κινητού.",
                        color = NewUiColors.onGroundFaint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    !calendarPermissionGranted -> Text(
                        "Δεν υπάρχει άδεια ημερολογίου, οπότε οι εγγραφές στο ημερολόγιο θα μείνουν.",
                        color = danger,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    else -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (alsoCalendar) danger.copy(alpha = 0.10f) else Color(0x66232532))
                            .border(
                                1.dp,
                                if (alsoCalendar) danger.copy(alpha = 0.35f) else NewUiColors.outlineSoft,
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { alsoCalendar = !alsoCalendar }
                            .padding(14.dp),
                    ) {
                        Icon(
                            Icons.Filled.EventBusy,
                            contentDescription = null,
                            tint = if (alsoCalendar) danger else NewUiColors.onGroundDim,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Και από το ημερολόγιο", color = NewUiColors.onGround, fontSize = 14.sp)
                            Text(
                                if (bookings.size == 1) {
                                    "Αυτή η δουλειά έχει εγγραφή στο ημερολόγιο."
                                } else {
                                    "$calendarCount από τις ${bookings.size} έχουν εγγραφή."
                                },
                                color = NewUiColors.onGroundFaint,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        NewSwitch(checked = alsoCalendar, onColor = danger, onCheckedChange = { alsoCalendar = it })
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 20.dp)) {
                    AccentButton(
                        text = "Άκυρο",
                        onClick = onDismiss,
                        borderColor = NewUiColors.outline,
                        containerColor = Color(0x66232532),
                        contentColor = NewUiColors.onGroundMuted,
                        glowColor = Color.Transparent,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                    )
                    AccentButton(
                        text = "Διαγραφή",
                        onClick = { onConfirm(alsoCalendar && canTouchCalendar) },
                        icon = Icons.Filled.DeleteForever,
                        borderColor = danger.copy(alpha = 0.55f),
                        containerColor = danger.copy(alpha = 0.16f),
                        contentColor = danger,
                        glowColor = danger.copy(alpha = 0.35f),
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private const val PREVIEW_LIMIT = 4

/** One line of the "about to be deleted" list, carrying the same type colour as its card. */
@Composable
private fun DeletePreviewRow(booking: Booking) {
    val colors = typeColors(booking.type)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Box(modifier = Modifier.fillMaxHeight().width(3.dp).clip(RoundedCornerShape(2.dp)).background(colors.barBrush))
        Text(
            booking.ceremonyStart.format(newPreviewDateFormatter),
            color = colors.light,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp),
        )
        Text(
            booking.title.ifBlank { booking.type.displayName },
            color = NewUiColors.onGroundMuted,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        )
    }
}

/** Says what the delete actually did, and stays put when part of it failed. */
@Composable
private fun DeleteResultBanner(result: BulkDeleteResult, onDismiss: () -> Unit) {
    val hasFailures = result.calendarEventsFailed > 0
    val tint = if (hasFailures) Color(0xFFFF6B6B) else NewUiColors.success
    var visible by remember(result) { mutableStateOf(false) }

    LaunchedEffect(result) {
        visible = true
        // A clean result is worth a glance, not a dismissal; a partial one has to be read, so it
        // waits for the user instead of sliding away with the bad news.
        if (!hasFailures) {
            delay(5000)
            visible = false
            delay(220)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(320, easing = NewDesignEasing)) { -it / 2 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(220)) { -it / 2 },
    ) {
        GlowBox(
            glowColor = tint.copy(alpha = 0.22f),
            blurRadius = 26.dp,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.16f), Color(0xB3232532))))
                    .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (hasFailures) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
                    Text(
                        "Διαγράφηκαν ${result.deleted} ${if (result.deleted == 1) "δουλειά" else "δουλειές"}",
                        color = NewUiColors.onGround,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        when {
                            hasFailures ->
                                "${result.calendarEventsFailed} εγγραφές ημερολογίου δεν διαγράφηκαν - " +
                                    "μπορεί να τις έχει σβήσει ήδη κάποιος άλλος ή να λείπει η άδεια."
                            result.calendarEventsDeleted > 0 ->
                                "Μαζί και ${result.calendarEventsDeleted} εγγραφές από το ημερολόγιο."
                            else -> "Το ημερολόγιο του κινητού δεν πειράχτηκε."
                        },
                        color = if (hasFailures) tint else NewUiColors.onGroundDim,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                NewIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Κλείσιμο",
                    onClick = onDismiss,
                    size = 32.dp,
                    iconSize = 15.dp,
                    containerColor = Color.Transparent,
                    borderColor = Color.Transparent,
                )
            }
        }
    }
}
