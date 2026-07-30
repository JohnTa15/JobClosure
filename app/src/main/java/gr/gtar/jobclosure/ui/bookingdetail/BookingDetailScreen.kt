package gr.gtar.jobclosure.ui.bookingdetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.data.Booking
import gr.gtar.jobclosure.data.MapsProvider
import gr.gtar.jobclosure.network.DroneConditionsResult
import gr.gtar.jobclosure.network.TravelTimeResult
import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm", Locale("el", "GR"))
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("el", "GR"))

/** Drone Aware - GR (DAGR): HCAA/HASP's official pre-flight airspace check for Greece. No public
 *  API is published for it, so this links out to the real site instead of guessing at one. */
private const val DAGR_URL = "https://dagr.hasp.gov.gr/"

private sealed interface TravelDisplayState {
    data object Loading : TravelDisplayState
    data class Success(val text: String) : TravelDisplayState
    data class Error(val message: String) : TravelDisplayState
    data object Empty : TravelDisplayState
}

private sealed interface DroneDisplayState {
    data object Loading : DroneDisplayState
    data class Success(val conditions: DroneConditionsResult.Success) : DroneDisplayState
    data class Error(val message: String) : DroneDisplayState
    data object Empty : DroneDisplayState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    viewModel: BookingDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.booking?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Πίσω")
                    }
                },
                actions = {
                    state.booking?.let { booking ->
                        IconButton(onClick = { onEdit(booking.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Επεξεργασία")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val booking = state.booking
        if (state.isLoading || booking == null) {
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!booking.isConfirmed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Filled.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Χωρίς επιβεβαίωση ακόμα",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(booking.type.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                booking.ceremonyStart.format(dateTimeFormatter)
                    .replaceFirstChar { it.titlecase(Locale("el", "GR")) },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (booking.hasDrone) {
                Text("Θα χρησιμοποιηθεί drone", style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider()

            LocationCard(
                title = "Εκκλησία",
                name = booking.churchName,
                address = booking.churchAddress,
                travelLabel = "Από το σπίτι",
                travelResult = state.homeToChurch,
                isLoadingTravelTime = state.isLoadingTravelTimes,
                showDroneConditions = booking.hasDrone,
                droneConditions = state.churchDroneConditions,
                droneForecastDate = booking.ceremonyStart.toLocalDate(),
                isLoadingDroneConditions = state.isLoadingDroneConditions,
                onNavigate = {
                    openDirections(
                        context, state.settings.homeAddress, booking.churchAddress,
                        state.settings.mapsProvider, state.homeToChurch,
                    )
                },
            )

            if (booking.hasReception) {
                LocationCard(
                    title = "Δεξίωση",
                    name = booking.receptionVenueName,
                    address = booking.receptionVenueAddress,
                    subtitle = booking.receptionStart?.format(dateTimeFormatter)
                        ?.replaceFirstChar { it.titlecase(Locale("el", "GR")) },
                    travelLabel = "Από την εκκλησία",
                    travelResult = state.churchToReception,
                    isLoadingTravelTime = state.isLoadingTravelTimes,
                    showDroneConditions = booking.hasDrone,
                    droneConditions = state.receptionDroneConditions,
                    droneForecastDate = (booking.receptionStart ?: booking.ceremonyStart).toLocalDate(),
                    isLoadingDroneConditions = state.isLoadingDroneConditions,
                    onNavigate = {
                        openDirections(
                            context, booking.churchAddress, booking.receptionVenueAddress,
                            state.settings.mapsProvider, state.churchToReception,
                        )
                    },
                )
            }

            if (booking.notes.isNotBlank()) {
                HorizontalDivider()
                Text("Σημειώσεις", style = MaterialTheme.typography.titleMedium)
                Text(booking.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LocationCard(
    title: String,
    name: String,
    address: String,
    subtitle: String? = null,
    travelLabel: String,
    travelResult: TravelTimeResult?,
    isLoadingTravelTime: Boolean,
    showDroneConditions: Boolean = false,
    droneConditions: DroneConditionsResult? = null,
    droneForecastDate: java.time.LocalDate? = null,
    isLoadingDroneConditions: Boolean = false,
    onNavigate: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (name.isNotBlank()) Text(name, style = MaterialTheme.typography.bodyLarge)
            if (address.isNotBlank()) {
                Text(address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }

            val travelDisplay: TravelDisplayState = when {
                isLoadingTravelTime -> TravelDisplayState.Loading
                travelResult is TravelTimeResult.Success ->
                    TravelDisplayState.Success("$travelLabel: ${travelResult.durationText}")
                travelResult is TravelTimeResult.Error -> TravelDisplayState.Error(travelResult.message)
                else -> TravelDisplayState.Empty
            }
            AnimatedContent(
                targetState = travelDisplay,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "travel-time",
                modifier = Modifier.padding(top = 4.dp),
            ) { display ->
                when (display) {
                    is TravelDisplayState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).size(16.dp))
                    }
                    is TravelDisplayState.Success ->
                        Text(display.text, style = MaterialTheme.typography.bodyMedium)
                    is TravelDisplayState.Error ->
                        Text(display.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TravelDisplayState.Empty -> Row {}
                }
            }

            if (showDroneConditions) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    if (droneForecastDate != null) {
                        "Συνθήκες για Drone - πρόβλεψη για ${droneForecastDate.format(shortDateFormatter)}"
                    } else {
                        "Συνθήκες για Drone"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )

                val droneDisplay: DroneDisplayState = when {
                    isLoadingDroneConditions -> DroneDisplayState.Loading
                    droneConditions is DroneConditionsResult.Success -> DroneDisplayState.Success(droneConditions)
                    droneConditions is DroneConditionsResult.Error -> DroneDisplayState.Error(droneConditions.message)
                    else -> DroneDisplayState.Empty
                }
                AnimatedContent(
                    targetState = droneDisplay,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                    label = "drone-conditions",
                ) { display ->
                    when (display) {
                        is DroneDisplayState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).size(16.dp))
                        }
                        is DroneDisplayState.Success -> {
                            val c = display.conditions
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                DroneConditionRow(
                                    Icons.Filled.Thermostat,
                                    "${c.temperatureC.roundToInt()}°C, ${c.weatherDescription}",
                                )
                                DroneConditionRow(
                                    Icons.Filled.Air,
                                    "Άνεμος: ${c.windSpeedKmh.roundToInt()} km/h (${windDirectionLabel(c.windDirectionDeg)})",
                                )
                                DroneConditionRow(
                                    Icons.Filled.Terrain,
                                    "Υψόμετρο περιοχής: ${c.elevationMeters.roundToInt()} μ.",
                                )
                            }
                        }
                        is DroneDisplayState.Error ->
                            Text(display.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        DroneDisplayState.Empty -> Row {}
                    }
                }

                val droneAwareContext = LocalContext.current
                OutlinedButton(
                    onClick = {
                        droneAwareContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DAGR_URL)))
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    Icon(Icons.Filled.GppGood, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Επίσημος έλεγχος στο Drone Aware - GR (ΥΠΑ/HASP)")
                }
                Text(
                    "Πάντα έλεγχε εκεί πριν πετάξεις, ειδικά κοντά σε αεροδρόμια ή στρατιωτικές περιοχές.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (address.isNotBlank()) {
                OutlinedButton(onClick = onNavigate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Άνοιγμα διαδρομής στο Google Maps")
                }
            }
        }
    }
}

@Composable
private fun DroneConditionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Rough 8-point compass label from a wind direction in degrees. */
private fun windDirectionLabel(degrees: Double): String {
    val directions = listOf("Β", "ΒΑ", "Α", "ΝΑ", "Ν", "ΝΔ", "Δ", "ΒΔ")
    val index = (((degrees % 360) + 360) % 360 / 45.0).roundToInt() % 8
    return directions[index]
}

private fun openDirections(
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
            // Coordinates already geocoded (via Nominatim) while computing the travel time -
            // reuse them instead of re-geocoding just for the link.
            Uri.parse(
                "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car" +
                    "&route=${travelResult.originLat},${travelResult.originLng}" +
                    ";${travelResult.destinationLat},${travelResult.destinationLng}"
            )
        } else {
            // No coordinates on hand yet (travel time still loading or failed) - fall back to a
            // plain address search rather than blocking the button on a fresh geocode call.
            Uri.parse("https://www.openstreetmap.org/search?query=${URLEncoder.encode(destination, "UTF-8")}")
        }
    } else {
        Uri.parse(
            "https://www.google.com/maps/dir/?api=1" +
                "&origin=${URLEncoder.encode(origin, "UTF-8")}" +
                "&destination=${URLEncoder.encode(destination, "UTF-8")}" +
                "&travelmode=driving"
        )
    }
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}
