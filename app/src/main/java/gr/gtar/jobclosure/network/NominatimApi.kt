package gr.gtar.jobclosure.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/**
 * Nominatim (nominatim.openstreetmap.org): OpenStreetMap's free geocoding service, no API key
 * needed. Its usage policy requires an identifying User-Agent on every request (a generic HTTP
 * client User-Agent isn't enough) and asks for at most ~1 request/second - both fine for this
 * app's occasional, single-user lookups. Also used for address/venue-name autocomplete
 * suggestions (limit > 1), biased to Greece since that's this app's only real userbase.
 */
interface NominatimApi {

    @Headers("User-Agent: JobClosure-Android-App (single-user booking tracker, no contact address)")
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Query("countrycodes") countryCodes: String = "gr",
    ): List<NominatimResult>

    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org/"
    }
}

/** lat/lon come back as strings from Nominatim's API - a known quirk of it, not a typo here. */
@JsonClass(generateAdapter = true)
data class NominatimResult(
    val lat: String,
    val lon: String,
    @Json(name = "display_name") val displayName: String = "",
)
