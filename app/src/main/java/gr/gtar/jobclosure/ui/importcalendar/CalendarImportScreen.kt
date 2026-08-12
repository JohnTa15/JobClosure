package gr.gtar.jobclosure.ui.importcalendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.gtar.jobclosure.calendar.CalendarHelper
import gr.gtar.jobclosure.ui.components.AccentButton
import gr.gtar.jobclosure.ui.components.AmbientBackground
import gr.gtar.jobclosure.ui.components.NewChip
import gr.gtar.jobclosure.ui.components.NewIconButton
import gr.gtar.jobclosure.ui.components.NewSectionLabel
import gr.gtar.jobclosure.ui.theme.AccentPalette
import gr.gtar.jobclosure.ui.theme.NewUiColors
import gr.gtar.jobclosure.ui.theme.typeColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val importDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale("el", "GR"))

/**
 * Reviews what the parser found in the device calendar before anything is written. Importing
 * straight from a scan would be faster, but a decade of calendar entries is guaranteed to contain
 * things that only look like sacraments, and unpicking a bad bulk import by hand is far more work
 * than ticking a list once.
 */
@Composable
fun CalendarImportScreen(
    viewModel: CalendarImportViewModel,
    palette: AccentPalette,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) viewModel.scan()
    }

    LaunchedEffect(Unit) {
        if (CalendarHelper.hasCalendarPermissions(context)) {
            viewModel.scan()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(palette = palette, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                NewIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Πίσω", onClick = onBack, size = 42.dp)
                Text(
                    "Εισαγωγή από ημερολόγιο",
                    color = NewUiColors.onGround,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }

            // Kept outside the result list: duplicates left over from an earlier import still need
            // clearing even when this scan turns up nothing new to add.
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                TextButton(onClick = { viewModel.removeDuplicateBookings() }) {
                    Icon(Icons.Filled.CleaningServices, contentDescription = null, tint = palette.accent, modifier = Modifier.size(15.dp))
                    Text("  Καθαρισμός διπλών δουλειών", color = palette.accent, fontSize = 12.sp)
                }
                state.duplicatesRemoved?.let { count ->
                    Text(
                        if (count == 0) "Δεν βρέθηκαν διπλές δουλειές." else "Διαγράφηκαν $count διπλές δουλειές.",
                        color = NewUiColors.success,
                        fontSize = 11.sp,
                    )
                }
            }

            when {
                state.missingPermission -> CenteredNote(
                    "Χρειάζεται άδεια ημερολογίου για να διαβαστούν τα παλιά μυστήρια.",
                )
                state.isScanning -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                // The filters stay on screen even when they leave nothing to show - hiding them
                // there would strand the user with an empty list and no way to widen it again.
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    ImportFilters(state = state, palette = palette, viewModel = viewModel)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    ) {
                        NewSectionLabel(
                            text = "${state.candidates.size} βρέθηκαν - ${state.selectedCount} επιλεγμένα",
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.setAllSelected(state.selectedCount < state.selectableCount) }) {
                            Text(
                                if (state.selectedCount < state.selectableCount) "Επιλογή όλων" else "Καθαρισμός",
                                color = palette.accent,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    if (state.candidates.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (state.calendars.isNotEmpty() && state.selectedCalendarIds.isEmpty()) {
                                    "Δεν έχει επιλεγεί κανένα ημερολόγιο."
                                } else {
                                    "Δεν βρέθηκαν γάμοι ή βαφτίσεις με αυτά τα φίλτρα."
                                },
                                color = NewUiColors.onGroundMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
                        ) {
                            items(state.candidates, key = { it.parsed.calendarEventId }) { candidate ->
                                CandidateRow(
                                    candidate = candidate,
                                    accent = palette.accent,
                                    onToggle = { viewModel.toggle(candidate.parsed.calendarEventId) },
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                        state.importedCount?.let { count ->
                            Text(
                                "Προστέθηκαν $count δουλειές.",
                                color = NewUiColors.success,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        AccentButton(
                            text = "Εισαγωγή ${state.selectedCount} επιλεγμένων",
                            onClick = { viewModel.importSelected() },
                            borderColor = palette.accentBorder,
                            containerColor = palette.accentContainer,
                            contentColor = palette.onAccentContainer,
                            glowColor = palette.accentGlow,
                            height = 48.dp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Which calendars to read and which year to show. Both narrow an already-completed scan rather than
 * re-reading the calendar, so switching between them is immediate.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportFilters(
    state: CalendarImportUiState,
    palette: AccentPalette,
    viewModel: CalendarImportViewModel,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 6.dp)) {
        if (state.calendars.size > 1) {
            NewSectionLabel(text = "Ημερολόγια", modifier = Modifier.padding(bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.calendars.forEach { calendar ->
                    val selected = calendar.id in state.selectedCalendarIds
                    FilterPill(
                        text = calendar.displayName.ifBlank { calendar.accountName },
                        selected = selected,
                        accent = palette,
                        onClick = { viewModel.setCalendarSelected(calendar.id, !selected) },
                    )
                }
            }
        }

        if (state.availableYears.isNotEmpty()) {
            NewSectionLabel(text = "Έτος", modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill(
                    text = "Όλα",
                    selected = state.selectedYear == null,
                    accent = palette,
                    onClick = { viewModel.setYear(null) },
                )
                state.availableYears.forEach { year ->
                    FilterPill(
                        text = year.toString(),
                        selected = state.selectedYear == year,
                        accent = palette,
                        onClick = { viewModel.setYear(if (state.selectedYear == year) null else year) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, accent: AccentPalette, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) accent.onAccentContainer else NewUiColors.onGroundDim,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accent.accentContainer else Color(0x80232532))
            .border(
                1.dp,
                if (selected) accent.accentBorder else NewUiColors.outlineSoft,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun CenteredNote(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = NewUiColors.onGroundFaint, modifier = Modifier.size(18.dp))
            Text(
                text,
                color = NewUiColors.onGroundMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

@Composable
private fun CandidateRow(candidate: ImportCandidate, accent: Color, onToggle: () -> Unit) {
    val parsed = candidate.parsed
    val colors = typeColors(parsed.type)
    val start = Instant.ofEpochMilli(parsed.startMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x73232532))
            .border(1.dp, if (candidate.selected) colors.chipBorder else NewUiColors.outlineSoft, RoundedCornerShape(14.dp))
            .clickable(enabled = !candidate.alreadyImported, onClick = onToggle)
            .padding(14.dp),
    ) {
        Icon(
            if (candidate.selected || candidate.alreadyImported) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = when {
                candidate.alreadyImported -> NewUiColors.onGroundFaint
                candidate.selected -> accent
                else -> NewUiColors.onGroundFaint
            },
            modifier = Modifier.size(20.dp),
        )

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                parsed.clientName.ifBlank { parsed.title },
                color = NewUiColors.onGround,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                start.format(importDateFormatter),
                color = NewUiColors.onGroundMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (parsed.venueAddress.isNotBlank()) {
                Text(
                    parsed.venueAddress,
                    color = NewUiColors.onGroundFaint,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                NewChip(
                    text = parsed.type.displayName,
                    textColor = colors.light,
                    borderColor = colors.chipBorder,
                    fillColor = colors.chipFill,
                )
                if (parsed.hasDrone) {
                    NewChip(
                        text = "Drone",
                        icon = Icons.Filled.FlightTakeoff,
                        textColor = NewUiColors.droneChip,
                        borderColor = NewUiColors.droneChip.copy(alpha = 0.35f),
                        fillColor = NewUiColors.droneChip.copy(alpha = 0.08f),
                    )
                }
                if (candidate.alreadyImported) {
                    NewChip(
                        text = "Ήδη στην εφαρμογή",
                        textColor = NewUiColors.onGroundFaint,
                        borderColor = NewUiColors.outlineSoft,
                        fillColor = Color.Transparent,
                    )
                }
            }
        }
    }
}
