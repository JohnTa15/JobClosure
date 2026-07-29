package gr.gtar.jobclosure.shared.calendar

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BookingMetadata(
    val bookingId: String,
    val role: String,
    val type: String? = null,
    val hasDrone: Boolean = false,
    val title: String? = null,
    val venueName: String? = null,
)

/**
 * Encodes/decodes the structured booking fields a plain calendar event has no room for, as a
 * marked line appended to the event's description. Kept in the description rather than
 * CalendarContract's ExtendedProperties table: description sync is basic, well-established
 * behaviour every calendar client (and Android's own CalendarContract sync adapter) is guaranteed
 * to get right, whereas whether ExtendedProperties round-trips through to Google Calendar's
 * extendedProperties.private isn't something this project could verify without a real device and
 * account - so the one mechanism actually used here is the one that's certain to work.
 */
object BookingMetadataCodec {
    private const val MARKER = "::jobclosure::"
    private val json = Json { ignoreUnknownKeys = true }

    const val ROLE_CEREMONY = "ceremony"
    const val ROLE_RECEPTION = "reception"

    fun encode(userNotes: String, metadata: BookingMetadata): String {
        val cleanNotes = userNotes(userNotes)
        val metadataLine = MARKER + json.encodeToString(metadata)
        return if (cleanNotes.isEmpty()) metadataLine else "$cleanNotes\n\n$metadataLine"
    }

    fun decodeMetadata(description: String?): BookingMetadata? {
        val line = description?.lineSequence()?.firstOrNull { it.startsWith(MARKER) } ?: return null
        return runCatching { json.decodeFromString<BookingMetadata>(line.removePrefix(MARKER)) }.getOrNull()
    }

    /** The human-readable part of a description, with the metadata line (if any) stripped out. */
    fun userNotes(description: String?): String =
        (description ?: "").lineSequence().filterNot { it.startsWith(MARKER) }.joinToString("\n").trim()
}
