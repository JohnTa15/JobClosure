package gr.gtar.jobclosure.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Type of job. Church-sacrament types (WEDDING/BAPTISM/BOTH) are the ones that
 * expose church + reception fields; the rest are here because the same user
 * also books school events and stage shows and wants everything on one calendar.
 */
enum class BookingType(val displayName: String) {
    WEDDING("Γάμος"),
    BAPTISM("Βάφτιση"),
    WEDDING_AND_BAPTISM("Γάμος & Βάφτιση"),
    SCHOOL_EVENT("Σχολική Εκδήλωση"),
    PERFORMANCE("Παράσταση"),
    OTHER("Άλλο");

    val isChurchSacrament: Boolean
        get() = this == WEDDING || this == BAPTISM || this == WEDDING_AND_BAPTISM
}

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Stable identifier shared with the desktop app's Google Calendar sync: it's what lets a
    // booking's ceremony and reception events be recognised as one booking, and what a booking
    // created on the phone is recognised by when it shows up (via Calendar sync) on the desktop.
    val bookingId: String = java.util.UUID.randomUUID().toString(),

    val title: String,
    val type: BookingType,
    val notes: String = "",

    val hasDrone: Boolean = false,

    // Church ceremony (relevant when type.isChurchSacrament)
    val churchName: String = "",
    val churchAddress: String = "",
    val ceremonyStart: LocalDateTime,
    val ceremonyDurationMinutes: Int = 60,

    // Reception (optional; may follow the church ceremony at a different venue)
    val hasReception: Boolean = false,
    val receptionVenueName: String = "",
    val receptionVenueAddress: String = "",
    val receptionStart: LocalDateTime? = null,
    val receptionDurationMinutes: Int = 240,

    // Device calendar linkage (Google Calendar / Samsung Calendar), so we can
    // update or remove the events later instead of duplicating them.
    val calendarId: Long? = null,
    val churchCalendarEventId: Long? = null,
    val receptionCalendarEventId: Long? = null,

    // Denormalized busy window used for fast overlap ("do I already have a job
    // then?") queries. Always recomputed from the fields above before saving.
    val occupiedStartEpochMinute: Long = 0,
    val occupiedEndEpochMinute: Long = 0,
) {
    val ceremonyEnd: LocalDateTime
        get() = ceremonyStart.plusMinutes(ceremonyDurationMinutes.toLong())

    val receptionEnd: LocalDateTime?
        get() = receptionStart?.plusMinutes(receptionDurationMinutes.toLong())

    /** Single contiguous busy window covering the ceremony and, if any, the reception. */
    val occupiedStart: LocalDateTime
        get() = ceremonyStart

    val occupiedEnd: LocalDateTime
        get() = if (hasReception && receptionStart != null) {
            maxOf(receptionEnd!!, ceremonyEnd)
        } else {
            ceremonyEnd
        }

    companion object {
        fun epochMinute(dateTime: LocalDateTime): Long =
            dateTime.atZone(ZoneId.systemDefault()).toEpochSecond() / 60
    }

    /** Returns a copy with occupiedStart/EndEpochMinute recomputed from the current fields. */
    fun withComputedOccupiedWindow(): Booking = copy(
        occupiedStartEpochMinute = epochMinute(occupiedStart),
        occupiedEndEpochMinute = epochMinute(occupiedEnd),
    )
}
