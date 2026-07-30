package gr.gtar.jobclosure.network

import gr.gtar.jobclosure.data.MapsProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
    /**
     * [eventDate] is the booking's own date - conditions are fetched as a forecast *for that day*,
     * not "right now", so changing a booking's date actually changes what's shown here. Open-Meteo
     * only forecasts reliably within [FORECAST_HORIZON_DAYS] days ahead, so dates further out (the
     * common case for weddings booked months in advance) get an explanatory message instead of a
     * misleading "today's weather" stand-in.
     */
    suspend fun getConditions(
        address: String,
        provider: MapsProvider,
        googleApiKey: String,
        eventDate: LocalDate,
    ): DroneConditionsResult {
        if (address.isBlank()) return DroneConditionsResult.Error("Λείπει διεύθυνση")
        if (provider == MapsProvider.GOOGLE && googleApiKey.isBlank()) {
            return DroneConditionsResult.Error("Δεν έχει οριστεί κλειδί Google Maps API (Ρυθμίσεις)")
        }

        val daysUntilEvent = ChronoUnit.DAYS.between(LocalDate.now(), eventDate)
        if (daysUntilEvent > FORECAST_HORIZON_DAYS) {
            return DroneConditionsResult.Error(
                "Η πρόβλεψη καιρού είναι διαθέσιμη μόνο έως $FORECAST_HORIZON_DAYS ημέρες πριν την " +
                    "εκδήλωση - ξαναδοκίμασε πιο κοντά στην ημερομηνία.",
            )
        }
        if (daysUntilEvent < -PAST_HORIZON_DAYS) {
            return DroneConditionsResult.Error("Η δουλειά έχει ήδη περάσει - δεν υπάρχει πρόβλεψη καιρού.")
        }

        return try {
            val location = geocode(address, provider, googleApiKey)
                ?: return DroneConditionsResult.Error("Δεν βρέθηκε τοποθεσία για τη διεύθυνση")

            val dateText = eventDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val daily = openMeteoApi.getDailyForecast(
                latitude = location.first,
                longitude = location.second,
                startDate = dateText,
                endDate = dateText,
            ).daily
            val index = daily?.time?.indexOf(dateText) ?: -1
            val tempMax = daily?.temperatureMax?.getOrNull(index)
            val tempMin = daily?.temperatureMin?.getOrNull(index)
            val wind = daily?.windSpeedMax?.getOrNull(index)
            val code = daily?.weatherCode?.getOrNull(index)
            if (tempMax == null || tempMin == null || wind == null || code == null) {
                return DroneConditionsResult.Error("Δεν βρέθηκαν δεδομένα καιρού για αυτή την ημερομηνία")
            }
            val elevation = openMeteoApi.getElevation(location.first, location.second).elevation.firstOrNull()

            DroneConditionsResult.Success(
                temperatureC = (tempMax + tempMin) / 2.0,
                windSpeedKmh = wind,
                windDirectionDeg = daily.windDirectionDominant.getOrElse(index) { 0.0 },
                weatherDescription = weatherCodeToGreek(code),
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

    companion object {
        private const val FORECAST_HORIZON_DAYS = 15L
        private const val PAST_HORIZON_DAYS = 5L
    }
}
