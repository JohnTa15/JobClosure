package gr.gtar.jobclosure.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.gtar.jobclosure.desktop.auth.EmbeddedGoogleCredentials
import gr.gtar.jobclosure.desktop.ui.components.AccentButton
import gr.gtar.jobclosure.desktop.ui.components.AmbientBackground
import gr.gtar.jobclosure.desktop.ui.components.NewSectionLabel
import gr.gtar.jobclosure.desktop.ui.theme.AppTheme
import gr.gtar.jobclosure.desktop.ui.theme.AppThemePalettes
import gr.gtar.jobclosure.desktop.ui.theme.NewUiColors
import gr.gtar.jobclosure.shared.calendar.GCalCalendarListEntry

/**
 * First-run screen: sign in via the system browser, then pick which calendar bookings should
 * sync through. Restyled to match the Android app's dark design. When this build has an embedded
 * Google OAuth Client ID/Secret (see EmbeddedGoogleCredentials), that's all there is to it - the
 * Client ID/Secret fields only show up as a fallback on a build without one baked in.
 */
@Composable
fun SignInScreen(
    state: AppUiState,
    onSaveCredentials: (clientId: String, clientSecret: String, dronePartnerEmail: String) -> Unit,
    onSignIn: () -> Unit,
    onSelectCalendar: (String) -> Unit,
) {
    var clientId by remember { mutableStateOf(state.settings.clientId) }
    var clientSecret by remember { mutableStateOf(state.settings.clientSecret) }
    var dronePartnerEmail by remember { mutableStateOf(state.settings.dronePartnerEmail) }
    val needsManualCredentials = !EmbeddedGoogleCredentials.isConfigured
    val activeTheme = AppTheme.fromKey(state.settings.themeKey)
    val palette = AppThemePalettes.getValue(activeTheme)

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(palette = palette, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC191B28))
                    .border(1.dp, palette.accentCardBorder, RoundedCornerShape(20.dp))
                    .padding(26.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NewSectionLabel(text = "JobClosure", color = palette.accent)
                Text(
                    "Συνδέστε το Google Calendar που χρησιμοποιεί ήδη το κινητό σας, ώστε οι κρατήσεις " +
                        "να συγχρονίζονται και στους δύο υπολογιστές.",
                    color = NewUiColors.onGroundMuted,
                    fontSize = 14.sp,
                )

                if (state.calendars.isEmpty()) {
                    if (needsManualCredentials) {
                        Text(
                            "Ο τύπος του OAuth client πρέπει να είναι \"Desktop app\". Ένα " +
                                "\"Web application\" client απορρίπτει τη σύνδεση με σφάλμα " +
                                "redirect_uri_mismatch.",
                            color = NewUiColors.onGroundFaint,
                            fontSize = 12.sp,
                        )
                        NewField(
                            label = "Google OAuth Client ID",
                            value = clientId,
                            onValueChange = { clientId = it },
                            accent = palette.accent,
                        )
                        NewField(
                            label = "Google OAuth Client Secret",
                            value = clientSecret,
                            onValueChange = { clientSecret = it },
                            accent = palette.accent,
                        )
                    }
                    NewField(
                        label = "Email συνεργάτη drone (προαιρετικό)",
                        value = dronePartnerEmail,
                        onValueChange = { dronePartnerEmail = it },
                        accent = palette.accent,
                    )

                    val canSignIn = !state.isLoading && (!needsManualCredentials || (clientId.isNotBlank() && clientSecret.isNotBlank()))
                    AccentButton(
                        text = if (state.isLoading) "Σύνδεση..." else "Σύνδεση με Google",
                        onClick = {
                            if (canSignIn) {
                                onSaveCredentials(clientId.trim(), clientSecret.trim(), dronePartnerEmail.trim())
                                onSignIn()
                            }
                        },
                        icon = Icons.Filled.Login,
                        borderColor = if (canSignIn) palette.accentBorder else NewUiColors.outline,
                        containerColor = if (canSignIn) palette.accentContainer else Color(0x60232532),
                        contentColor = palette.onAccentContainer,
                        glowColor = if (canSignIn) palette.accentGlow else Color.Transparent,
                        height = 50.dp,
                    )
                } else {
                    Text(
                        "Επιλέξτε ημερολόγιο συγχρονισμού:",
                        color = NewUiColors.onGround,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    CalendarDropdown(
                        calendars = state.calendars,
                        selectedId = state.settings.calendarId,
                        onSelect = onSelectCalendar,
                        accent = palette.accent,
                    )
                }

                if (state.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = palette.accent)
                        Text(
                            state.statusMessage ?: "Παρακαλώ περίμενε...",
                            color = NewUiColors.onGroundMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CalendarDropdown(
    calendars: List<GCalCalendarListEntry>,
    selectedId: String,
    onSelect: (String) -> Unit,
    accent: Color,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = calendars.firstOrNull { it.id == selectedId }?.summary ?: "Επιλέξτε ημερολόγιο"

    Column {
        NewSectionLabel(text = "Ημερολόγιο", modifier = Modifier.padding(bottom = 7.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x80232532))
                .border(1.dp, NewUiColors.outline, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(14.dp),
        ) {
            Text(
                selectedLabel,
                color = NewUiColors.onGround,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
            Text("Επιλογή", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            calendars.forEach { calendar ->
                DropdownMenuItem(
                    text = { Text(calendar.summary) },
                    onClick = {
                        expanded = false
                        onSelect(calendar.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun NewField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
) {
    Column {
        NewSectionLabel(text = label, modifier = Modifier.padding(bottom = 7.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = NewUiColors.onGround, fontSize = 15.sp),
            cursorBrush = SolidColor(accent),
            singleLine = true,
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
                    Box(modifier = Modifier.weight(1f)) { innerTextField() }
                }
            },
        )
    }
}
