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
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.data.MapsProvider
import gr.gtar.jobclosure.network.NetworkModule
import gr.gtar.jobclosure.ui.theme.NewUiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val PHOTO_MAX_WIDTH = 400
private val DISPLAY_HEIGHT = 120.dp
private const val GOOGLE_PHOTO_URL = "https://maps.googleapis.com/maps/api/place/photo"
private const val WIKIPEDIA_API_URL = "https://el.wikipedia.org/w/api.php"
private const val WIKIPEDIA_SEARCH_RADIUS_METRES = 700
private const val USER_AGENT = "JobClosure-Android-App (single-user booking tracker, no contact address)"

private sealed interface VenuePhotoState {
    data object Loading : VenuePhotoState
    data class Loaded(val bitmap: ImageBitmap) : VenuePhotoState
    data object NotFound : VenuePhotoState
}

/**
 * A photo of a church/venue, sourced to match the configured maps provider: Google Places
 * (Find Place From Text -> photo_reference -> Place Photo) when Google is selected, otherwise a
 * nearby Wikipedia article's lead image found by [coordinates] - the closest free equivalent, since
 * OSM/Nominatim itself serves no photography. Fetched with plain OkHttp and decoded to a Bitmap,
 * the same lightweight approach as [MiniMapPreview] rather than pulling in an image-loading library.
 *
 * Renders nothing at all when no photo is found (very common for small private venues on the
 * Wikipedia path) rather than showing an empty or broken placeholder.
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
    if (useGoogle && query.isBlank()) return
    if (!useGoogle && coordinates == null) return

    var state by remember(query, useGoogle, coordinates) { mutableStateOf<VenuePhotoState>(VenuePhotoState.Loading) }

    LaunchedEffect(query, useGoogle, googleApiKey, coordinates) {
        state = VenuePhotoState.Loading
        val bitmap = withContext(Dispatchers.IO) {
            if (useGoogle) {
                loadGooglePlacePhoto(query, googleApiKey)
            } else {
                coordinates?.let { (lat, lon) -> loadWikipediaPhoto(lat, lon) }
            }
        }
        state = if (bitmap != null) VenuePhotoState.Loaded(bitmap) else VenuePhotoState.NotFound
    }

    val current = state
    if (current is VenuePhotoState.NotFound) return

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
                else -> Box(Modifier.fillMaxWidth().height(DISPLAY_HEIGHT), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

private suspend fun loadGooglePlacePhoto(query: String, apiKey: String): ImageBitmap? {
    val photoReference = try {
        NetworkModule.placesApi.findPlace(input = query, apiKey = apiKey)
            .candidates.firstOrNull()?.photos?.firstOrNull()?.photoReference
    } catch (e: Exception) {
        null
    } ?: return null

    val url = GOOGLE_PHOTO_URL.toHttpUrl().newBuilder()
        .addQueryParameter("maxwidth", PHOTO_MAX_WIDTH.toString())
        .addQueryParameter("photoreference", photoReference)
        .addQueryParameter("key", apiKey)
        .build()
    return downloadBitmap(Request.Builder().url(url).build())
}

/** Wikipedia's geosearch generator returns articles near a point; `pageimages` then gives each
 *  one's lead thumbnail. Takes the first article that actually has an image. */
private fun loadWikipediaPhoto(latitude: Double, longitude: Double): ImageBitmap? {
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
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val pages = JSONObject(body).optJSONObject("query")?.optJSONArray("pages") ?: return null
            (0 until pages.length())
                .asSequence()
                .mapNotNull { index -> pages.optJSONObject(index)?.optJSONObject("thumbnail")?.optString("source") }
                .firstOrNull { it.isNotBlank() }
        }
    } catch (e: Exception) {
        null
    } ?: return null

    return downloadBitmap(Request.Builder().url(thumbnailUrl).header("User-Agent", USER_AGENT).build())
}

private fun downloadBitmap(request: Request): ImageBitmap? = try {
    OkHttpClient().newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            null
        } else {
            response.body?.bytes()?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }
    }
} catch (e: Exception) {
    null
}
