package gr.gtar.jobclosure.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.shared.changelog.CURRENT_CHANGELOG_ITEMS

/** Shown once right after an update, so what changed doesn't go unnoticed. */
@Composable
fun ChangelogDialog(onDismiss: () -> Unit, items: List<String> = CURRENT_CHANGELOG_ITEMS) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.NewReleases, contentDescription = null) },
        title = { Text("Τι νέο υπάρχει") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item -> ChangelogItemRow(item) }
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
