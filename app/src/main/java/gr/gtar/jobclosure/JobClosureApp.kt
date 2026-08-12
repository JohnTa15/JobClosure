package gr.gtar.jobclosure

import android.app.Application
import gr.gtar.jobclosure.crash.CrashReporter
import gr.gtar.jobclosure.data.AppDatabase
import gr.gtar.jobclosure.data.BookingRepository
import gr.gtar.jobclosure.data.SettingsRepository
import gr.gtar.jobclosure.network.NetworkModule

class JobClosureApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }

    val bookingRepository: BookingRepository by lazy {
        BookingRepository(AppDatabase.getInstance(this).bookingDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    val travelTimeRepository get() = NetworkModule.travelTimeRepository
    val droneConditionsRepository get() = NetworkModule.droneConditionsRepository
    val placeSearchRepository get() = NetworkModule.placeSearchRepository
    val updateRepository get() = NetworkModule.updateRepository
    val crashReportSender get() = NetworkModule.crashReportSender
}
