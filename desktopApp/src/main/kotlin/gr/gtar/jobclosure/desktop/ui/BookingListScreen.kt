package gr.gtar.jobclosure.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.shared.model.Booking
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingListScreen(
    state: AppUiState,
    onAddBooking: () -> Unit,
    onOpenBooking: (Booking) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Κρατήσεις") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ρυθμίσεις")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddBooking, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Νέα κράτηση") })
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.bookings.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.bookings.isEmpty() -> {
                    Text(
                        "Δεν υπάρχουν κρατήσεις τον τελευταίο μήνα και μετά.",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.bookings, key = { it.bookingId }) { booking ->
                            BookingRow(booking, onClick = { onOpenBooking(booking) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingRow(booking: Booking, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(booking.title, style = MaterialTheme.typography.titleMedium)
                if (booking.hasDrone) {
                    Icon(Icons.Filled.FlightTakeoff, contentDescription = "Drone", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(booking.type.displayName, style = MaterialTheme.typography.bodySmall)
            Text(
                booking.ceremonyStart.atZone(ZoneId.systemDefault()).format(displayFormatter),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (booking.hasReception) {
                Text("+ Δεξίωση: ${booking.receptionVenueName}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
