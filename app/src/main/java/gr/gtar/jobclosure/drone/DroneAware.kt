package gr.gtar.jobclosure.drone

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.getSystemService
import java.util.Locale

/**
 * Opening a venue in DAGR (Drone Aware - GR, the HCAA/HASP airspace map at dagr.hasp.gov.gr).
 *
 * DAGR publishes no way to deep-link a position: it is a single-page app whose only public entry
 * point is the map page itself, and its own Quick Start guide tells pilots to paste WGS84
 * coordinates into the flight manager. So this does what DAGR expects rather than guessing at a
 * query parameter that would silently centre on nothing - it puts the venue's coordinates on the
 * clipboard and opens the map, leaving one paste between the booking and the airspace check.
 */
object DroneAware {

    const val MAP_URL = "https://dagr.hasp.gov.gr/#map_page"

    /** Decimal degrees, the form DAGR's flight manager and the HCAA request forms both use. */
    fun formatCoordinates(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.6f, %.6f", latitude, longitude)

    /**
     * Copies [coordinates] (when geocoding has produced any) and opens DAGR. [venueLabel] names the
     * venue in the confirmation, since with both buttons on screen "coordinates copied" alone would
     * not say which of the two ended up on the clipboard.
     */
    fun open(context: Context, venueLabel: String, coordinates: Pair<Double, Double>?) {
        if (coordinates != null) {
            val text = formatCoordinates(coordinates.first, coordinates.second)
            context.getSystemService<ClipboardManager>()
                ?.setPrimaryClip(ClipData.newPlainText("Συντεταγμένες: $venueLabel", text))
            // Android 13+ shows its own copy confirmation, so a second toast there is just noise -
            // but the part worth saying is what to do with them, which that popup doesn't cover.
            val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "$venueLabel: επικόλλησε τις συντεταγμένες στην αναζήτηση του DAGR."
            } else {
                "Αντιγράφηκαν οι συντεταγμένες ($venueLabel): $text"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(
                context,
                "Δεν βρέθηκαν συντεταγμένες για $venueLabel - έλεγξε τη διεύθυνση.",
                Toast.LENGTH_LONG,
            ).show()
        }
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MAP_URL)))
    }
}
