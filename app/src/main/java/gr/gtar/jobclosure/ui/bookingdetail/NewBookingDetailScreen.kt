package gr.gtar.jobclosure.ui.bookingdetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import gr.gtar.jobclosure.data.Booking
import gr.gtar.jobclosure.data.MapsProvider
import gr.gtar.jobclosure.network.DroneConditionsResult
import gr.gtar.jobclosure.network.TravelTimeResult
import gr.gtar.jobclosure.ui.components.AccentButton
import gr.gtar.jobclosure.ui.components.AmbientBackground
import gr.gtar.jobclosure.ui.components.MiniMapPreview
import gr.gtar.jobclosure.ui.components.NewChip
import gr.gtar.jobclosure.ui.components.NewIconButton
import gr.gtar.jobclosure.ui.components.NewSectionLabel
import gr.gtar.jobclosure.ui.components.VenuePhotoPreview
import gr.gtar.jobclosure.ui.theme.AppTheme
import gr.gtar.jobclosure.ui.theme.AppThemePalettes
import gr.gtar.jobclosure.ui.theme.NewUiColors
import gr.gtar.jobclosure.ui.theme.typeColors
import gr.gtar.jobclosure.data.BookingType
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

private val newDateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm", Locale("el", "GR"))
private val newShortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("el", "GR"))
private val CountUpEasing = Easing { fraction -> 1f - (1f - fraction).pow(3) }

private sealed interface NewTravelState {
    data object Loading : NewTravelState
    data class Success(val text: String) : NewTravelState
    data class Error(val message: String) : NewTravelState
    data object Empty : NewTravelState
}

private sealed interface NewWeatherState {
    data object Loading : NewWeatherState
    data class Success(val conditions: DroneConditionsResult.Success) : NewWeatherState
    data class Error(val message: String) : NewWeatherState
    data object Empty : NewWeatherState
}

/** Restyled booking detail - see design_handoff_theme_switcher/README.md "Screen 2". */
@Composable
fun NewBookingDetailScreen(
    viewModel: BookingDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activeTheme = AppTheme.fromKey(state.settings.themeKey)
    val palette = AppThemePalettes.getValue(activeTheme)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val booking = state.booking
    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(palette = palette, modifier = Modifier.fillMaxSize())

        if (state.isLoading || booking == null) return@Box

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                NewIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Πίσω", onClick = onBack, size = 42.dp)
                Box(modifier = Modifier.weight(1f))
                NewIconButton(icon = Icons.Filled.Edit, contentDescription = "Επεξεργασία", onClick = { onEdit(booking.id) }, size = 42.dp)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 32.dp),
            ) {
                val typeAccent = typeColors(booking.type)

                NewSectionLabel(text = booking.type.displayName, color = typeAccent.light)
                Text(
                    booking.title,
                    color = NewUiColors.onGround,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    booking.ceremonyStart.format(newDateTimeFormatter).replaceFirstChar { it.titlecase(Locale("el", "GR")) },
                    color = NewUiColors.onGroundMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    if (booking.clientPhone.isNotBlank()) {
                        NewChip(
                            text = booking.clientPhone,
                            icon = Icons.Filled.Phone,
                            textColor = NewUiColors.onGroundMuted,
                            borderColor = NewUiColors.outline,
                            fillColor = Color.Transparent,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${booking.clientPhone}")))
                            },
                        )
                    }
                    if (booking.price > 0.0) {
                        NewChip(
                            text = String.format(Locale.US, "%.0f€", booking.price),
                            icon = Icons.Filled.Euro,
                            textColor = NewUiColors.onGroundMuted,
                            borderColor = NewUiColors.outline,
                            fillColor = Color.Transparent,
                        )
                    }
                    if (booking.hasDrone) {
                        NewChip(
                            text = "Με drone",
                            icon = Icons.Filled.FlightTakeoff,
                            textColor = NewUiColors.droneChip,
                            borderColor = NewUiColors.droneChip.copy(alpha = 0.35f),
                            fillColor = NewUiColors.droneChip.copy(alpha = 0.08f),
                        )
                    }
                }

                NewLocationPanel(
                    icon = Icons.Filled.Church,
                    label = "Εκκλησία",
                    name = booking.churchName,
                    address = booking.churchAddress,
                    travelLabel = "Από το σπίτι",
                    travelState = state.homeToChurch.toNewState(state.isLoadingTravelTimes),
                    accent = palette.accent,
                    accentBorder = palette.accentBorder,
                    accentContainer = palette.accentContainer,
                    onAccentContainer = palette.onAccentContainer,
                    onNavigate = {
                        openNewDirections(context, state.settings.homeAddress, booking.churchAddress, state.settings.mapsProvider, state.homeToChurch)
                    },
                    modifier = Modifier.padding(top = 20.dp),
                )

                NewWeatherPanel(
                    hasDrone = booking.hasDrone,
                    weatherState = state.churchDroneConditions.toNewState(state.isLoadingDroneConditions),
                    forecastDate = booking.ceremonyStart.toLocalDate(),
                    modifier = Modifier.padding(top = 16.dp),
                )

                if (booking.hasReception) {
                    NewLocationPanel(
                        icon = Icons.Filled.Celebration,
                        label = "Δεξίωση",
                        name = booking.receptionVenueName,
                        address = booking.receptionVenueAddress,
                        subtitle = booking.receptionStart?.format(newDateTimeFormatter)?.replaceFirstChar { it.titlecase(Locale("el", "GR")) },
                        travelLabel = "Από την εκκλησία",
                        travelState = state.churchToReception.toNewState(state.isLoadingTravelTimes),
                        accent = Color(0xFFFF8FB0),
                        accentBorder = NewUiColors.receptionChip,
                        accentContainer = NewUiColors.receptionChip.copy(alpha = 0.18f),
                        onAccentContainer = Color(0xFFFFD7E4),
                        onNavigate = {
                            openNewDirections(context, booking.churchAddress, booking.receptionVenueAddress, state.settings.mapsProvider, state.churchToReception)
                        },
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    NewWeatherPanel(
                        hasDrone = booking.hasDrone,
                        weatherState = state.receptionDroneConditions.toNewState(state.isLoadingDroneConditions),
                        forecastDate = (booking.receptionStart ?: booking.ceremonyStart).toLocalDate(),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                if (booking.notes.isNotBlank()) {
                    NewPanel(borderColor = NewUiColors.outlineSoft, fillColor = Color(0x80232532), modifier = Modifier.padding(top = 16.dp)) {
                        NewSectionLabel(text = "Σημειώσεις")
                        Text(booking.notes, color = NewUiColors.onGroundMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                state.mapPreviewCoordinates?.let { (lat, lon) ->
                    Box(modifier = Modifier.padding(top = 16.dp)) {
                        MiniMapPreview(latitude = lat, longitude = lon, modifier = Modifier.fillMaxWidth())
                    }
                }

                if (state.settings.mapsProvider == MapsProvider.GOOGLE && state.settings.mapsApiKey.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                        if (booking.churchAddress.isNotBlank()) {
                            VenuePhotoPreview(
                                label = "Εκκλησία",
                                query = listOfNotNull(booking.churchName.ifBlank { null }, booking.churchAddress.ifBlank { null }).joinToString(", "),
                                apiKey = state.settings.mapsApiKey,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (booking.hasReception && booking.receptionVenueAddress.isNotBlank()) {
                            VenuePhotoPreview(
                                label = "Δεξίωση",
                                query = listOfNotNull(booking.receptionVenueName.ifBlank { null }, booking.receptionVenueAddress.ifBlank { null }).joinToString(", "),
                                apiKey = state.settings.mapsApiKey,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun TravelTimeResult?.toNewState(isLoading: Boolean): NewTravelState = when {
    isLoading -> NewTravelState.Loading
    this is TravelTimeResult.Success -> NewTravelState.Success(durationText)
    this is TravelTimeResult.Error -> NewTravelState.Error(message)
    else -> NewTravelState.Empty
}

private fun DroneConditionsResult?.toNewState(isLoading: Boolean): NewWeatherState = when {
    isLoading -> NewWeatherState.Loading
    this is DroneConditionsResult.Success -> NewWeatherState.Success(this)
    this is DroneConditionsResult.Error -> NewWeatherState.Error(message)
    else -> NewWeatherState.Empty
}

@Composable
private fun NewPanel(
    borderColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier,
    hairlineColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(fillColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        if (hairlineColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, hairlineColor, Color.Transparent)))
                    .padding(bottom = 4.dp),
            )
        }
        content()
    }
}

@Composable
private fun NewLocationPanel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    name: String,
    address: String,
    travelLabel: String,
    travelState: NewTravelState,
    accent: Color,
    accentBorder: Color,
    accentContainer: Color,
    onAccentContainer: Color,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    NewPanel(
        borderColor = NewUiColors.outlineSoft,
        fillColor = Color(0x80232532),
        hairlineColor = accent.copy(alpha = 0.70f),
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
            NewSectionLabel(text = label)
        }
        if (name.isNotBlank()) {
            Text(name, color = NewUiColors.onGround, fontSize = 19.sp, modifier = Modifier.padding(top = 10.dp))
        }
        if (address.isNotBlank()) {
            Text(address, color = NewUiColors.onGroundDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        subtitle?.let {
            Text(it, color = NewUiColors.onGroundDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = NewUiColors.onGroundMuted, modifier = Modifier.size(16.dp))
            Text(travelLabel, color = NewUiColors.onGroundMuted, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(NewUiColors.outline, Color.Transparent))),
            )
            when (travelState) {
                is NewTravelState.Loading -> CircularProgressIndicator(modifier = Modifier.size(14.dp))
                is NewTravelState.Success -> Text(travelState.text, color = NewUiColors.onGround, fontSize = 15.sp)
                is NewTravelState.Error -> Text(travelState.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                NewTravelState.Empty -> {}
            }
        }

        if (address.isNotBlank()) {
            AccentButton(
                text = "Άνοιγμα διαδρομής",
                onClick = onNavigate,
                icon = Icons.Filled.Navigation,
                borderColor = accentBorder,
                containerColor = accentContainer,
                contentColor = onAccentContainer,
                glowColor = accentBorder.copy(alpha = 0.0f),
                glowRadius = 0.dp,
                height = 36.dp,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun NewWeatherPanel(
    hasDrone: Boolean,
    weatherState: NewWeatherState,
    forecastDate: LocalDate?,
    modifier: Modifier = Modifier,
) {
    if (weatherState is NewWeatherState.Empty) return
    val borderColor = if (hasDrone) NewUiColors.droneChip.copy(alpha = 0.28f) else NewUiColors.outlineSoft
    val fillColor = if (hasDrone) NewUiColors.droneChip.copy(alpha = 0.10f) else Color(0x80232532)

    NewPanel(borderColor = borderColor, fillColor = fillColor, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Air, contentDescription = null, tint = NewUiColors.success, modifier = Modifier.size(17.dp))
            NewSectionLabel(text = if (hasDrone) "Συνθήκες πτήσης" else "Καιρός", color = NewUiColors.success, modifier = Modifier.padding(start = 8.dp))
            Box(modifier = Modifier.weight(1f))
            if (forecastDate != null) {
                Text("πρόβλεψη ${forecastDate.format(newShortDateFormatter)}", color = NewUiColors.onGroundFaint, fontSize = 10.sp)
            }
        }

        when (weatherState) {
            NewWeatherState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp).width(18.dp).height(18.dp))
            is NewWeatherState.Error -> Text(weatherState.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            NewWeatherState.Empty -> {}
            is NewWeatherState.Success -> {
                val c = weatherState.conditions
                val temp = remember(c) { Animatable(0f) }
                val wind = remember(c) { Animatable(0f) }
                val rain = remember(c) { Animatable(0f) }
                val elevation = remember(c) { Animatable(0f) }
                androidx.compose.runtime.LaunchedEffect(c) {
                    launch { temp.animateTo(c.temperatureC.toFloat(), tween(750, easing = CountUpEasing)) }
                    launch { wind.animateTo(c.windSpeedKmh.toFloat(), tween(750, easing = CountUpEasing)) }
                    launch { rain.animateTo((c.precipitationProbabilityPercent ?: 0).toFloat(), tween(750, easing = CountUpEasing)) }
                    launch { elevation.animateTo(c.elevationMeters.toFloat(), tween(750, easing = CountUpEasing)) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    StatTile(
                        icon = Icons.Filled.Thermostat,
                        iconTint = NewUiColors.unconfirmedMarker,
                        value = "${temp.value.roundToInt()}°",
                        caption = c.weatherDescription,
                        modifier = Modifier.weight(1f),
                    )
                    if (hasDrone) {
                        StatTile(
                            icon = Icons.Filled.Air,
                            iconTint = NewUiColors.success,
                            value = "${wind.value.roundToInt()}",
                            valueSuffix = " km/h",
                            caption = windDirectionLabel(c.windDirectionDeg),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (c.precipitationProbabilityPercent != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Text("Πιθανότητα βροχής", color = NewUiColors.onGroundMuted, fontSize = 12.sp, modifier = Modifier.width(118.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xB30D0E17)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((rain.value / 100f).coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Brush.horizontalGradient(listOf(NewUiColors.success, NewUiColors.success))),
                            )
                        }
                        Text(
                            "${rain.value.roundToInt()}%",
                            color = NewUiColors.onGround,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            modifier = Modifier.width(34.dp),
                        )
                    }
                }
                if (c.isRainy) {
                    Text(
                        "Πάρε ομπρέλα - υπάρχει πιθανότητα βροχής.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                if (hasDrone) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Υψόμετρο περιοχής", color = NewUiColors.onGroundMuted, fontSize = 12.sp, modifier = Modifier.width(118.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .height(1.dp)
                                .background(Brush.horizontalGradient(listOf(NewUiColors.outline, Color.Transparent))),
                        )
                        Text("${elevation.value.roundToInt()} μ.", color = NewUiColors.onGround, fontSize = 12.sp)
                    }

                    val dagrContext = LocalContext.current
                    AccentButton(
                        text = "Έλεγχος στο Drone Aware - GR",
                        onClick = { dagrContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DAGR_URL))) },
                        icon = Icons.Filled.Verified,
                        borderColor = NewUiColors.droneChip.copy(alpha = 0.5f),
                        containerColor = Color.Transparent,
                        contentColor = NewUiColors.success,
                        glowColor = NewUiColors.droneChip.copy(alpha = 0.35f),
                        glowRadius = 26.dp,
                        height = 36.dp,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        "Πάντα έλεγχε εκεί πριν πετάξεις - ειδικά κοντά σε αεροδρόμια ή στρατιωτικές περιοχές.",
                        color = NewUiColors.onGroundFaint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    valueSuffix: String = "",
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NewUiColors.wellDark)
            .border(1.dp, NewUiColors.outlineSoft, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
            Text(value, color = NewUiColors.onGround, fontSize = 26.sp, fontWeight = FontWeight.Medium)
            if (valueSuffix.isNotBlank()) {
                Text(valueSuffix, color = NewUiColors.onGroundDim, fontSize = 12.sp)
            }
        }
        Text(caption, color = NewUiColors.onGroundDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

private fun windDirectionLabel(degrees: Double): String {
    val directions = listOf("Β", "ΒΑ", "Α", "ΝΑ", "Ν", "ΝΔ", "Δ", "ΒΔ")
    val index = (((degrees % 360) + 360) % 360 / 45.0).roundToInt() % 8
    return directions[index]
}

private const val DAGR_URL = "https://dagr.hasp.gov.gr/"

private fun openNewDirections(
    context: Context,
    origin: String,
    destination: String,
    provider: MapsProvider,
    travelResult: TravelTimeResult?,
) {
    val uri = if (provider == MapsProvider.OPENSTREETMAP) {
        if (
            travelResult is TravelTimeResult.Success &&
            travelResult.originLat != null && travelResult.originLng != null &&
            travelResult.destinationLat != null && travelResult.destinationLng != null
        ) {
            Uri.parse(
                "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car" +
                    "&route=${travelResult.originLat},${travelResult.originLng}" +
                    ";${travelResult.destinationLat},${travelResult.destinationLng}",
            )
        } else {
            Uri.parse("https://www.openstreetmap.org/search?query=${URLEncoder.encode(destination, "UTF-8")}")
        }
    } else {
        Uri.parse(
            "https://www.google.com/maps/dir/?api=1" +
                "&origin=${URLEncoder.encode(origin, "UTF-8")}" +
                "&destination=${URLEncoder.encode(destination, "UTF-8")}" +
                "&travelmode=driving",
        )
    }
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}
