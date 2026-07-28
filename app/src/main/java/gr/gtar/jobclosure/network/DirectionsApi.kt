package gr.gtar.jobclosure.network

import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsApi {

    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String,
    ): DirectionsResponse

    companion object {
        const val BASE_URL = "https://maps.googleapis.com/"
    }
}
