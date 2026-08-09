package gr.gtar.jobclosure.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.gtar.jobclosure.data.MapsProvider
import gr.gtar.jobclosure.network.NetworkModule
import gr.gtar.jobclosure.network.PlacesApi
import gr.gtar.jobclosure.network.PlacesTextSearchRequest
import gr.gtar.jobclosure.ui.theme.NewUiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.HttpException

private const val PHOTO_MAX_WIDTH = 400
private val DISPLAY_HEIGHT = 120.dp
private const val GOOGLE_PHOTO_URL = "https://places.googleapis.com/v1/"
private const val WIKIPEDIA_API_URL = "https://el.wikipedia.org/w/api.php"
private const val WIKIPEDIA_SEARCH_RADIUS_METRES = 700
private const val USER_AGENT = "JobClosure-Android-App (single-user booking tracker, no contact address)"

private sealed interface VenuePhotoState {
    data object Loading : VenuePhotoState
    data class Loaded(val bitmap: ImageBitmap) : VenuePhotoState

    /** Every source was asked and none had a photo of this place. */
    data object NotFound : VenuePhotoState

    /** The lookup itself was rejected (API not enabled for the key, quota, ...). Worth showing,
     *  since it's fixable and otherwise indistinguishable from "no photo exists". */
    data class Failed(val message: String) : VenuePhotoState
}

/**
 * A photo of a church/venue. With Google selected it asks Places (New) Text Search first, and falls
 * back to the lead image of a Wikipedia article near [coordinates] when Places has nothing - plenty
 * of churches are documented there but absent from Places' photography, and the fallback costs
 * nothing. On the OpenStreetMap provider only the Wikipedia source exists, since OSM/Nominatim serve
 * no photography at all. Fetched with plain OkHttp and decoded to a Bitmap, the same lightweight
 * approach as [MiniMapPreview] rather than pulling in an image-loading library.
 *
 * The box is always drawn once there's something to look up, saying so when no photo was found -
 * silently rendering nothing made a photo-less venue look identical to a broken feature.
 */
@Composable
fun VenuePhotoPreview(
    label: String,
    query: String,
    provider: MapsProvider,
    googleApiKey: String,
    coordinates: Pair<Double, Double>?,
    modifier: Modifier = Modifier,
) {
    val useGoogle = provider == MapsProvider.GOOGLE && googleApiKey.isNotBlank()
    val canAskGoogle = useGoogle && query.isNotBlank()
    if (!canAskGoogle && coordinates == null) return

    var state by remember(query, useGoogle, coordinates) { mutableStateOf<VenuePhotoState>(VenuePhotoState.Loading) }

    LaunchedEffect(query, useGoogle, googleApiKey, coordinates) {
        state = VenuePhotoState.Loading
        state = withContext(Dispatchers.IO) {
            val fromGoogle = if (canAskGoogle) loadGooglePlacePhoto(query, googleApiKey) else null
            when {
                fromGoogle is VenuePhotoState.Loaded -> fromGoogle
                // A rejected call is the user's to fix, so it wins over any fallback result.
                fromGoogle is VenuePhotoState.Failed -> fromGoogle
                coordinates != null -> loadWikipediaPhoto(coordinates.first, coordinates.second)
                else -> VenuePhotoState.NotFound
            }
        }
    }

    val current = state

    Column(modifier = modifier) {
        NewSectionLabel(text = label, modifier = Modifier.padding(bottom = 7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DISPLAY_HEIGHT)
                .clip(RoundedCornerShape(12.dp))
                .background(NewUiColors.outlineSoft.copy(alpha = 0.3f)),
        ) {
            when (current) {
                is VenuePhotoState.Loaded -> Image(
                    bitmap = current.bitmap,
                    contentDescription = label,
                    modifier = Modifier.fillMaxWidth().height(DISPLAY_HEIGHT),
                    contentScale = ContentScale.Crop,
                )
                is VenuePhotoState.Failed -> Box(
                    Modifier.fillMaxWidth().height(DISPLAY_HEIGHT).padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        current.message,
                        color = NewUiColors.onGroundFaint,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                is VenuePhotoState.NotFound -> Box(
                    Modifier.fillMaxWidth().height(DISPLAY_HEIGHT).padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Δεν βρέθηκε φωτογραφία για αυτό το μέρος.",
                        color = NewUiColors.onGroundFaint,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> Box(Modifier.fillMaxWidth().height(DISPLAY_HEIGHT), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

/**
 * Text Search on Places API (New) for the venue, then its first photo through the media endpoint.
 * A search that simply matches nothing (or a venue with no photography) is [VenuePhotoState.NotFound];
 * a rejected call carries Google's own error body, since that's what says which API or billing
 * setting the key is missing.
 */
private suspend fun loadGooglePlacePhoto(query: String, apiKey: String): VenuePhotoState {
    val response = try {
        NetworkModule.placesApi.searchText(
            apiKey = apiKey,
            fieldMask = PlacesApi.PHOTO_FIELD_MASK,
            request = PlacesTextSearchRequest(textQuery = query),
        )
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string()?.trim()?.takeIf { it.isNotBlank() }
        return VenuePhotoState.Failed(body ?: "Google Places: HTTP ${e.code()}")
    } catch (e: Exception) {
        return VenuePhotoState.Failed(e.message ?: "Αποτυχία σύνδεσης στο Google Places.")
    }

    val photoName = response.places.firstOrNull()?.photos?.firstOrNull()?.name
        ?.takeIf { it.isNotBlank() }
        ?: return VenuePhotoState.NotFound

    val url = "$GOOGLE_PHOTO_URL$photoName/media".toHttpUrl().newBuilder()
        .addQueryParameter("maxWidthPx", PHOTO_MAX_WIDTH.toString())
        .addQueryParameter("key", apiKey)
        .build()
    return downloadBitmap(Request.Builder().url(url).build())
}

/** Wikipedia's geosearch generator returns articles near a point; `pageimages` then gives each
 *  one's lead thumbnail. Takes the first article that actually has an image. Nothing nearby is the
 *  norm for small private venues, so any miss here is [VenuePhotoState.NotFound], not an error. */
private fun loadWikipediaPhoto(latitude: Double, longitude: Double): VenuePhotoState {
    val url = WIKIPEDIA_API_URL.toHttpUrl().newBuilder()
        .addQueryParameter("action", "query")
        .addQueryParameter("format", "json")
        .addQueryParameter("formatversion", "2")
        .addQueryParameter("generator", "geosearch")
        .addQueryParameter("ggscoord", "$latitude|$longitude")
        .addQueryParameter("ggsradius", WIKIPEDIA_SEARCH_RADIUS_METRES.toString())
        .addQueryParameter("ggslimit", "5")
        .addQueryParameter("prop", "pageimages")
        .addQueryParameter("piprop", "thumbnail")
        .addQueryParameter("pithumbsize", PHOTO_MAX_WIDTH.toString())
        .build()

    val thumbnailUrl = try {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) return VenuePhotoState.NotFound
            val body = response.body?.string() ?: return VenuePhotoState.NotFound
            val pages = JSONObject(body).optJSONObject("query")?.optJSONArray("pages")
                ?: return VenuePhotoState.NotFound
            (0 until pages.length())
                .asSequence()
                .mapNotNull { index -> pages.optJSONObject(index)?.optJSONObject("thumbnail")?.optString("source") }
                .firstOrNull { it.isNotBlank() }
        }
    } catch (e: Exception) {
        null
    } ?: return VenuePhotoState.NotFound

    return downloadBitmap(Request.Builder().url(thumbnailUrl).header("User-Agent", USER_AGENT).build())
}

private fun downloadBitmap(request: Request): VenuePhotoState = try {
    OkHttpClient().newCall(request).execute().use { response ->
        val bytes = response.body?.bytes()
        when {
            !response.isSuccessful -> VenuePhotoState.Failed(
                bytes?.toString(Charsets.UTF_8)?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Η λήψη της φωτογραφίας απέτυχε (HTTP ${response.code}).",
            )
            bytes == null -> VenuePhotoState.NotFound
            else -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?.let { VenuePhotoState.Loaded(it.asImageBitmap()) }
                ?: VenuePhotoState.NotFound
        }
    }
} catch (e: Exception) {
    VenuePhotoState.Failed(e.message ?: "Αποτυχία λήψης φωτογραφίας.")
}
