package gr.gtar.jobclosure.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * OpenAIP (openaip.net): community aviation-data API listing registered airspaces
 * (restricted/prohibited/danger areas, control zones, etc). Free tier, requires an API key.
 */
interface OpenAipApi {

    @GET("api/airspaces")
    suspend fun getNearbyAirspaces(
        @Header("x-openaip-api-key") apiKey: String,
        @Query("pos") position: String,
        @Query("dist") distanceMeters: Int,
        @Query("limit") limit: Int = 20,
    ): OpenAipAirspaceResponse

    companion object {
        const val BASE_URL = "https://api.core.openaip.net/"
    }
}

@JsonClass(generateAdapter = true)
data class OpenAipAirspaceResponse(
    val items: List<OpenAipAirspace> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class OpenAipAirspace(
    val name: String,
    val country: String? = null,
)
