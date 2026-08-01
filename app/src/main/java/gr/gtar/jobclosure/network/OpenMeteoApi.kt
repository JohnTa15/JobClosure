package gr.gtar.jobclosure.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo: free weather/elevation API, no API key required. */
interface OpenMeteoApi {

    /**
     * Daily forecast for a specific date (Open-Meteo only forecasts reliably within roughly the
     * next 16 days), used so drone conditions actually reflect the booking's date instead of
     * always showing today's current weather regardless of when the event is.
     */
    @GET("v1/forecast")
    suspend fun getDailyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String =
            "weathercode,temperature_2m_max,temperature_2m_min,windspeed_10m_max,winddirection_10m_dominant," +
                "precipitation_probability_max",
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
        @Query("timezone") timezone: String = "auto",
    ): DailyForecastResponse

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
data class ElevationResponse(
    val elevation: List<Double> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DailyForecastResponse(
    val daily: DailyWeather? = null,
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    val time: List<String> = emptyList(),
    @Json(name = "weathercode") val weatherCode: List<Int> = emptyList(),
    @Json(name = "temperature_2m_max") val temperatureMax: List<Double> = emptyList(),
    @Json(name = "temperature_2m_min") val temperatureMin: List<Double> = emptyList(),
    @Json(name = "windspeed_10m_max") val windSpeedMax: List<Double> = emptyList(),
    @Json(name = "winddirection_10m_dominant") val windDirectionDominant: List<Double> = emptyList(),
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int> = emptyList(),
)
