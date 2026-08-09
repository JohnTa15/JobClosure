package gr.gtar.jobclosure.network

import com.squareup.moshi.Moshi
import gr.gtar.jobclosure.update.UpdateRepository
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

    private val gitHubRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GitHubReleaseApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val placesRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(PlacesApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val nominatimRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NominatimApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val osrmRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(OsrmApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val directionsApi: DirectionsApi by lazy { googleMapsRetrofit.create(DirectionsApi::class.java) }
    val geocodingApi: GeocodingApi by lazy { googleMapsRetrofit.create(GeocodingApi::class.java) }
    val placesApi: PlacesApi by lazy { placesRetrofit.create(PlacesApi::class.java) }
    val openMeteoApi: OpenMeteoApi by lazy { openMeteoRetrofit.create(OpenMeteoApi::class.java) }
    val gitHubReleaseApi: GitHubReleaseApi by lazy { gitHubRetrofit.create(GitHubReleaseApi::class.java) }
    val nominatimApi: NominatimApi by lazy { nominatimRetrofit.create(NominatimApi::class.java) }
    val osrmApi: OsrmApi by lazy { osrmRetrofit.create(OsrmApi::class.java) }

    val travelTimeRepository: TravelTimeRepository by lazy {
        TravelTimeRepository(directionsApi, nominatimApi, osrmApi)
    }
    val droneConditionsRepository: DroneConditionsRepository by lazy {
        DroneConditionsRepository(geocodingApi, nominatimApi, openMeteoApi)
    }
    val placeSearchRepository: PlaceSearchRepository by lazy { PlaceSearchRepository(nominatimApi, placesApi) }
    val updateRepository: UpdateRepository by lazy { UpdateRepository(gitHubReleaseApi) }
}
