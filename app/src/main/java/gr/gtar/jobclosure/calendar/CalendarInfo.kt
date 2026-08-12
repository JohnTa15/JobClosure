package gr.gtar.jobclosure.calendar

/** A writable calendar on the device, e.g. a Google Calendar account or Samsung Calendar. */
data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
)

/** A single event read back out of the device calendar, before any interpretation. */
data class CalendarEvent(
    val id: Long,
    val calendarId: Long,
    val title: String,
    val description: String,
    val location: String,
    val startMillis: Long,
    val endMillis: Long,
)
