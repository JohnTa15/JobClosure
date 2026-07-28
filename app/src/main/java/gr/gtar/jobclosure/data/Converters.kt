package gr.gtar.jobclosure.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class Converters {

    @TypeConverter
    fun fromEpochSecond(value: Long?): LocalDateTime? =
        value?.let { LocalDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneId.systemDefault()) }

    @TypeConverter
    fun toEpochSecond(dateTime: LocalDateTime?): Long? =
        dateTime?.atZone(ZoneId.systemDefault())?.toEpochSecond()

    @TypeConverter
    fun fromBookingType(value: String): BookingType = BookingType.valueOf(value)

    @TypeConverter
    fun toBookingType(type: BookingType): String = type.name
}
