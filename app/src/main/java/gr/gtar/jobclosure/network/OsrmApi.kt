package gr.gtar.jobclosure.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * OSRM (Open Source Routing Machine): free, no-API-key driving-route calculator, using OpenStreetMap
 * road data. Uses the public demo server at router.project-osrm.org - a fair-use demo, not meant for
 * heavy production traffic, but fine for this app's occasional single-user lookups.
 */
interface OsrmApi {

    /** [coordinates] must already be "lon1,lat1;lon2,lat2" (note: longitude first) - passed with
     *  encoded = true since commas/semicolons are valid unencoded in this path segment and OSRM
     *  expects them literally. */
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "false",
    ): OsrmRouteResponse

    companion object {
        const val BASE_URL = "https://router.project-osrm.org/"
    }
}

@JsonClass(generateAdapter = true)
data class OsrmRouteResponse(
    val code: String,
    val routes: List<OsrmRoute> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class OsrmRoute(
    val duration: Double,
    val distance: Double,
)
