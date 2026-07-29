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
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
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
import gr.gtar.jobclosure.network.TravelTimeResult
import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm", Locale("el", "GR"))

private sealed interface TravelDisplayState {
    data object Loading : TravelDisplayState
    data class Success(val text: String) : TravelDisplayState
    data class Error(val message: String) : TravelDisplayState
    data object Empty : TravelDisplayState
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
                onNavigate = { openDirections(context, state.settings.homeAddress, booking.churchAddress) },
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
                    onNavigate = {
                        openDirections(context, booking.churchAddress, booking.receptionVenueAddress)
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

            if (address.isNotBlank()) {
                OutlinedButton(onClick = onNavigate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Άνοιγμα διαδρομής στο Google Maps")
                }
            }
        }
    }
}

private fun openDirections(context: Context, origin: String, destination: String) {
    val uri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1" +
            "&origin=${URLEncoder.encode(origin, "UTF-8")}" +
            "&destination=${URLEncoder.encode(destination, "UTF-8")}" +
            "&travelmode=driving"
    )
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}
