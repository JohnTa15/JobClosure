package gr.gtar.jobclosure.drone

import java.util.Locale

/**
 * Where Drone Aware - GR lives, and the coordinate format it speaks.
 *
 * DAGR publishes no way to deep-link a position - it is a single-page app whose only public entry
 * point is the map page, and its own Quick Start guide tells pilots to type WGS84 coordinates into
 * the flight manager. So the app opens this URL in its own WebView and fills the forms there (see
 * gr.gtar.jobclosure.dagr), rather than guessing at a query parameter that would silently centre on
 * nothing. If HASP ever publishes a deep link, this is the one place that changes.
 */
object DroneAware {

    const val MAP_URL = "https://dagr.hasp.gov.gr/#map_page"

    /** Decimal degrees, the form DAGR's flight manager and the HCAA request forms both use. */
    fun formatCoordinates(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
}
