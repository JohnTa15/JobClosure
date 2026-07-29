package gr.gtar.jobclosure.network

import gr.gtar.jobclosure.data.MapsProvider

sealed interface DroneConditionsResult {
    data class Success(
        val temperatureC: Double,
        val windSpeedKmh: Double,
        val windDirectionDeg: Double,
        val weatherDescription: String,
        val elevationMeters: Double,
    ) : DroneConditionsResult

    data class Error(val message: String) : DroneConditionsResult
}

class DroneConditionsRepository(
    private val geocodingApi: GeocodingApi,
    private val nominatimApi: NominatimApi,
    private val openMeteoApi: OpenMeteoApi,
) {
    suspend fun getConditions(address: String, provider: MapsProvider, googleApiKey: String): DroneConditionsResult {
        if (address.isBlank()) return DroneConditionsResult.Error("Λείπει διεύθυνση")
        if (provider == MapsProvider.GOOGLE && googleApiKey.isBlank()) {
            return DroneConditionsResult.Error("Δεν έχει οριστεί κλειδί Google Maps API (Ρυθμίσεις)")
        }

        return try {
            val location = geocode(address, provider, googleApiKey)
                ?: return DroneConditionsResult.Error("Δεν βρέθηκε τοποθεσία για τη διεύθυνση")

            val weather = openMeteoApi.getCurrentWeather(location.first, location.second).currentWeather
                ?: return DroneConditionsResult.Error("Δεν βρέθηκαν δεδομένα καιρού")
            val elevation = openMeteoApi.getElevation(location.first, location.second).elevation.firstOrNull()

            DroneConditionsResult.Success(
                temperatureC = weather.temperature,
                windSpeedKmh = weather.windSpeed,
                windDirectionDeg = weather.windDirection,
                weatherDescription = weatherCodeToGreek(weather.weatherCode),
                elevationMeters = elevation ?: 0.0,
            )
        } catch (e: Exception) {
            DroneConditionsResult.Error("Αποτυχία σύνδεσης: ${e.message ?: "άγνωστο σφάλμα"}")
        }
    }

    /** Returns (lat, lon), or null if the chosen provider found nothing. */
    private suspend fun geocode(address: String, provider: MapsProvider, googleApiKey: String): Pair<Double, Double>? =
        when (provider) {
            MapsProvider.GOOGLE ->
                geocodingApi.geocode(address, googleApiKey).results.firstOrNull()?.geometry?.location
                    ?.let { it.lat to it.lng }
            MapsProvider.OPENSTREETMAP ->
                nominatimApi.search(address).firstOrNull()?.let { result ->
                    val lat = result.lat.toDoubleOrNull()
                    val lon = result.lon.toDoubleOrNull()
                    if (lat != null && lon != null) lat to lon else null
                }
        }

    private fun weatherCodeToGreek(code: Int): String = when (code) {
        0 -> "Καθαρός ουρανός"
        1 -> "Σχεδόν αίθριος"
        2 -> "Μερική νέφωση"
        3 -> "Συννεφιά"
        45, 48 -> "Ομίχλη"
        51, 53, 55 -> "Ψιλόβροχο"
        56, 57 -> "Παγωμένο ψιλόβροχο"
        61, 63, 65 -> "Βροχή"
        66, 67 -> "Παγωμένη βροχή"
        71, 73, 75 -> "Χιονόπτωση"
        77 -> "Κόκκοι χιονιού"
        80, 81, 82 -> "Μπόρες βροχής"
        85, 86 -> "Μπόρες χιονιού"
        95 -> "Καταιγίδα"
        96, 99 -> "Καταιγίδα με χαλάζι"
        else -> "Άγνωστες συνθήκες"
    }
}
