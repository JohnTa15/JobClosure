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
import gr.gtar.jobclosure.network.NetworkModule
import gr.gtar.jobclosure.ui.theme.NewUiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

private const val PHOTO_MAX_WIDTH = 400
private val DISPLAY_HEIGHT = 120.dp
private const val PHOTO_URL = "https://maps.googleapis.com/maps/api/place/photo"

private sealed interface VenuePhotoState {
    data object Loading : VenuePhotoState
    data class Loaded(val bitmap: ImageBitmap) : VenuePhotoState
    data object NotFound : VenuePhotoState
}

/**
 * Real photo of a church/venue via Google Places (Find Place From Text -> photo_reference ->
 * Place Photo), fetched with plain OkHttp and decoded to a Bitmap - same lightweight approach as
 * [MiniMapPreview] instead of pulling in an image-loading library for this one use case. Renders
 * nothing at all when there's no photo (or no Google API key configured) rather than showing an
 * empty/broken placeholder.
 */
@Composable
fun VenuePhotoPreview(label: String, query: String, apiKey: String, modifier: Modifier = Modifier) {
    if (apiKey.isBlank() || query.isBlank()) return

    var state by remember { mutableStateOf<VenuePhotoState>(VenuePhotoState.Loading) }

    LaunchedEffect(query, apiKey) {
        state = VenuePhotoState.Loading
        val bitmap = withContext(Dispatchers.IO) { loadVenuePhoto(query, apiKey) }
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

private suspend fun loadVenuePhoto(query: String, apiKey: String): ImageBitmap? {
    val photoReference = try {
        NetworkModule.placesApi.findPlace(input = query, apiKey = apiKey)
            .candidates.firstOrNull()?.photos?.firstOrNull()?.photoReference
    } catch (e: Exception) {
        null
    } ?: return null

    return try {
        val url = PHOTO_URL.toHttpUrl().newBuilder()
            .addQueryParameter("maxwidth", PHOTO_MAX_WIDTH.toString())
            .addQueryParameter("photoreference", photoReference)
            .addQueryParameter("key", apiKey)
            .build()
        val request = Request.Builder().url(url).build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}
