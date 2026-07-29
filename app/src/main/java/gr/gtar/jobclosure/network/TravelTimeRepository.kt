package gr.gtar.jobclosure.network

import gr.gtar.jobclosure.data.MapsProvider

sealed interface TravelTimeResult {
    /** [originLat]/[originLng]/[destinationLat]/[destinationLng] are only populated on the
     *  OpenStreetMap path (geocoded via Nominatim before routing) - used to build a coordinate-based
     *  "open directions" link instead of relying on Google Maps. */
    data class Success(
        val durationText: String,
        val durationMinutes: Int,
        val originLat: Double? = null,
        val originLng: Double? = null,
        val destinationLat: Double? = null,
        val destinationLng: Double? = null,
    ) : TravelTimeResult

    data class Error(val message: String) : TravelTimeResult
}

class TravelTimeRepository(
    private val directionsApi: DirectionsApi,
    private val nominatimApi: NominatimApi,
    private val osrmApi: OsrmApi,
) {

    suspend fun getTravelTime(
        origin: String,
        destination: String,
        provider: MapsProvider,
        googleApiKey: String,
    ): TravelTimeResult {
        if (origin.isBlank() || destination.isBlank()) {
            return TravelTimeResult.Error("Λείπει διεύθυνση αφετηρίας ή προορισμού")
        }
        return when (provider) {
            MapsProvider.GOOGLE -> getGoogleTravelTime(origin, destination, googleApiKey)
            MapsProvider.OPENSTREETMAP -> getOsmTravelTime(origin, destination)
        }
    }

    private suspend fun getGoogleTravelTime(origin: String, destination: String, apiKey: String): TravelTimeResult {
        if (apiKey.isBlank()) {
            return TravelTimeResult.Error("Δεν έχει οριστεί κλειδί Google Maps API (Ρυθμίσεις)")
        }
        return try {
            val response = directionsApi.getDirections(origin = origin, destination = destination, apiKey = apiKey)
            when (response.status) {
                "OK" -> {
                    val duration = response.routes.firstOrNull()?.legs?.firstOrNull()?.duration
                    if (duration != null) {
                        TravelTimeResult.Success(duration.text, (duration.value / 60).toInt())
                    } else {
                        TravelTimeResult.Error("Δεν βρέθηκε διαδρομή")
                    }
                }
                "ZERO_RESULTS" -> TravelTimeResult.Error("Δεν βρέθηκε διαδρομή μεταξύ των διευθύνσεων")
                "REQUEST_DENIED" -> TravelTimeResult.Error(
                    "Το κλειδί Google Maps API απορρίφθηκε${response.errorMessage?.let { ": $it" } ?: ""}"
                )
                else -> TravelTimeResult.Error("Σφάλμα υπηρεσίας: ${response.status}")
            }
        } catch (e: Exception) {
            TravelTimeResult.Error("Αποτυχία σύνδεσης: ${e.message ?: "άγνωστο σφάλμα"}")
        }
    }

    private suspend fun getOsmTravelTime(origin: String, destination: String): TravelTimeResult {
        return try {
            val originLocation = geocode(origin)
                ?: return TravelTimeResult.Error("Δεν βρέθηκε τοποθεσία για: $origin")
            val destinationLocation = geocode(destination)
                ?: return TravelTimeResult.Error("Δεν βρέθηκε τοποθεσία για: $destination")

            val coordinates = "${originLocation.second},${originLocation.first}" +
                ";${destinationLocation.second},${destinationLocation.first}"
            val response = osrmApi.getRoute(coordinates)
            val route = response.routes.firstOrNull()
            if (response.code != "Ok" || route == null) {
                return TravelTimeResult.Error("Δεν βρέθηκε διαδρομή (OSRM)")
            }

            val minutes = (route.duration / 60).toInt()
            TravelTimeResult.Success(
                durationText = formatDuration(minutes),
                durationMinutes = minutes,
                originLat = originLocation.first,
                originLng = originLocation.second,
                destinationLat = destinationLocation.first,
                destinationLng = destinationLocation.second,
            )
        } catch (e: Exception) {
            TravelTimeResult.Error("Αποτυχία σύνδεσης: ${e.message ?: "άγνωστο σφάλμα"}")
        }
    }

    /** Returns (lat, lon), or null if Nominatim found nothing. */
    private suspend fun geocode(address: String): Pair<Double, Double>? {
        val result = nominatimApi.search(address).firstOrNull() ?: return null
        val lat = result.lat.toDoubleOrNull() ?: return null
        val lon = result.lon.toDoubleOrNull() ?: return null
        return lat to lon
    }

    private fun formatDuration(minutes: Int): String =
        if (minutes < 60) "$minutes λεπτά" else "${minutes / 60} ώ ${minutes % 60} λ"
}
