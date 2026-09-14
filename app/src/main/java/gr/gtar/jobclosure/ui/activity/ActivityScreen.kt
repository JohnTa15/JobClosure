package gr.gtar.jobclosure.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.gtar.jobclosure.backup.BackupFile
import gr.gtar.jobclosure.data.ActivityAction
import gr.gtar.jobclosure.data.ActivityEntry
import gr.gtar.jobclosure.ui.components.AccentButton
import gr.gtar.jobclosure.ui.components.AmbientBackground
import gr.gtar.jobclosure.ui.components.NewIconButton
import gr.gtar.jobclosure.ui.components.NewSectionLabel
import gr.gtar.jobclosure.ui.theme.AccentPalette
import gr.gtar.jobclosure.ui.theme.NewUiColors
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val entryFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("el", "GR"))
private val backupFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale("el", "GR"))

/**
 * What the app has done, and the copies it keeps of it.
 *
 * The two belong on one screen: the history is how the user checks that something happened, and
 * the backups are what they reach for when it turns out something should not have. Both are local
 * to the phone - nothing here is uploaded anywhere.
 */
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    palette: AccentPalette,
    onBack: () -> Unit,
) {
    val entries by viewModel.entries.collectAsState()
    val backups by viewModel.backups.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val message by viewModel.message.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(palette = palette, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                NewIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Πίσω",
                    onClick = onBack,
                    size = 42.dp,
                )
                Text(
                    "Ιστορικό & αντίγραφα",
                    color = NewUiColors.onGround,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "backup-header") {
                    Column {
                        NewSectionLabel(text = "Αντίγραφα ασφαλείας", modifier = Modifier.padding(bottom = 8.dp))
                        AccentButton(
                            text = if (isWorking) "Δουλεύει..." else "Κράτα αντίγραφο τώρα",
                            onClick = { if (!isWorking) viewModel.backupNow() },
                            icon = Icons.Filled.Backup,
                            borderColor = palette.accentBorder,
                            containerColor = palette.accentContainer,
                            contentColor = palette.onAccentContainer,
                            glowColor = palette.accentGlow,
                            height = 46.dp,
                        )
                        Text(
                            "Κρατιέται ένα αντίγραφο την ημέρα, μόλις ανοίξεις την εφαρμογή. " +
                                "Περιέχει τις δουλειές σου με ό,τι γράφει το ημερολόγιο για την καθεμιά, " +
                                "και μένει στο κινητό - φυλάγονται τα 10 τελευταία.",
                            color = NewUiColors.onGroundFaint,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        message?.let { text ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                Text(text, color = NewUiColors.success, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.dismissMessage() }) {
                                    Text("OK", color = palette.accent, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (backups.isEmpty()) {
                    item(key = "no-backups") {
                        Text(
                            "Δεν υπάρχει ακόμα αντίγραφο.",
                            color = NewUiColors.onGroundMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                } else {
                    items(backups, key = { it.file.absolutePath }) { backup ->
                        BackupRow(
                            backup = backup,
                            accent = palette.accent,
                            enabled = !isWorking,
                            onRestore = { viewModel.restore(backup.file) },
                        )
                    }
                }

                item(key = "history-header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    ) {
                        NewSectionLabel(text = "Τι έγινε στην εφαρμογή", modifier = Modifier.weight(1f))
                        if (entries.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearHistory() }) {
                                Text("Καθαρισμός", color = palette.accent, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (entries.isEmpty()) {
                    item(key = "no-history") {
                        Text(
                            "Δεν έχει καταγραφεί ακόμα καμία ενέργεια. Από εδώ και πέρα γράφεται " +
                                "ό,τι προσθέτεις, αλλάζεις, διαγράφεις ή εισάγεις.",
                            color = NewUiColors.onGroundMuted,
                            fontSize = 12.sp,
                        )
                    }
                } else {
                    items(entries, key = { it.id }) { entry -> ActivityRow(entry = entry, accent = palette.accent) }
                }
            }
        }
    }
}

@Composable
private fun BackupRow(backup: BackupFile, accent: Color, enabled: Boolean, onRestore: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x73232532))
            .border(1.dp, NewUiColors.outlineSoft, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                backup.createdAt.format(backupFormatter),
                color = NewUiColors.onGround,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "${backup.bookingCount} δουλειές · ${backup.sizeBytes / 1024} KB",
                color = NewUiColors.onGroundFaint,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        TextButton(onClick = { if (enabled) onRestore() }) {
            Icon(Icons.Filled.Restore, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            Text("  Επαναφορά", color = accent, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActivityRow(entry: ActivityEntry, accent: Color) {
    val moment = LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.timestampEpochMillis), ZoneId.systemDefault())

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x59232532))
            .border(1.dp, NewUiColors.outlineSoft, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Icon(
            iconFor(entry.action),
            contentDescription = null,
            tint = if (entry.action == ActivityAction.BOOKING_DELETED) Color(0xFFFF6B6B) else accent,
            modifier = Modifier.size(17.dp).padding(top = 1.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Text(entry.action.label, color = NewUiColors.onGround, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (entry.subject.isNotBlank()) {
                Text(entry.subject, color = NewUiColors.onGroundMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            if (entry.details.isNotBlank()) {
                Text(entry.details, color = NewUiColors.onGroundFaint, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Text(
            moment.format(entryFormatter),
            color = NewUiColors.onGroundFaint,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private fun iconFor(action: ActivityAction): ImageVector = when (action) {
    ActivityAction.BOOKING_CREATED -> Icons.Filled.NoteAdd
    ActivityAction.BOOKING_UPDATED -> Icons.Filled.Edit
    ActivityAction.BOOKING_DELETED -> Icons.Filled.DeleteForever
    ActivityAction.CALENDAR_IMPORT -> Icons.Filled.EventRepeat
    ActivityAction.DUPLICATES_REMOVED -> Icons.Filled.CleaningServices
    ActivityAction.BACKUP_CREATED -> Icons.Filled.Backup
    ActivityAction.BACKUP_RESTORED -> Icons.Filled.Restore
}
