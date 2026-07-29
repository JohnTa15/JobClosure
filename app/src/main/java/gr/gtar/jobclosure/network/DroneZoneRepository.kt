package gr.gtar.jobclosure.network

/**
 * Best-effort informational check only - NOT an authoritative go/no-go for flying. It reports
 * aviation airspaces registered in OpenAIP's community database within [RADIUS_METERS] of the
 * address (restricted/prohibited/danger areas, control zones, etc). Always verify against an
 * official source (national aviation authority, an app like Dronerules.eu or a NOTAM briefing)
 * before actually flying, especially near airports or military sites.
 */
sealed interface DroneZoneResult {
    data class Success(val nearbyAirspaceNames: List<String>) : DroneZoneResult
    data class Error(val message: String) : DroneZoneResult
}

class DroneZoneRepository(
    private val geocodingApi: GeocodingApi,
    private val openAipApi: OpenAipApi,
) {
    suspend fun getNearbyAirspaces(address: String, mapsApiKey: String, openAipApiKey: String): DroneZoneResult {
        if (address.isBlank()) return DroneZoneResult.Error("Λείπει διεύθυνση")
        if (mapsApiKey.isBlank()) {
            return DroneZoneResult.Error("Δεν έχει οριστεί κλειδί Google Maps API (Ρυθμίσεις)")
        }
        if (openAipApiKey.isBlank()) {
            return DroneZoneResult.Error("Δεν έχει οριστεί κλειδί OpenAIP API (Ρυθμίσεις)")
        }

        return try {
            val geocoding = geocodingApi.geocode(address, mapsApiKey)
            val location = geocoding.results.firstOrNull()?.geometry?.location
                ?: return DroneZoneResult.Error("Δεν βρέθηκε τοποθεσία για τη διεύθυνση")

            val response = openAipApi.getNearbyAirspaces(
                apiKey = openAipApiKey,
                position = "${location.lat},${location.lng}",
                distanceMeters = RADIUS_METERS,
            )
            DroneZoneResult.Success(response.items.map { it.name })
        } catch (e: Exception) {
            DroneZoneResult.Error("Αποτυχία σύνδεσης: ${e.message ?: "άγνωστο σφάλμα"}")
        }
    }

    private companion object {
        const val RADIUS_METERS = 5000
    }
}
