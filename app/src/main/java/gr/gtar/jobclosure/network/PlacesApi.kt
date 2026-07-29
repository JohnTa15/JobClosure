package gr.gtar.jobclosure.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/** Google Places Autocomplete - used only for address/venue-name suggestions when the Google
 *  Maps provider is selected (same API key as Directions/Geocoding, but needs "Places API"
 *  enabled too). */
interface PlacesApi {

    @GET("maps/api/place/autocomplete/json")
    suspend fun autocomplete(
        @Query("input") input: String,
        @Query("key") apiKey: String,
        @Query("components") components: String = "country:gr",
        @Query("language") language: String = "el",
    ): PlacesAutocompleteResponse
}

@JsonClass(generateAdapter = true)
data class PlacesAutocompleteResponse(
    val predictions: List<PlacePrediction> = emptyList(),
    val status: String = "",
)

@JsonClass(generateAdapter = true)
data class PlacePrediction(
    val description: String,
)
