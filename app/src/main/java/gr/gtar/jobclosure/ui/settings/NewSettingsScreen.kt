package gr.gtar.jobclosure.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.gtar.jobclosure.BuildConfig
import gr.gtar.jobclosure.data.MapsProvider
import gr.gtar.jobclosure.location.LocationHelper
import gr.gtar.jobclosure.location.LocationResult
import gr.gtar.jobclosure.shared.changelog.CHANGELOG_HISTORY
import gr.gtar.jobclosure.ui.components.AccentButton
import gr.gtar.jobclosure.ui.components.AmbientBackground
import gr.gtar.jobclosure.ui.components.AutocompleteAddressField
import gr.gtar.jobclosure.ui.components.ChangelogDialog
import gr.gtar.jobclosure.ui.components.NewIconButton
import gr.gtar.jobclosure.ui.components.NewSectionLabel
import gr.gtar.jobclosure.ui.components.NewSelectableSwatch
import gr.gtar.jobclosure.ui.components.NewSwitch
import gr.gtar.jobclosure.ui.theme.AppTheme
import gr.gtar.jobclosure.ui.theme.AppThemePalettes
import gr.gtar.jobclosure.ui.theme.NewUiColors
import gr.gtar.jobclosure.update.ApkUpdateManager
import gr.gtar.jobclosure.update.DownloadResult
import gr.gtar.jobclosure.update.UpdateCheckResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Restyled settings screen - see design_handoff_theme_switcher/README.md "Screen 4". Also hosts
 *  the "Νέα εμφάνιση" switch itself, so it's reachable from either UI to turn the restyle off again. */
@Composable
fun NewSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onImportFromCalendar: () -> Unit) {
    var homeAddress by remember { mutableStateOf("") }
    var mapsApiKey by remember { mutableStateOf("") }
    var mapsProvider by remember { mutableStateOf(MapsProvider.OPENSTREETMAP) }
    var reminderMinutes by remember { mutableStateOf("120") }
    var dronePartnerEmail by remember { mutableStateOf("") }
    var gitHubToken by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var showChangelogHistory by remember { mutableStateOf(false) }
    var justSaved by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val activeTheme = AppTheme.fromKey(settings.themeKey)
    val palette = AppThemePalettes.getValue(activeTheme)

    suspend fun useCurrentLocation() {
        locationError = null
        isLocating = true
        when (val location = LocationHelper.getCurrentLocation(context)) {
            is LocationResult.Success -> {
                val suggestion = viewModel.placeSearchRepository.reverseGeocode(location.latitude, location.longitude)
                if (suggestion != null) {
                    homeAddress = suggestion.fullText
                } else {
                    locationError = "Δεν βρέθηκε διεύθυνση για αυτή την τοποθεσία"
                }
            }
            is LocationResult.Error -> locationError = location.message
        }
        isLocating = false
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) {
            scope.launch { useCurrentLocation() }
        } else {
            locationError = "Χρειάζεται άδεια τοποθεσίας για αυτή τη λειτουργία"
        }
    }

    LaunchedEffect(Unit) {
        val current = viewModel.currentSettings()
        homeAddress = current.homeAddress
        mapsApiKey = current.mapsApiKey
        mapsProvider = current.mapsProvider
        reminderMinutes = current.reminderMinutesBefore.toString()
        dronePartnerEmail = current.dronePartnerEmail
        gitHubToken = current.gitHubToken
        loaded = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(palette = palette, modifier = Modifier.fillMaxSize())

        if (!loaded) return@Box

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
                NewUpdateCard(viewModel = viewModel, palette = AppThemePalettes.getValue(activeTheme), onShowHistory = { showChangelogHistory = true })

                DesignSection(settings = settings, viewModel = viewModel, accent = palette.accent)

                Column {
                    NewSectionLabel(text = "Παλιά μυστήρια", modifier = Modifier.padding(bottom = 7.dp))
                    AccentButton(
                        text = "Εισαγωγή από το ημερολόγιο",
                        onClick = onImportFromCalendar,
                        icon = Icons.Filled.EventRepeat,
                        borderColor = palette.accentBorder,
                        containerColor = palette.accentContainer,
                        contentColor = palette.onAccentContainer,
                        glowColor = palette.accentGlow,
                        height = 46.dp,
                    )
                    Text(
                        "Ψάχνει στο ημερολόγιο του κινητού για γάμους και βαφτίσεις που έχεις " +
                            "γράψει με το χέρι, και τα προσθέτει σαν δουλειές - μαζί με drone, " +
                            "τοποθεσία και ώρα.",
                        color = NewUiColors.onGroundFaint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                ThemeSection(selected = activeTheme, onSelect = { viewModel.setThemeKey(it.key) })

                Column {
                    NewSectionLabel(text = "Διεύθυνση σπιτιού", modifier = Modifier.padding(bottom = 7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AutocompleteAddressField(
                            value = homeAddress,
                            onValueChange = { homeAddress = it },
                            label = "Διεύθυνση σπιτιού",
                            provider = mapsProvider,
                            googleApiKey = mapsApiKey,
                            placeSearchRepository = viewModel.placeSearchRepository,
                            modifier = Modifier.weight(1f),
                        )
                        NewIconButton(
                            icon = Icons.Filled.MyLocation,
                            contentDescription = "Χρήση τρέχουσας τοποθεσίας",
                            onClick = {
                                if (LocationHelper.hasLocationPermission(context)) {
                                    scope.launch { useCurrentLocation() }
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                                    )
                                }
                            },
                            iconColor = palette.accent,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    locationError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    Text(
                        "Αφετηρία για τον υπολογισμό χρόνου διαδρομής.",
                        color = NewUiColors.onGroundFaint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                Column {
                    NewSectionLabel(text = "Πάροχος χαρτών", modifier = Modifier.padding(bottom = 7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProviderButton(
                            text = "OpenStreetMap",
                            icon = Icons.Filled.Public,
                            selected = mapsProvider == MapsProvider.OPENSTREETMAP,
                            accent = palette,
                            onClick = {
                                mapsProvider = MapsProvider.OPENSTREETMAP
                                viewModel.setMapsProvider(MapsProvider.OPENSTREETMAP)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        ProviderButton(
                            text = "Google Maps",
                            icon = Icons.Filled.Map,
                            selected = mapsProvider == MapsProvider.GOOGLE,
                            accent = palette,
                            onClick = {
                                mapsProvider = MapsProvider.GOOGLE
                                viewModel.setMapsProvider(MapsProvider.GOOGLE)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        if (mapsProvider == MapsProvider.OPENSTREETMAP) {
                            "Nominatim + OSRM - δωρεάν, χωρίς κλειδί API."
                        } else {
                            "Χρειάζεται Google Cloud API key με Directions + Geocoding."
                        },
                        color = NewUiColors.onGroundFaint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (mapsProvider == MapsProvider.GOOGLE) {
                        SettingsField(
                            label = "Κλειδί Google Maps API",
                            value = mapsApiKey,
                            onValueChange = { mapsApiKey = it },
                            accent = palette.accent,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsField(
                        label = "Υπενθύμιση πριν (λεπτά)",
                        value = reminderMinutes,
                        onValueChange = { value -> reminderMinutes = value },
                        leadingIcon = Icons.Filled.NotificationsActive,
                        iconTint = NewUiColors.unconfirmedMarker,
                        keyboardType = KeyboardType.Number,
                        accent = palette.accent,
                        modifier = Modifier.weight(1f),
                    )
                    SettingsField(
                        label = "Συνεργάτης drone",
                        value = dronePartnerEmail,
                        onValueChange = { dronePartnerEmail = it },
                        leadingIcon = Icons.Filled.Person,
                        iconTint = NewUiColors.droneChip,
                        keyboardType = KeyboardType.Email,
                        accent = palette.accent,
                        modifier = Modifier.weight(1f),
                    )
                }

                val updateStatusForToken by viewModel.updateStatus.collectAsState()
                Column {
                    SettingsField(
                        label = "GitHub token",
                        value = gitHubToken,
                        onValueChange = { gitHubToken = it },
                        leadingIcon = Icons.Filled.Key,
                        accent = palette.accent,
                    )
                    if (settings.gitHubToken.isNotBlank()) {
                        val (message, color) = when (updateStatusForToken) {
                            is UpdateCheckResult.UpdateAvailable, UpdateCheckResult.UpToDate ->
                                "Αποθηκευμένο και επιβεβαιωμένο." to NewUiColors.success
                            is UpdateCheckResult.Error ->
                                "Αποθηκευμένο, αλλά ο τελευταίος έλεγχος απέτυχε." to MaterialTheme.colorScheme.error
                            null -> "Αποθηκευμένο - πάτα \"Έλεγχος τώρα\" στο κάρτα ενημερώσεων." to NewUiColors.onGroundFaint
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            if (color == NewUiColors.success) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                                Box(modifier = Modifier.padding(start = 6.dp))
                            }
                            Text(message, color = color, fontSize = 11.sp)
                        }
                    }
                }

                Column {
                    AccentButton(
                        text = if (justSaved) "Αποθηκεύτηκε" else "Αποθήκευση & εφαρμογή",
                        onClick = {
                            viewModel.setHomeAddress(homeAddress)
                            viewModel.setMapsApiKey(mapsApiKey)
                            reminderMinutes.toIntOrNull()?.let { viewModel.setReminderMinutesBefore(it) }
                            viewModel.setDronePartnerEmail(dronePartnerEmail)
                            viewModel.setGitHubToken(gitHubToken)
                            justSaved = true
                        },
                        icon = if (justSaved) Icons.Filled.CheckCircle else Icons.Filled.Save,
                        borderColor = palette.accentBorder,
                        containerColor = palette.accentContainer,
                        contentColor = palette.onAccentContainer,
                        glowColor = palette.accentGlow,
                        height = 46.dp,
                    )
                    Text(
                        "Αποθηκεύει τη διεύθυνση σπιτιού, το κλειδί Google Maps, την υπενθύμιση, τον συνεργάτη drone και το GitHub token.",
                        color = NewUiColors.onGroundFaint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }

    LaunchedEffect(justSaved) {
        if (justSaved) {
            delay(2000)
            justSaved = false
        }
    }

    if (showChangelogHistory) {
        ChangelogDialog(entries = CHANGELOG_HISTORY, onDismiss = { showChangelogHistory = false }, title = "Ιστορικό ενημερώσεων")
    }
}

@Composable
private fun DesignSection(settings: gr.gtar.jobclosure.data.AppSettings, viewModel: SettingsViewModel, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x73232532))
            .border(1.dp, NewUiColors.outlineSoft, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Νέα εμφάνιση", color = NewUiColors.onGround, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "Το restyle με θέματα χρωμάτων και κινούμενα εφέ. Μπορείς να το απενεργοποιήσεις όποτε θέλεις.",
                color = NewUiColors.onGroundFaint,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        NewSwitch(
            checked = settings.useNewDesign,
            onColor = accent,
            onCheckedChange = { enabled -> viewModel.setUseNewDesign(enabled) },
            modifier = Modifier.padding(start = 12.dp),
        )
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
    }
}

@Composable
private fun NewUpdateCard(
    viewModel: SettingsViewModel,
    palette: gr.gtar.jobclosure.ui.theme.AccentPalette,
    onShowHistory: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var showInstallPermissionDialog by remember { mutableStateOf(false) }

    val infinite = rememberInfiniteTransition(label = "update-card-pulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.accentCardFillTop.let { Brush.verticalGradient(listOf(it, NewUiColors.surfaceSunken)) })
            .border(1.dp, palette.accentCardBorder, RoundedCornerShape(18.dp)),
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .alpha(pulseAlpha)
                .blur(18.dp)
                .background(Brush.radialGradient(listOf(palette.blob1, Color.Transparent)), CircleShape),
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = palette.accent, modifier = Modifier.size(17.dp))
                NewSectionLabel(text = "Ενημερώσεις", color = palette.accent, modifier = Modifier.padding(start = 8.dp))
            }

            when (val status = updateStatus) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Text("Διαθέσιμη έκδοση ${status.versionName}", color = NewUiColors.onGround, fontSize = 20.sp, modifier = Modifier.padding(top = 10.dp))
                    Text("Τρέχουσα: ${BuildConfig.VERSION_NAME}", color = NewUiColors.onGroundDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    AccentButton(
                        text = if (isDownloading) "Λήψη..." else "Λήψη & εγκατάσταση",
                        onClick = {
                            if (!ApkUpdateManager.canInstallUnknownApps(context)) {
                                showInstallPermissionDialog = true
                            } else {
                                downloadError = null
                                isDownloading = true
                                scope.launch {
                                    val result = ApkUpdateManager.downloadUpdate(
                                        context = context,
                                        downloadUrl = status.downloadUrl,
                                        gitHubToken = settings.gitHubToken,
                                        expectedSizeBytes = status.expectedSizeBytes,
                                        expectedSha256 = status.expectedSha256,
                                    )
                                    when (result) {
                                        is DownloadResult.Success -> {
                                            isDownloading = false
                                            ApkUpdateManager.promptInstall(context, result.apkUri)
                                        }
                                        is DownloadResult.Error -> {
                                            isDownloading = false
                                            downloadError = result.message
                                        }
                                    }
                                }
                            }
                        },
                        icon = Icons.Filled.Download,
                        borderColor = palette.accentBorder,
                        containerColor = palette.accentContainer,
                        contentColor = palette.onAccentContainer,
                        glowColor = palette.accentGlow,
                        height = 46.dp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    downloadError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                UpdateCheckResult.UpToDate -> Text(
                    "Έχεις την πιο πρόσφατη έκδοση (${BuildConfig.VERSION_NAME}).",
                    color = NewUiColors.onGroundDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                is UpdateCheckResult.Error -> Text(
                    status.message,
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
                TextButton(onClick = { viewModel.checkForUpdateNow() }) {
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

    if (showInstallPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showInstallPermissionDialog = false },
            title = { Text("Άδεια εγκατάστασης") },
            text = { Text("Για να εγκατασταθεί η ενημέρωση, χρειάζεται να επιτρέψεις στο JobClosure να εγκαθιστά εφαρμογές από άγνωστες πηγές.") },
            confirmButton = {
                TextButton(onClick = {
                    showInstallPermissionDialog = false
                    ApkUpdateManager.openInstallPermissionSettings(context)
                }) { Text("Ρυθμίσεις") }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPermissionDialog = false }) { Text("Άκυρο") }
            },
        )
    }
}

@Composable
private fun ProviderButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: gr.gtar.jobclosure.ui.theme.AccentPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.accentContainer else Color(0x80232532))
            .border(1.dp, if (selected) accent.accentBorder else NewUiColors.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) accent.onAccentContainer else NewUiColors.onGroundDim, modifier = Modifier.size(16.dp))
        Text(
            text,
            color = if (selected) accent.onAccentContainer else NewUiColors.onGroundDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = accent,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = modifier) {
        NewSectionLabel(text = label, modifier = Modifier.padding(bottom = 7.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(color = NewUiColors.onGround, fontSize = 15.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x80232532), RoundedCornerShape(12.dp))
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
