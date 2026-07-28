package gr.gtar.jobclosure.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DirectionsResponse(
    val status: String,
    val routes: List<DirectionsRoute> = emptyList(),
    @Json(name = "error_message") val errorMessage: String? = null,
)

@JsonClass(generateAdapter = true)
data class DirectionsRoute(
    val legs: List<DirectionsLeg> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DirectionsLeg(
    val duration: DirectionsTextValue?,
    @Json(name = "duration_in_traffic") val durationInTraffic: DirectionsTextValue?,
    val distance: DirectionsTextValue?,
)

@JsonClass(generateAdapter = true)
data class DirectionsTextValue(
    val text: String,
    val value: Long,
)
