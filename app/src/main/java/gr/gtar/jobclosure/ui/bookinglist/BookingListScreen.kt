package gr.gtar.jobclosure.ui.bookinglist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.data.Booking
import gr.gtar.jobclosure.ui.components.ChangelogDialog
import gr.gtar.jobclosure.ui.theme.accentColor
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateHeaderFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale("el", "GR"))
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingListScreen(
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
    val unseenChangelogEntries by viewModel.unseenChangelogEntries.collectAsState()
    val deleteResult by viewModel.lastDeleteResult.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(deleteResult) {
        val result = deleteResult ?: return@LaunchedEffect
        val message = buildString {
            append("Διαγράφηκαν ${result.deleted} ")
            append(if (result.deleted == 1) "δουλειά" else "δουλειές")
            when {
                result.calendarEventsFailed > 0 ->
                    append(" · ${result.calendarEventsFailed} εγγραφές ημερολογίου ΔΕΝ διαγράφηκαν")
                result.calendarEventsDeleted > 0 ->
                    append(" · ${result.calendarEventsDeleted} και από το ημερολόγιο")
            }
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        viewModel.dismissDeleteResult()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelectionMode) "${selectedIds.size} επιλεγμένες" else "JobClosure") },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.selectAllVisible() }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Επιλογή όλων")
                        }
                        IconButton(onClick = { viewModel.requestBulkDelete() }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Διαγραφή επιλεγμένων",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Έξοδος από την επιλογή")
                        }
                    } else {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Ρυθμίσεις")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBooking) {
                Icon(Icons.Filled.Add, contentDescription = "Νέα δουλειά")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BookingFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { viewModel.setFilter(option) },
                        label = { Text(option.label) },
                    )
                }
            }

            AnimatedContent(
                targetState = bookings.isEmpty(),
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "booking-list-content",
            ) { isEmpty ->
                if (isEmpty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Δεν υπάρχουν κλεισμένες δουλειές ακόμα.\nΠάτησε + για να προσθέσεις μία.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val grouped = bookings.groupBy { it.ceremonyStart.toLocalDate() }
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        grouped.forEach { (date, dayBookings) ->
                            item(key = "header-$date") {
                                Text(
                                    text = date.format(dateHeaderFormatter)
                                        .replaceFirstChar { it.titlecase(Locale("el", "GR")) },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                                )
                            }
                            items(dayBookings, key = { it.id }) { booking ->
                                BookingRow(
                                    booking,
                                    selected = booking.id in selectedIds,
                                    onClick = {
                                        if (isSelectionMode) viewModel.toggleSelection(booking) else onOpenBooking(booking.id)
                                    },
                                    onLongPress = {
                                        if (isSelectionMode) viewModel.clearSelection() else viewModel.startSelection(booking)
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { booking ->
        val hasCalendarEvent = booking.churchCalendarEventId != null || booking.receptionCalendarEventId != null
        ClassicDeleteDialog(
            title = "Διαγραφή δουλειάς",
            body = "Να διαγραφεί οριστικά η δουλειά \"${booking.title}\";",
            calendarCount = if (hasCalendarEvent) 1 else 0,
            totalCount = 1,
            calendarPermissionGranted = true,
            onDismiss = { viewModel.dismissDeleteRequest() },
            onConfirm = { alsoCalendar -> viewModel.confirmDelete(alsoCalendar) },
        )
    }

    pendingBulkDelete?.let { request ->
        ClassicDeleteDialog(
            title = "Διαγραφή ${request.bookings.size} δουλειών",
            body = "Θα διαγραφούν οριστικά από την εφαρμογή.",
            calendarCount = request.withCalendarEvents,
            totalCount = request.bookings.size,
            calendarPermissionGranted = request.calendarPermissionGranted,
            onDismiss = { viewModel.dismissBulkDelete() },
            onConfirm = { alsoCalendar -> viewModel.confirmBulkDelete(alsoCalendar) },
        )
    }

    if (unseenChangelogEntries.isNotEmpty()) {
        ChangelogDialog(entries = unseenChangelogEntries, onDismiss = { viewModel.dismissChangelog() })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookingRow(
    booking: Booking,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
            ),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(5.dp)
                    .background(booking.type.accentColor()),
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!booking.isConfirmed) {
                        UnconfirmedBadge(modifier = Modifier.padding(end = 6.dp))
                    }
                    Text(
                        text = "${booking.ceremonyStart.format(timeFormatter)}  ${booking.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (booking.isConfirmed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(booking.type.accentColor()),
                    )
                    Text(text = booking.type.displayName, style = MaterialTheme.typography.bodyMedium)
                }
                if (booking.churchName.isNotBlank()) {
                    Text(
                        text = "Εκκλησία: ${booking.churchName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    if (booking.hasDrone) {
                        IconPill(
                            icon = Icons.Filled.FlightTakeoff,
                            label = "Drone",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    if (booking.hasReception) {
                        IconPill(
                            icon = Icons.Filled.Celebration,
                            label = "Δεξίωση",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

/** Small circular "?" marker shown next to a booking's title while it's only tentatively booked. */
@Composable
private fun UnconfirmedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "?",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun IconPill(
    icon: ImageVector,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}


/** Classic-design twin of the restyled screen's delete dialog - same choice, same counts. */
@Composable
private fun ClassicDeleteDialog(
    title: String,
    body: String,
    calendarCount: Int,
    totalCount: Int,
    calendarPermissionGranted: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (alsoDeleteFromCalendar: Boolean) -> Unit,
) {
    val canTouchCalendar = calendarPermissionGranted && calendarCount > 0
    var alsoCalendar by remember(title) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(body)
                when {
                    calendarCount == 0 -> Text(
                        "Καμία από αυτές δεν έχει εγγραφή στο ημερολόγιο του κινητού.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    !calendarPermissionGranted -> Text(
                        "Δεν υπάρχει άδεια ημερολογίου, οπότε οι εγγραφές στο ημερολόγιο θα μείνουν.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { alsoCalendar = !alsoCalendar },
                    ) {
                        Checkbox(checked = alsoCalendar, onCheckedChange = { alsoCalendar = it })
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text("Διαγραφή και από το ημερολόγιο")
                            Text(
                                if (totalCount == 1) {
                                    "Αυτή η δουλειά έχει εγγραφή στο ημερολόγιο."
                                } else {
                                    "$calendarCount από τις $totalCount έχουν εγγραφή στο ημερολόγιο."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(alsoCalendar && canTouchCalendar) }) {
                Text("Διαγραφή", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Άκυρο") } },
    )
}
