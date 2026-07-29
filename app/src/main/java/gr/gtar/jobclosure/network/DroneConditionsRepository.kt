package gr.gtar.jobclosure.network

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
    private val openMeteoApi: OpenMeteoApi,
) {
    suspend fun getConditions(address: String, mapsApiKey: String): DroneConditionsResult {
        if (address.isBlank()) return DroneConditionsResult.Error("Λείπει διεύθυνση")
        if (mapsApiKey.isBlank()) {
            return DroneConditionsResult.Error("Δεν έχει οριστεί κλειδί Google Maps API (Ρυθμίσεις)")
        }

        return try {
            val geocoding = geocodingApi.geocode(address, mapsApiKey)
            val location = geocoding.results.firstOrNull()?.geometry?.location
                ?: return DroneConditionsResult.Error("Δεν βρέθηκε τοποθεσία για τη διεύθυνση")

            val weather = openMeteoApi.getCurrentWeather(location.lat, location.lng).currentWeather
                ?: return DroneConditionsResult.Error("Δεν βρέθηκαν δεδομένα καιρού")
            val elevation = openMeteoApi.getElevation(location.lat, location.lng).elevation.firstOrNull()

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
