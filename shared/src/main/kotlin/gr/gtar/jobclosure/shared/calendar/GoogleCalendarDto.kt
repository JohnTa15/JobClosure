package gr.gtar.jobclosure.shared.calendar

import kotlinx.serialization.Serializable

@Serializable
data class GCalEventDateTime(
    val dateTime: String? = null,
    val timeZone: String? = null,
)

@Serializable
data class GCalEvent(
    val id: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val start: GCalEventDateTime? = null,
    val end: GCalEventDateTime? = null,
)

@Serializable
data class GCalEventsListResponse(
    val items: List<GCalEvent> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class GCalCalendarListEntry(
    val id: String,
    val summary: String = "",
    val accessRole: String = "",
)

@Serializable
data class GCalCalendarListResponse(
    val items: List<GCalCalendarListEntry> = emptyList(),
)
