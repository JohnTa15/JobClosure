package gr.gtar.jobclosure.network

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {

    private val moshi: Moshi by lazy {
        Moshi.Builder().build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    private val googleMapsRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(DirectionsApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val openMeteoRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val openAipRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(OpenAipApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val directionsApi: DirectionsApi by lazy { googleMapsRetrofit.create(DirectionsApi::class.java) }
    val geocodingApi: GeocodingApi by lazy { googleMapsRetrofit.create(GeocodingApi::class.java) }
    val openMeteoApi: OpenMeteoApi by lazy { openMeteoRetrofit.create(OpenMeteoApi::class.java) }
    val openAipApi: OpenAipApi by lazy { openAipRetrofit.create(OpenAipApi::class.java) }

    val travelTimeRepository: TravelTimeRepository by lazy { TravelTimeRepository(directionsApi) }
    val droneConditionsRepository: DroneConditionsRepository by lazy {
        DroneConditionsRepository(geocodingApi, openMeteoApi)
    }
    val droneZoneRepository: DroneZoneRepository by lazy {
        DroneZoneRepository(geocodingApi, openAipApi)
    }
}
