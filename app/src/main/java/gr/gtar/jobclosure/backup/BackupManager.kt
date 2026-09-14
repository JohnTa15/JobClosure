package gr.gtar.jobclosure.backup

import android.content.Context
import gr.gtar.jobclosure.calendar.CalendarEvent
import gr.gtar.jobclosure.calendar.CalendarHelper
import gr.gtar.jobclosure.data.ActivityAction
import gr.gtar.jobclosure.data.ActivityRepository
import gr.gtar.jobclosure.data.Booking
import gr.gtar.jobclosure.data.BookingRepository
import gr.gtar.jobclosure.data.BookingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One backup on disk, as the history screen lists it. */
data class BackupFile(
    val file: File,
    val createdAt: LocalDateTime,
    val bookingCount: Int,
    val sizeBytes: Long,
)

/** What a restore put back. Nothing is ever overwritten or removed, so this only counts additions. */
data class RestoreResult(
    val restored: Int,
    val alreadyPresent: Int,
)

private const val FORMAT_VERSION = 1
private const val KEEP_BACKUPS = 10
private val fileStampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.UK)

/**
 * Writes the app's bookings, and what their calendar entries say, to a JSON file under the app's
 * own storage.
 *
 * This exists because the app no longer removes anything from the calendar: the calendar is the
 * user's, and the app's copy of a job (price, phone, notes, drone) lives only in the app, where a
 * reinstall or a wiped phone would take it with no way back. A backup is a plain file, readable
 * years later without this app, and restoring only ever *adds* jobs back.
 */
class BackupManager(
    private val context: Context,
    private val bookingRepository: BookingRepository,
    private val activityRepository: ActivityRepository,
) {

    private val directory: File
        get() = File(context.filesDir, "backups").apply { mkdirs() }

    /** Newest first, which is the order both the list and the pruning want. */
    fun listBackups(): List<BackupFile> =
        directory.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            .orEmpty()
            .mapNotNull { file -> readHeader(file) }
            .sortedByDescending { it.createdAt }

    /**
     * One backup per calendar day, taken when the app opens. Tying it to the day rather than to an
     * interval means a day's work is never lost to a phone that was only opened once, and a day
     * spent opening the app twenty times still produces one file.
     */
    suspend fun backupIfDue(): BackupFile? {
        val today = LocalDate.now()
        val alreadyToday = listBackups().any { it.createdAt.toLocalDate() == today }
        return if (alreadyToday) null else backupNow(automatic = true)
    }

    suspend fun backupNow(automatic: Boolean = false): BackupFile? = withContext(Dispatchers.IO) {
        val bookings = bookingRepository.observeAll().first()
        val eventIds = bookings.flatMap {
            listOfNotNull(it.churchCalendarEventId, it.receptionCalendarEventId)
        }
        val events = runCatching { CalendarHelper.readEventsByIds(context, eventIds) }.getOrDefault(emptyMap())

        val now = LocalDateTime.now()
        val root = JSONObject().apply {
            put("formatVersion", FORMAT_VERSION)
            put("createdAt", now.toString())
            put("bookingCount", bookings.size)
            put("automatic", automatic)
            put("bookings", JSONArray().apply { bookings.forEach { put(bookingToJson(it, events)) } })
        }

        val file = File(directory, "jobclosure-${now.format(fileStampFormatter)}.json")
        runCatching { file.writeText(root.toString(2)) }
            .onFailure { return@withContext null }

        prune()
        activityRepository.log(
            action = ActivityAction.BACKUP_CREATED,
            subject = if (automatic) "Αυτόματο αντίγραφο" else "Χειροκίνητο αντίγραφο",
            details = "${bookings.size} δουλειές",
        )
        readHeader(file)
    }

    /**
     * Puts back every job in the file that is not in the app any more. Existing jobs are left
     * exactly as they are: a restore is for filling a hole, and silently overwriting a job the
     * user has since edited would be its own kind of data loss.
     */
    suspend fun restore(file: File): RestoreResult = withContext(Dispatchers.IO) {
        val text = runCatching { file.readText() }.getOrNull()
            ?: return@withContext RestoreResult(restored = 0, alreadyPresent = 0)
        val bookingsJson = runCatching { JSONObject(text).getJSONArray("bookings") }.getOrNull()
            ?: return@withContext RestoreResult(restored = 0, alreadyPresent = 0)

        val existing = bookingRepository.observeAll().first()
        val knownIds = existing.map { it.bookingId }.toSet()
        // Also matched on content, because a job restored from a backup taken on another device -
        // or re-imported from the calendar since - carries a different bookingId.
        val knownContent = existing.map { contentKey(it) }.toSet()

        var restored = 0
        var alreadyPresent = 0
        for (index in 0 until bookingsJson.length()) {
            val booking = runCatching { bookingFromJson(bookingsJson.getJSONObject(index)) }.getOrNull() ?: continue
            if (booking.bookingId in knownIds || contentKey(booking) in knownContent) {
                alreadyPresent++
                continue
            }
            // id = 0 so Room assigns a fresh one; the backup's row id means nothing here.
            bookingRepository.save(booking.copy(id = 0))
            restored++
        }

        activityRepository.log(
            action = ActivityAction.BACKUP_RESTORED,
            subject = file.name,
            details = "Επαναφέρθηκαν $restored · υπήρχαν ήδη $alreadyPresent",
        )
        RestoreResult(restored = restored, alreadyPresent = alreadyPresent)
    }

    private fun prune() {
        listBackups().drop(KEEP_BACKUPS).forEach { runCatching { it.file.delete() } }
    }

    private fun readHeader(file: File): BackupFile? {
        val json = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return null
        val createdAt = runCatching { LocalDateTime.parse(json.getString("createdAt")) }.getOrNull()
            ?: LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault())
        return BackupFile(
            file = file,
            createdAt = createdAt,
            bookingCount = json.optInt("bookingCount", 0),
            sizeBytes = file.length(),
        )
    }

    private fun bookingToJson(booking: Booking, events: Map<Long, CalendarEvent>): JSONObject =
        JSONObject().apply {
            put("bookingId", booking.bookingId)
            put("title", booking.title)
            put("type", booking.type.name)
            put("notes", booking.notes)
            put("clientPhone", booking.clientPhone)
            put("isConfirmed", booking.isConfirmed)
            put("hasDrone", booking.hasDrone)
            put("price", booking.price)
            put("churchName", booking.churchName)
            put("churchAddress", booking.churchAddress)
            put("ceremonyStart", booking.ceremonyStart.toString())
            put("ceremonyDurationMinutes", booking.ceremonyDurationMinutes)
            put("hasReception", booking.hasReception)
            put("receptionVenueName", booking.receptionVenueName)
            put("receptionVenueAddress", booking.receptionVenueAddress)
            put("receptionStart", booking.receptionStart?.toString() ?: JSONObject.NULL)
            put("receptionDurationMinutes", booking.receptionDurationMinutes)
            put("calendarId", booking.calendarId ?: JSONObject.NULL)
            put("churchCalendarEventId", booking.churchCalendarEventId ?: JSONObject.NULL)
            put("receptionCalendarEventId", booking.receptionCalendarEventId ?: JSONObject.NULL)
            put(
                "calendarEvents",
                JSONArray().apply {
                    booking.churchCalendarEventId?.let { id -> events[id]?.let { put(eventToJson("ceremony", it)) } }
                    booking.receptionCalendarEventId?.let { id -> events[id]?.let { put(eventToJson("reception", it)) } }
                },
            )
        }

    private fun eventToJson(role: String, event: CalendarEvent): JSONObject =
        JSONObject().apply {
            put("role", role)
            put("eventId", event.id)
            put("calendarId", event.calendarId)
            put("title", event.title)
            put("description", event.description)
            put("location", event.location)
            put("start", millisToText(event.startMillis))
            put("end", millisToText(event.endMillis))
        }

    private fun bookingFromJson(json: JSONObject): Booking = Booking(
        bookingId = json.optString("bookingId").ifBlank { java.util.UUID.randomUUID().toString() },
        title = json.optString("title"),
        type = runCatching { BookingType.valueOf(json.optString("type")) }.getOrDefault(BookingType.OTHER),
        notes = json.optString("notes"),
        clientPhone = json.optString("clientPhone"),
        isConfirmed = json.optBoolean("isConfirmed", true),
        hasDrone = json.optBoolean("hasDrone", false),
        price = json.optDouble("price", 0.0),
        churchName = json.optString("churchName"),
        churchAddress = json.optString("churchAddress"),
        ceremonyStart = LocalDateTime.parse(json.getString("ceremonyStart")),
        ceremonyDurationMinutes = json.optInt("ceremonyDurationMinutes", 60),
        hasReception = json.optBoolean("hasReception", false),
        receptionVenueName = json.optString("receptionVenueName"),
        receptionVenueAddress = json.optString("receptionVenueAddress"),
        receptionStart = json.optString("receptionStart").takeIf { it.isNotBlank() && it != "null" }
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
        receptionDurationMinutes = json.optInt("receptionDurationMinutes", 240),
        calendarId = json.opt("calendarId")?.takeIf { it != JSONObject.NULL }?.let { (it as Number).toLong() },
        churchCalendarEventId = json.opt("churchCalendarEventId")?.takeIf { it != JSONObject.NULL }
            ?.let { (it as Number).toLong() },
        receptionCalendarEventId = json.opt("receptionCalendarEventId")?.takeIf { it != JSONObject.NULL }
            ?.let { (it as Number).toLong() },
    )

    private fun millisToText(millis: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).toString()

    /** Same job, whatever ids it carries: same minute, same type, same client. */
    private fun contentKey(booking: Booking): String =
        "${Booking.epochMinute(booking.ceremonyStart)}|${booking.type.name}|${booking.title.trim().lowercase()}"
}
