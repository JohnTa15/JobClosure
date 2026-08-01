package gr.gtar.jobclosure.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds clientPhone and price as plain new columns - by now real bookings are in active use on the
 * user's phone, so (unlike the earlier destructive migrations before this app was actually in use)
 * this one preserves existing rows instead of wiping the table.
 */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bookings ADD COLUMN clientPhone TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE bookings ADD COLUMN price REAL NOT NULL DEFAULT 0.0")
    }
}

@Database(entities = [Booking::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookingDao(): BookingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jobclosure.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    // Only reached for an install older than version 3, which predates this app
                    // being in real use - safe to recreate the table in that one remaining case.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
