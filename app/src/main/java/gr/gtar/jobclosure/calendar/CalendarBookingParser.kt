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

    // Longest first, so "γάμου" matches the whole word rather than stopping at the "γάμο" prefix
    // and leaving a stray letter at the head of the client name.
    private val weddingWords = listOf("γαμος", "γαμου", "γαμο").sortedByDescending { it.length }
    private val baptismWords = listOf("βαπτιση", "βαφτιση", "βαπτισι", "βαφτισι").sortedByDescending { it.length }

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

        val matchedWord = (weddingWords + baptismWords).firstOrNull { foldedTitle.startsWith(it) }
            ?: return null

        val mentionsWedding = weddingWords.any { foldedTitle.contains(it) }
        val mentionsBaptism = baptismWords.any { foldedTitle.contains(it) }
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
            clientName = clientNameFrom(rawTitle, matchedWord),
            venueName = event.location.substringBefore(",").trim(),
            venueAddress = event.location.trim(),
            startMillis = event.startMillis,
            durationMinutes = durationMinutes,
            notes = event.description.trim(),
        )
    }

    /**
     * Everything after the sacrament word is the client: "Γάμος Παπαδόπουλου - drone" leaves
     * "Παπαδόπουλου". Falls back to the whole title when that leaves nothing usable, so an event
     * titled just "Γάμος" still imports with something readable in the client field.
     */
    private fun clientNameFrom(rawTitle: String, matchedWord: String): String {
        // The folded and raw titles line up character-for-character (folding only drops combining
        // marks and changes case), so the match length is a safe offset into the original.
        val remainder = rawTitle.drop(matchedWord.length)
            .trim()
            .trimStart('&', '-', '–', ':', ',', '/')
            .let { stripSecondSacramentWord(it) }
            .trim()
            .trimStart('&', '-', '–', ':', ',', '/')
            .trim()
        val withoutDrone = remainder
            .replace(Regex("(?i)\\bdrone\\b"), "")
            .trim()
            .trim('-', '–', ':', ',', '/', '(', ')')
            .trim()
        return withoutDrone.ifBlank { rawTitle.trim() }
    }

    /** "Γάμος & Βάπτιση Ελένης" should yield "Ελένης", not "Βάπτιση Ελένης". */
    private fun stripSecondSacramentWord(text: String): String {
        val folded = fold(text)
        val second = (weddingWords + baptismWords).firstOrNull { folded.startsWith(it) } ?: return text
        return text.drop(second.length)
    }

    private const val MIN_DURATION_MINUTES = 15
    private const val MAX_DURATION_MINUTES = 12 * 60
}
