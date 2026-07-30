package gr.gtar.jobclosure.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

sealed interface LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult
    data class Error(val message: String) : LocationResult
}

/**
 * Wraps the platform LocationManager (no Google Play Services dependency, so it works the same
 * whether the user picked OpenStreetMap or Google Maps as their provider) to fetch a one-off
 * "current location" fix for the home-address field.
 */
object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun getCurrentLocation(context: Context): LocationResult {
        if (!hasLocationPermission(context)) {
            return LocationResult.Error("Δεν έχει δοθεί άδεια τοποθεσίας")
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return LocationResult.Error("Η υπηρεσία τοποθεσίας δεν είναι διαθέσιμη")

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return LocationResult.Error("Ενεργοποίησε την τοποθεσία (GPS) στη συσκευή")
        }

        return try {
            val location = requestFreshLocation(context, locationManager, provider)
                ?: lastKnownLocation(locationManager)
                ?: return LocationResult.Error("Δεν ήταν δυνατός ο εντοπισμός τοποθεσίας")
            LocationResult.Success(location.latitude, location.longitude)
        } catch (e: SecurityException) {
            LocationResult.Error("Δεν έχει δοθεί άδεια τοποθεσίας")
        }
    }

    private fun lastKnownLocation(locationManager: LocationManager): Location? =
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

    private suspend fun requestFreshLocation(
        context: Context,
        locationManager: LocationManager,
        provider: String,
    ): Location? = suspendCancellableCoroutine { continuation ->
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val cancellationSignal = CancellationSignal()
                continuation.invokeOnCancellation { cancellationSignal.cancel() }
                locationManager.getCurrentLocation(provider, cancellationSignal, context.mainExecutor) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            } else {
                @Suppress("DEPRECATION")
                locationManager.requestSingleUpdate(
                    provider,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (continuation.isActive) continuation.resume(location)
                        }
                        override fun onProviderDisabled(provider: String) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    },
                    Looper.getMainLooper(),
                )
            }
        } catch (e: SecurityException) {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
