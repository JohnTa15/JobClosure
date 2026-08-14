package gr.gtar.jobclosure.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.gtar.jobclosure.desktop.ui.components.AccentButton
import gr.gtar.jobclosure.desktop.ui.components.AmbientBackground
import gr.gtar.jobclosure.desktop.ui.components.NewIconButton
import gr.gtar.jobclosure.desktop.ui.components.NewSectionLabel
import gr.gtar.jobclosure.desktop.ui.components.NewSelectableSwatch
import gr.gtar.jobclosure.desktop.ui.theme.AppTheme
import gr.gtar.jobclosure.desktop.ui.theme.AppThemePalettes
import gr.gtar.jobclosure.desktop.ui.theme.NewUiColors
import gr.gtar.jobclosure.desktop.update.AppVersion
import gr.gtar.jobclosure.desktop.update.UpdateCheckResult
import gr.gtar.jobclosure.desktop.util.openInBrowser

/** Restyled settings screen - desktop port of the Android "NewSettingsScreen" ("Screen 4"). */
@Composable
fun DesktopSettingsScreen(
    state: AppUiState,
    onSetThemeKey: (String) -> Unit,
    onSaveGitHubToken: (String) -> Unit,
    onSaveDronePartnerEmail: (String) -> Unit,
    onCheckForUpdate: () -> Unit,
    onShowChangelogHistory: () -> Unit,
    onBack: () -> Unit,
) {
    var gitHubToken by remember(state.settings.gitHubToken) { mutableStateOf(state.settings.gitHubToken) }
    var dronePartnerEmail by remember(state.settings.dronePartnerEmail) { mutableStateOf(state.settings.dronePartnerEmail) }
    val activeTheme = AppTheme.fromKey(state.settings.themeKey)
    val palette = AppThemePalettes.getValue(activeTheme)

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(palette = palette, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                NewIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Πίσω", onClick = onBack, size = 42.dp)
                Text(
                    "Ρυθμίσεις",
                    color = NewUiColors.onGround,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NewUpdateCard(
                    state = state,
                    onCheckForUpdate = onCheckForUpdate,
                    onShowHistory = onShowChangelogHistory,
                    palette = palette,
                )

                ThemeSection(selected = activeTheme, onSelect = { onSetThemeKey(it.key) })

                Column {
                    NewSectionLabel(text = "Συνεργάτης drone", modifier = Modifier.padding(bottom = 7.dp))
                    SettingsField(
                        label = "Συνεργάτης drone",
                        value = dronePartnerEmail,
                        onValueChange = { value ->
                            dronePartnerEmail = value
                            onSaveDronePartnerEmail(value)
                        },
                        leadingIcon = Icons.Filled.Person,
                        iconTint = NewUiColors.droneChip,
                        accent = palette.accent,
                    )
                    Text(
                        "Email στο οποίο ειδοποιείται ο συνεργάτης για τις δουλειές με drone.",
                        color = NewUiColors.onGroundFaint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                Column {
                    NewSectionLabel(text = "GitHub token", modifier = Modifier.padding(bottom = 7.dp))
                    SettingsField(
                        label = "GitHub token",
                        value = gitHubToken,
                        onValueChange = { value ->
                            gitHubToken = value
                            onSaveGitHubToken(value)
                        },
                        leadingIcon = Icons.Filled.Key,
                        accent = palette.accent,
                    )
                    Text(
                        "Χρειάζεται μόνο αν το repository του project παραμένει private - fine-grained " +
                            "personal access token, scoped μόνο σε αυτό το repo, με δικαίωμα 'Contents: " +
                            "Read-only'. Αν το repo γίνει public, δεν χρειάζεται καθόλου.",
                        color = NewUiColors.onGroundFaint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    Column {
        NewSectionLabel(text = "Θέμα", modifier = Modifier.padding(bottom = 7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun NewUpdateCard(
    state: AppUiState,
    onCheckForUpdate: () -> Unit,
    onShowHistory: () -> Unit,
    palette: gr.gtar.jobclosure.desktop.ui.theme.AccentPalette,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.accentCardFillTop.let { Brush.verticalGradient(listOf(it, NewUiColors.surfaceSunken)) })
            .border(1.dp, palette.accentCardBorder, RoundedCornerShape(18.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = palette.accent, modifier = Modifier.size(17.dp))
                NewSectionLabel(text = "Ενημερώσεις", color = palette.accent, modifier = Modifier.padding(start = 8.dp))
            }

            when (val result = state.updateCheckResult) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Text("Διαθέσιμη έκδοση ${result.versionName}", color = NewUiColors.onGround, fontSize = 20.sp, modifier = Modifier.padding(top = 10.dp))
                    Text("Τρέχουσα: ${AppVersion.current}", color = NewUiColors.onGroundDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    AccentButton(
                        text = "Λήψη νέας έκδοσης",
                        onClick = { openInBrowser(result.downloadUrl) },
                        icon = Icons.Filled.Download,
                        borderColor = palette.accentBorder,
                        containerColor = palette.accentContainer,
                        contentColor = palette.onAccentContainer,
                        glowColor = palette.accentGlow,
                        height = 46.dp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                UpdateCheckResult.UpToDate -> Text(
                    "Έχεις την πιο πρόσφατη έκδοση (${AppVersion.current}).",
                    color = NewUiColors.onGroundDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                is UpdateCheckResult.Error -> Text(
                    result.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                null -> Text(
                    "Δεν έχει γίνει έλεγχος ακόμα.",
                    color = NewUiColors.onGroundDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Row(modifier = Modifier.padding(top = 14.dp)) {
                TextButton(onClick = onCheckForUpdate, enabled = !state.isCheckingForUpdate) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = NewUiColors.onGroundMuted, modifier = Modifier.size(15.dp))
                    Text("  Έλεγχος τώρα", color = NewUiColors.onGroundMuted, fontSize = 12.sp)
                }
                TextButton(onClick = onShowHistory) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = NewUiColors.onGroundMuted, modifier = Modifier.size(15.dp))
                    Text("  Ιστορικό", color = NewUiColors.onGroundMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color = accent,
) {
    Column(modifier = modifier) {
        NewSectionLabel(text = label, modifier = Modifier.padding(bottom = 7.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = NewUiColors.onGround, fontSize = 15.sp),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
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
                        Icon(leadingIcon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(end = 10.dp))
                    }
                    Box(modifier = Modifier.weight(1f)) { inner() }
                }
            },
        )
    }
}
