package gr.gtar.jobclosure.calendar

/** A writable calendar on the device, e.g. a Google Calendar account or Samsung Calendar. */
data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
)
