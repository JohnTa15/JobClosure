package gr.gtar.jobclosure.shared.model

import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

/**
 * A booking as understood by the desktop app: backed entirely by Google Calendar events (the
 * ceremony event, plus an optional separate reception event), no local database. [eventId]/
 * [receptionEventId] are null for a booking that hasn't been saved (created) yet.
 */
@OptIn(ExperimentalUuidApi::class)
data class Booking(
    /** Correlates the ceremony and reception Calendar events that make up one booking. Stable
     *  across edits; generated fresh only when a booking is first created. */
    val bookingId: String = Uuid.random().toString(),
    val eventId: String? = null,
    val receptionEventId: String? = null,
    val title: String,
    val type: BookingType,
    val notes: String = "",
    val clientPhone: String = "",
    val price: Double = 0.0,
    // false = only tentatively booked, not yet confirmed by the client.
    val isConfirmed: Boolean = true,
    val hasDrone: Boolean = false,
    val churchName: String = "",
    val churchAddress: String = "",
    val ceremonyStart: Instant,
    val ceremonyDurationMinutes: Int = 60,
    val hasReception: Boolean = false,
    val receptionVenueName: String = "",
    val receptionVenueAddress: String = "",
    val receptionStart: Instant? = null,
    val receptionDurationMinutes: Int = 240,
) {
    val ceremonyEnd: Instant get() = ceremonyStart + ceremonyDurationMinutes.minutes
    val receptionEnd: Instant? get() = receptionStart?.plus(receptionDurationMinutes.minutes)

    val occupiedStart: Instant get() = ceremonyStart

    val occupiedEnd: Instant
        get() = if (hasReception && receptionStart != null) {
            maxOf(receptionEnd ?: ceremonyEnd, ceremonyEnd)
        } else {
            ceremonyEnd
        }

    /** True if this booking's busy window overlaps [other]'s. */
    fun conflictsWith(other: Booking): Boolean =
        occupiedStart < other.occupiedEnd && other.occupiedStart < occupiedEnd
}

/** All bookings in [existing] whose busy window overlaps [candidate]'s, excluding itself. */
fun findConflicts(candidate: Booking, existing: List<Booking>): List<Booking> =
    existing.filter { it.bookingId != candidate.bookingId }
        .filter { candidate.conflictsWith(it) }
