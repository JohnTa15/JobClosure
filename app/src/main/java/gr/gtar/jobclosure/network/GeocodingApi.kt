package gr.gtar.jobclosure.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {
    @GET("maps/api/geocode/json")
    suspend fun geocode(
        @Query("address") address: String,
        @Query("key") apiKey: String,
    ): GeocodingResponse
}

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val status: String,
    val results: List<GeocodingResult> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val geometry: GeocodingGeometry,
)

@JsonClass(generateAdapter = true)
data class GeocodingGeometry(
    val location: GeocodingLatLng,
)

@JsonClass(generateAdapter = true)
data class GeocodingLatLng(
    val lat: Double,
    val lng: Double,
)
