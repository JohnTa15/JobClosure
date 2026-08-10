package gr.gtar.jobclosure.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gr.gtar.jobclosure.data.MapsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

private const val ZOOM = 14
private const val TILE_PX = 256
private val DISPLAY_SIZE = 200.dp
private const val STATIC_MAP_URL = "https://maps.googleapis.com/maps/api/staticmap"

private data class TileLocation(val tileX: Int, val tileY: Int, val pixelX: Double, val pixelY: Double)

/** An OSM tile still needs its pin drawn on top at [tile]'s offset; a Google static map already
 *  has the marker baked into the returned image, so it carries no tile location. */
private data class MapImage(val bitmap: ImageBitmap, val tile: TileLocation?)

private sealed interface MapResult {
    /** Carries the undecoded [bytes] alongside the image so the caller can cache them. */
    class Success(val image: MapImage, val bytes: ByteArray) : MapResult
    data class Failure(val message: String) : MapResult
}

/**
 * Small, non-interactive map preview centered on [latitude]/[longitude]. Follows whichever provider
 * is configured in Settings: a Google Static Maps image when Google is selected and a key is
 * available, otherwise a single tile from the standard OSM tile server (the same free service
 * Nominatim/OSRM already come from elsewhere in this app) with a pin drawn at the exact pixel offset
 * within it. Not pannable or zoomable - just a quick visual reference before opening full
 * turn-by-turn directions.
 */
@Composable
fun MiniMapPreview(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    provider: MapsProvider = MapsProvider.OPENSTREETMAP,
    googleApiKey: String = "",
) {
    val useGoogle = provider == MapsProvider.GOOGLE && googleApiKey.isNotBlank()
    val cacheRoot = LocalContext.current.cacheDir
    var loaded by remember(latitude, longitude, useGoogle) { mutableStateOf<MapImage?>(null) }
    var error by remember(latitude, longitude, useGoogle) { mutableStateOf<String?>(null) }

    LaunchedEffect(latitude, longitude, useGoogle, googleApiKey) {
        loaded = null
        error = null
        when (val result = withContext(Dispatchers.IO) {
            // The pin overlay's position is derived locally from the coordinates, so only the
            // image itself has to survive in the cache.
            val tile = if (useGoogle) null else locateTile(latitude, longitude, ZOOM)
            val cacheKey = "map|google=$useGoogle|z=$ZOOM|at=$latitude,$longitude"
            val cached = RemoteImageCache.load(cacheRoot, cacheKey)
                ?.takeIf { it.isNotEmpty() }
                ?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bytes to it } }
            if (cached != null) {
                MapResult.Success(MapImage(cached.second.asImageBitmap(), tile), cached.first)
            } else {
                val fetched = if (useGoogle) {
                    loadGoogleStaticMap(latitude, longitude, googleApiKey)
                } else {
                    loadTile(latitude, longitude, tile!!)
                }
                (fetched as? MapResult.Success)?.let { RemoteImageCache.store(cacheRoot, cacheKey, it.bytes) }
                fetched
            }
        }) {
            is MapResult.Success -> loaded = result.image
            is MapResult.Failure -> error = result.message
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DISPLAY_SIZE)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val current = loaded
            when {
                current != null -> {
                    Image(
                        bitmap = current.bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(DISPLAY_SIZE),
                        contentScale = ContentScale.Crop,
                    )
                    current.tile?.let { tile ->
                        val pinXDp = (tile.pixelX / TILE_PX * DISPLAY_SIZE.value).dp - 16.dp
                        val pinYDp = (tile.pixelY / TILE_PX * DISPLAY_SIZE.value).dp - 32.dp
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFE85D4A),
                            modifier = Modifier.size(32.dp).offset(x = pinXDp, y = pinYDp),
                        )
                    }
                }
                error != null -> Box(
                    Modifier.fillMaxWidth().height(DISPLAY_SIZE).padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Ο χάρτης δεν φορτώθηκε.\n$error",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> Box(Modifier.fillMaxWidth().height(DISPLAY_SIZE), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        Text(
            if (useGoogle) "© Google" else "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * A failed Static Maps call answers with a plain-text explanation ("This API project is not
 * authorized to use this API", "The provided API key is expired", ...) rather than an image, so the
 * body is passed straight through to the UI - guessing at which of the several Maps Platform APIs
 * still needs enabling is exactly the thing that message already answers.
 */
private fun loadGoogleStaticMap(latitude: Double, longitude: Double, apiKey: String): MapResult {
    val center = "$latitude,$longitude"
    val url = STATIC_MAP_URL.toHttpUrl().newBuilder()
        .addQueryParameter("center", center)
        .addQueryParameter("zoom", ZOOM.toString())
        .addQueryParameter("size", "640x320")
        .addQueryParameter("scale", "2")
        .addQueryParameter("markers", "color:red|$center")
        .addQueryParameter("key", apiKey)
        .build()
    return try {
        OkHttpClient().newCall(Request.Builder().url(url).build()).execute().use { response ->
            val bytes = response.body?.bytes()
            if (!response.isSuccessful) {
                val explanation = bytes?.toString(Charsets.UTF_8)?.trim()?.takeIf { it.isNotBlank() }
                return MapResult.Failure(explanation ?: "Google Static Maps: HTTP ${response.code}")
            }
            if (bytes == null) return MapResult.Failure("Κενή απάντηση από το Google Static Maps.")
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return MapResult.Failure(
                    bytes.toString(Charsets.UTF_8).trim().takeIf { it.isNotBlank() }
                        ?: "Η απάντηση του Google Static Maps δεν ήταν εικόνα.",
                )
            MapResult.Success(MapImage(bitmap.asImageBitmap(), tile = null), bytes)
        }
    } catch (e: Exception) {
        MapResult.Failure(e.message ?: "Αποτυχία σύνδεσης στο Google Static Maps.")
    }
}

private fun locateTile(latitude: Double, longitude: Double, zoom: Int): TileLocation {
    val n = 1 shl zoom
    val xTileF = (longitude + 180.0) / 360.0 * n
    val latRad = Math.toRadians(latitude)
    val yTileF = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
    val xTile = floor(xTileF).toInt()
    val yTile = floor(yTileF).toInt()
    return TileLocation(
        tileX = xTile,
        tileY = yTile,
        pixelX = (xTileF - xTile) * TILE_PX,
        pixelY = (yTileF - yTile) * TILE_PX,
    )
}

private fun loadTile(latitude: Double, longitude: Double, tile: TileLocation): MapResult {
    val url = "https://tile.openstreetmap.org/$ZOOM/${tile.tileX}/${tile.tileY}.png"
    return try {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "JobClosure-Android-App (single-user booking tracker, no contact address)")
            .build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) return MapResult.Failure("OpenStreetMap: HTTP ${response.code}")
            val bytes = response.body?.bytes() ?: return MapResult.Failure("Κενή απάντηση από τον OpenStreetMap.")
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return MapResult.Failure("Το tile του OpenStreetMap δεν διαβάστηκε.")
            MapResult.Success(MapImage(bitmap.asImageBitmap(), tile), bytes)
        }
    } catch (e: Exception) {
        MapResult.Failure(e.message ?: "Αποτυχία σύνδεσης στον OpenStreetMap.")
    }
}
