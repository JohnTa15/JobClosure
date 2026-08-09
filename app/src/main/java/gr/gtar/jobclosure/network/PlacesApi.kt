package gr.gtar.jobclosure.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Places API (New) - `places.googleapis.com`, POST + JSON bodies with the key in an `X-Goog-Api-Key`
 * header, rather than the legacy `maps.googleapis.com/maps/api/place/*` GET endpoints.
 *
 * The legacy API is what this used to call, but Google no longer enables it on new Cloud projects:
 * a project that only has "Places API (New)" switched on answers every legacy call with
 * REQUEST_DENIED, which silently emptied the address autocomplete and blocked venue photos.
 */
interface PlacesApi {

    @POST("v1/places:autocomplete")
    suspend fun autocomplete(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Body request: PlacesAutocompleteRequest,
    ): PlacesAutocompleteResponse

    /**
     * Text Search. [fieldMask] is mandatory on this API and doubles as the billing tier selector -
     * asking only for `places.photos` keeps it to what the venue-photo preview actually needs.
     */
    @POST("v1/places:searchText")
    suspend fun searchText(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body request: PlacesTextSearchRequest,
    ): PlacesTextSearchResponse

    companion object {
        const val BASE_URL = "https://places.googleapis.com/"
        const val PHOTO_FIELD_MASK = "places.photos"
    }
}

@JsonClass(generateAdapter = true)
data class PlacesAutocompleteRequest(
    val input: String,
    val includedRegionCodes: List<String> = listOf("gr"),
    val languageCode: String = "el",
)

@JsonClass(generateAdapter = true)
data class PlacesAutocompleteResponse(
    val suggestions: List<PlaceSuggestionEntry> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PlaceSuggestionEntry(
    val placePrediction: PlacePrediction? = null,
)

@JsonClass(generateAdapter = true)
data class PlacePrediction(
    val text: PlaceText? = null,
)

@JsonClass(generateAdapter = true)
data class PlaceText(
    val text: String = "",
)

@JsonClass(generateAdapter = true)
data class PlacesTextSearchRequest(
    val textQuery: String,
    val languageCode: String = "el",
    val maxResultCount: Int = 1,
)

@JsonClass(generateAdapter = true)
data class PlacesTextSearchResponse(
    val places: List<PlaceResult> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PlaceResult(
    val photos: List<PlacePhotoRef> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PlacePhotoRef(
    /** Resource name, e.g. `places/ChIJ.../photos/AeJb...` - the path the media endpoint takes. */
    @Json(name = "name") val name: String = "",
)
