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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

private const val ZOOM = 16
private const val TILE_PX = 256
private val DISPLAY_SIZE = 200.dp

private data class TileLocation(val tileX: Int, val tileY: Int, val pixelX: Double, val pixelY: Double)

/**
 * Small, non-interactive OpenStreetMap preview centered on [latitude]/[longitude]: a single tile
 * fetched from the standard OSM tile server (the same free service Nominatim/OSRM already come
 * from elsewhere in this app), with a pin drawn at the exact pixel offset within it. Not pannable
 * or zoomable - just a quick visual reference before opening full turn-by-turn directions.
 */
@Composable
fun MiniMapPreview(latitude: Double, longitude: Double, modifier: Modifier = Modifier) {
    var loaded by remember(latitude, longitude) { mutableStateOf<Pair<ImageBitmap, TileLocation>?>(null) }
    var failed by remember(latitude, longitude) { mutableStateOf(false) }

    LaunchedEffect(latitude, longitude) {
        loaded = null
        failed = false
        val result = withContext(Dispatchers.IO) { loadTile(latitude, longitude) }
        if (result == null) failed = true else loaded = result
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
                    val (bitmap, tile) = current
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(DISPLAY_SIZE),
                        contentScale = ContentScale.Crop,
                    )
                    val pinXDp = (tile.pixelX / TILE_PX * DISPLAY_SIZE.value).dp - 16.dp
                    val pinYDp = (tile.pixelY / TILE_PX * DISPLAY_SIZE.value).dp - 32.dp
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFE85D4A),
                        modifier = Modifier.size(32.dp).offset(x = pinXDp, y = pinYDp),
                    )
                }
                failed -> Box(Modifier.fillMaxWidth().height(DISPLAY_SIZE), contentAlignment = Alignment.Center) {
                    Text("Ο χάρτης δεν φορτώθηκε", style = MaterialTheme.typography.bodySmall)
                }
                else -> Box(Modifier.fillMaxWidth().height(DISPLAY_SIZE), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        Text(
            "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
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

private fun loadTile(latitude: Double, longitude: Double): Pair<ImageBitmap, TileLocation>? {
    val tile = locateTile(latitude, longitude, ZOOM)
    val url = "https://tile.openstreetmap.org/$ZOOM/${tile.tileX}/${tile.tileY}.png"
    return try {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "JobClosure-Android-App (single-user booking tracker, no contact address)")
            .build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            bitmap.asImageBitmap() to tile
        }
    } catch (e: Exception) {
        null
    }
}
