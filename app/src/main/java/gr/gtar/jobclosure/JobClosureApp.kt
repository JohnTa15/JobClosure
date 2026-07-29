package gr.gtar.jobclosure

import android.app.Application
import gr.gtar.jobclosure.data.AppDatabase
import gr.gtar.jobclosure.data.BookingRepository
import gr.gtar.jobclosure.data.SettingsRepository
import gr.gtar.jobclosure.network.NetworkModule

class JobClosureApp : Application() {

    val bookingRepository: BookingRepository by lazy {
        BookingRepository(AppDatabase.getInstance(this).bookingDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    val travelTimeRepository get() = NetworkModule.travelTimeRepository
    val droneConditionsRepository get() = NetworkModule.droneConditionsRepository
    val droneZoneRepository get() = NetworkModule.droneZoneRepository
}
