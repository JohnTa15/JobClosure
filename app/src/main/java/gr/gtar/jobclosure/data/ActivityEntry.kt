package gr.gtar.jobclosure.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * What kind of thing happened. The label is what the history screen shows, so these read as a
 * person's actions ("Νέα δουλειά") rather than as database operations.
 */
enum class ActivityAction(val label: String) {
    BOOKING_CREATED("Νέα δουλειά"),
    BOOKING_UPDATED("Αλλαγή δουλειάς"),
    BOOKING_DELETED("Διαγραφή δουλειάς"),
    CALENDAR_IMPORT("Εισαγωγή από ημερολόγιο"),
    DUPLICATES_REMOVED("Καθαρισμός διπλών"),
    BACKUP_CREATED("Αντίγραφο ασφαλείας"),
    BACKUP_RESTORED("Επαναφορά αντιγράφου"),
}

/**
 * One line of the app's own history. Written after the change it describes has gone through, so
 * the log never claims something that did not happen.
 */
@Entity(tableName = "activity_log")
data class ActivityEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampEpochMillis: Long = System.currentTimeMillis(),
    val action: ActivityAction,
    /** Which job (or file) this was about - the headline of the row. */
    val subject: String = "",
    /** Anything worth keeping beside it: the date of the job, how many rows were touched. */
    val details: String = "",
)
