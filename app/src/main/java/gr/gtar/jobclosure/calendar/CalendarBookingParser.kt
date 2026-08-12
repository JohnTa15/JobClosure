package gr.gtar.jobclosure.calendar

import gr.gtar.jobclosure.data.BookingType
import java.text.Normalizer
import java.util.Locale

/** A parsed sacrament found in the device calendar, ready to be turned into a booking. */
data class ParsedCalendarBooking(
    val calendarEventId: Long,
    val calendarId: Long,
    val title: String,
    val type: BookingType,
    val hasDrone: Boolean,
    val clientName: String,
    val venueName: String,
    val venueAddress: String,
    val startMillis: Long,
    val durationMinutes: Int,
    val notes: String,
)

/**
 * Recognises the sacraments the user has been keeping in their phone calendar by hand, so years of
 * past jobs can be pulled into the app instead of retyped.
 *
 * The convention being read is the user's own: an event titled "Γάμος Παπαδόπουλου", "Βάπτιση
 * Μαρίας - drone", and so on. Matching is deliberately accent- and case-insensitive, because a
 * title typed on a phone keyboard over several years is not consistently accented ("Βάπτιση",
 * "βαπτιση", "ΒΑΦΤΙΣΗ" all appear), and both the -π- and -φ- spellings of βάπτιση/βάφτιση are in
 * common use.
 */
object CalendarBookingParser {

    /** Anywhere in the event's text, not just the title - "drone" is as likely to be a note. */
    private const val DRONE_MARKER = "drone"

    // Stems rather than whole words, because the case endings vary freely in a hand-typed title
    // ("Γάμος", "Γάμου", "Γάμο", "Γάμων", "Βαπτίσεις") and every one of those is the same job.
    // Στεφάνωμα is included as the other common word for a wedding.
    private val weddingStems = listOf("γαμ", "στεφανωμ")
    private val baptismStems = listOf("βαπτ", "βαφτ")

    /** Strips accents and lowercases, so "Βάπτιση" and "ΒΑΠΤΙΣΗ" compare equal. Greek final sigma
     *  is folded too, otherwise "γάμος" would not match a stem written "γάμοσ". */
    private fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale("el", "GR"))
            .replace('ς', 'σ')

    /**
     * Returns null when the event isn't one of the recognised sacraments - the calendar is full of
     * unrelated appointments and importing those would be worse than importing nothing.
     */
    fun parse(event: CalendarEvent): ParsedCalendarBooking? {
        // Trimmed before folding, so the folded and raw titles keep the same character offsets and
        // the matched word's length can be used to slice the client name out of the original.
        val rawTitle = event.title.trim()
        val foldedTitle = fold(rawTitle)
        if (foldedTitle.isBlank()) return null

        // Matched anywhere in the title, not just at the start: plenty of entries read
        // "Παπαδόπουλος - γάμος" or "Αγ. Νικόλαος βάπτιση". Being permissive is cheap here because
        // nothing is imported without passing through the review list first, where a false positive
        // costs one untick - whereas a missed sacrament is invisible and stays missed.
        val mentionsWedding = weddingStems.any { foldedTitle.contains(it) }
        val mentionsBaptism = baptismStems.any { foldedTitle.contains(it) }
        if (!mentionsWedding && !mentionsBaptism) return null

        val type = when {
            mentionsWedding && mentionsBaptism -> BookingType.WEDDING_AND_BAPTISM
            mentionsBaptism -> BookingType.BAPTISM
            else -> BookingType.WEDDING
        }

        val haystack = fold("$rawTitle ${event.description} ${event.location}")
        val hasDrone = haystack.contains(DRONE_MARKER)

        // Clamped before narrowing: an all-day or mis-entered event can span days, and a ceremony
        // length taken literally from that would poison the booking's busy window.
        val durationMinutes = ((event.endMillis - event.startMillis) / 60_000L)
            .coerceIn(MIN_DURATION_MINUTES.toLong(), MAX_DURATION_MINUTES.toLong())
            .toInt()

        return ParsedCalendarBooking(
            calendarEventId = event.id,
            calendarId = event.calendarId,
            title = rawTitle,
            type = type,
            hasDrone = hasDrone,
            clientName = clientNameFrom(rawTitle),
            venueName = event.location.substringBefore(",").trim(),
            venueAddress = event.location.trim(),
            startMillis = event.startMillis,
            durationMinutes = durationMinutes,
            notes = event.description.trim(),
        )
    }

    /**
     * The client is whatever the title says once the sacrament words, the drone marker and any
     * leftover separators are taken out: "Γάμος Παπαδόπουλου - drone" and "Παπαδόπουλου (γάμος)"
     * both leave "Παπαδόπουλου". Falls back to the whole title when that empties it, so an event
     * titled just "Γάμος" still lands with something readable in the client field.
     */
    private fun clientNameFrom(rawTitle: String): String {
        // Words are dropped whole rather than by character offset: a stem can sit anywhere in the
        // title now, and cutting at a stem's length would leave its ending behind ("ς" from "γάμος").
        val kept = rawTitle
            .split(' ', '\t')
            .filter { word ->
                val folded = fold(word)
                folded.isNotBlank() &&
                    (weddingStems + baptismStems).none { folded.contains(it) } &&
                    !folded.contains(DRONE_MARKER)
            }
            .joinToString(" ")

        val cleaned = kept
            .trim()
            .trim('&', '-', '–', ':', ',', '/', '(', ')', '.')
            .trim()
        return cleaned.ifBlank { rawTitle.trim() }
    }

    private const val MIN_DURATION_MINUTES = 15
    private const val MAX_DURATION_MINUTES = 12 * 60
}
