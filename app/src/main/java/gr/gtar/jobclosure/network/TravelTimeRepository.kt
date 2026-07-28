package gr.gtar.jobclosure.network

sealed interface TravelTimeResult {
    data class Success(val durationText: String, val durationMinutes: Int) : TravelTimeResult
    data class Error(val message: String) : TravelTimeResult
}

class TravelTimeRepository(private val api: DirectionsApi) {

    suspend fun getTravelTime(origin: String, destination: String, apiKey: String): TravelTimeResult {
        if (apiKey.isBlank()) {
            return TravelTimeResult.Error("Δεν έχει οριστεί κλειδί Google Maps API (Ρυθμίσεις)")
        }
        if (origin.isBlank() || destination.isBlank()) {
            return TravelTimeResult.Error("Λείπει διεύθυνση αφετηρίας ή προορισμού")
        }
        return try {
            val response = api.getDirections(origin = origin, destination = destination, apiKey = apiKey)
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
}
