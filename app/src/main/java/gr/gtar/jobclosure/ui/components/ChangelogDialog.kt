package gr.gtar.jobclosure.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.shared.changelog.ChangelogEntry

/**
 * Shows one or more changelog entries: as the "what's new" popup right after an update (only the
 * entries not yet seen, oldest first so nothing skips past unread), or as the full history on
 * demand from Settings (every entry ever shipped).
 */
@Composable
fun ChangelogDialog(entries: List<ChangelogEntry>, onDismiss: () -> Unit, title: String = "Τι νέο υπάρχει") {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.NewReleases, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
            ) {
                entries.sortedByDescending { it.id }.forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (entries.size > 1) {
                            Text(
                                "Ενημέρωση #${entry.id}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        entry.items.forEach { item -> ChangelogItemRow(item) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Το κατάλαβα") }
        },
    )
}

@Composable
private fun ChangelogItemRow(text: String, icon: ImageVector = Icons.Filled.CheckCircle) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
