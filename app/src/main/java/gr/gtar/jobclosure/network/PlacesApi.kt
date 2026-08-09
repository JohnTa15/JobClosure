package gr.gtar.jobclosure.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/** Google Places Autocomplete/Find Place - used only when the Google Maps provider is selected
 *  (same API key as Directions/Geocoding, but needs "Places API" enabled too). */
interface PlacesApi {

    @GET("maps/api/place/autocomplete/json")
    suspend fun autocomplete(
        @Query("input") input: String,
        @Query("key") apiKey: String,
        @Query("components") components: String = "country:gr",
        @Query("language") language: String = "el",
    ): PlacesAutocompleteResponse

    /** Looks up a venue by its name/address and asks only for its photos - used by
     *  [gr.gtar.jobclosure.ui.components.VenuePhotoPreview] to show a real photo of the
     *  church/reception venue on the detail screen. */
    @GET("maps/api/place/findplacefromtext/json")
    suspend fun findPlace(
        @Query("input") input: String,
        @Query("key") apiKey: String,
        @Query("inputtype") inputType: String = "textquery",
        @Query("fields") fields: String = "photos",
    ): PlaceFindResponse
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

@JsonClass(generateAdapter = true)
data class PlaceFindResponse(
    val candidates: List<PlaceCandidate> = emptyList(),
    val status: String = "",
)

@JsonClass(generateAdapter = true)
data class PlaceCandidate(
    val photos: List<PlacePhotoRef> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PlacePhotoRef(
    @Json(name = "photo_reference") val photoReference: String,
)
