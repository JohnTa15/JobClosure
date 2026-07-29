package gr.gtar.jobclosure.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo: free weather/elevation API, no API key required. */
interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
    ): WeatherResponse

    @GET("v1/elevation")
    suspend fun getElevation(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): ElevationResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }
}

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "current_weather") val currentWeather: CurrentWeather?,
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    val temperature: Double,
    @Json(name = "windspeed") val windSpeed: Double,
    @Json(name = "winddirection") val windDirection: Double,
    @Json(name = "weathercode") val weatherCode: Int,
)

@JsonClass(generateAdapter = true)
data class ElevationResponse(
    val elevation: List<Double> = emptyList(),
)
