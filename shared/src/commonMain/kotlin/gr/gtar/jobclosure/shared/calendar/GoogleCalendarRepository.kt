package gr.gtar.jobclosure.shared.calendar

import gr.gtar.jobclosure.shared.model.Booking
import gr.gtar.jobclosure.shared.model.BookingType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds

/**
 * Reads and writes bookings as Google Calendar events, carrying the structured fields a plain
 * calendar event doesn't have (booking type, drone flag, bookingId linking a ceremony/reception
 * pair, etc) as a marked line in the event's description via [BookingMetadataCodec]. This is what
 * makes a booking created on one device (phone or desktop) show up, fully reconstructed, on the
 * other - Google's own calendar sync does the actual data transport, this just knows how to encode
 * and decode a Booking as one or two calendar events.
 */
class GoogleCalendarRepository(
    private val httpClient: HttpClient,
    private val getAccessToken: suspend () -> String,
) {
    private val baseUrl = "https://www.googleapis.com/calendar/v3"

    suspend fun listWritableCalendars(): List<GCalCalendarListEntry> {
        val response: GCalCalendarListResponse = httpClient.get("$baseUrl/users/me/calendarList") {
            bearerAuth(getAccessToken())
        }.body()
        return response.items.filter { it.accessRole == "owner" || it.accessRole == "writer" }
    }

    suspend fun listBookings(calendarId: String): List<Booking> {
        val response: GCalEventsListResponse = httpClient.get(
            "$baseUrl/calendars/${calendarId.encodeURLPathPart()}/events",
        ) {
            bearerAuth(getAccessToken())
            parameter("singleEvents", "true")
            parameter("maxResults", "2500")
            parameter("orderBy", "startTime")
            parameter("timeMin", (Clock.System.now() - (SECONDS_PER_DAY * 30).seconds).toString())
        }.body()

        val jobClosureEvents = response.items.mapNotNull { event ->
            val metadata = BookingMetadataCodec.decodeMetadata(event.description) ?: return@mapNotNull null
            metadata to event
        }

        return jobClosureEvents
            .groupBy { (metadata, _) -> metadata.bookingId }
            .mapNotNull { (_, events) ->
                val ceremony = events.firstOrNull { (metadata, _) -> metadata.role == BookingMetadataCodec.ROLE_CEREMONY }
                    ?: return@mapNotNull null
                val reception = events.firstOrNull { (metadata, _) -> metadata.role == BookingMetadataCodec.ROLE_RECEPTION }
                eventsToBooking(ceremony.second, ceremony.first, reception?.second, reception?.first)
            }
            .sortedBy { it.ceremonyStart }
    }

    suspend fun saveBooking(calendarId: String, booking: Booking): Booking {
        val ceremonyMetadata = BookingMetadata(
            bookingId = booking.bookingId,
            role = BookingMetadataCodec.ROLE_CEREMONY,
            type = booking.type.name,
            hasDrone = booking.hasDrone,
            isConfirmed = booking.isConfirmed,
            title = booking.title,
            venueName = booking.churchName,
            clientPhone = booking.clientPhone,
            price = booking.price,
        )
        val ceremonyEvent = GCalEvent(
            summary = "${booking.type.displayName}: ${booking.title}",
            description = BookingMetadataCodec.encode(booking.notes, ceremonyMetadata),
            location = booking.churchAddress,
            start = booking.ceremonyStart.toGCalDateTime(),
            end = booking.ceremonyEnd.toGCalDateTime(),
        )
        val ceremonyId = if (booking.eventId != null) {
            updateEvent(calendarId, booking.eventId, ceremonyEvent)
            booking.eventId
        } else {
            insertEvent(calendarId, ceremonyEvent).id
                ?: error("Google Calendar did not return an id for the new event")
        }

        val receptionId = if (booking.hasReception && booking.receptionStart != null) {
            val receptionMetadata = BookingMetadata(
                bookingId = booking.bookingId,
                role = BookingMetadataCodec.ROLE_RECEPTION,
                venueName = booking.receptionVenueName,
            )
            val receptionEvent = GCalEvent(
                summary = "Δεξίωση: ${booking.title}",
                description = BookingMetadataCodec.encode("", receptionMetadata),
                location = booking.receptionVenueAddress,
                start = booking.receptionStart.toGCalDateTime(),
                end = (booking.receptionEnd ?: booking.receptionStart).toGCalDateTime(),
            )
            if (booking.receptionEventId != null) {
                updateEvent(calendarId, booking.receptionEventId, receptionEvent)
                booking.receptionEventId
            } else {
                insertEvent(calendarId, receptionEvent).id
            }
        } else {
            booking.receptionEventId?.let { deleteEvent(calendarId, it) }
            null
        }

        return booking.copy(eventId = ceremonyId, receptionEventId = receptionId)
    }

    suspend fun deleteBooking(calendarId: String, booking: Booking) {
        booking.eventId?.let { deleteEvent(calendarId, it) }
        booking.receptionEventId?.let { deleteEvent(calendarId, it) }
    }

    private suspend fun insertEvent(calendarId: String, event: GCalEvent): GCalEvent =
        httpClient.post("$baseUrl/calendars/${calendarId.encodeURLPathPart()}/events") {
            bearerAuth(getAccessToken())
            contentType(ContentType.Application.Json)
            setBody(event)
        }.body()

    private suspend fun updateEvent(calendarId: String, eventId: String, event: GCalEvent) {
        httpClient.patch(
            "$baseUrl/calendars/${calendarId.encodeURLPathPart()}/events/${eventId.encodeURLPathPart()}",
        ) {
            bearerAuth(getAccessToken())
            contentType(ContentType.Application.Json)
            setBody(event)
        }
    }

    private suspend fun deleteEvent(calendarId: String, eventId: String) {
        httpClient.delete(
            "$baseUrl/calendars/${calendarId.encodeURLPathPart()}/events/${eventId.encodeURLPathPart()}",
        ) {
            bearerAuth(getAccessToken())
        }
    }

    private fun eventsToBooking(
        ceremony: GCalEvent,
        ceremonyMetadata: BookingMetadata,
        reception: GCalEvent?,
        receptionMetadata: BookingMetadata?,
    ): Booking? {
        val ceremonyStart = ceremony.start?.dateTime?.let { Instant.parse(it) } ?: return null
        val ceremonyEnd = ceremony.end?.dateTime?.let { Instant.parse(it) } ?: ceremonyStart
        val receptionStart = reception?.start?.dateTime?.let { Instant.parse(it) }
        val receptionEnd = reception?.end?.dateTime?.let { Instant.parse(it) }

        return Booking(
            bookingId = ceremonyMetadata.bookingId,
            eventId = ceremony.id,
            receptionEventId = reception?.id,
            title = ceremonyMetadata.title ?: ceremony.summary ?: "",
            type = ceremonyMetadata.type?.let { runCatching { BookingType.valueOf(it) }.getOrNull() }
                ?: BookingType.OTHER,
            notes = BookingMetadataCodec.userNotes(ceremony.description),
            hasDrone = ceremonyMetadata.hasDrone,
            isConfirmed = ceremonyMetadata.isConfirmed,
            clientPhone = ceremonyMetadata.clientPhone ?: "",
            price = ceremonyMetadata.price ?: 0.0,
            churchName = ceremonyMetadata.venueName ?: "",
            churchAddress = ceremony.location ?: "",
            ceremonyStart = ceremonyStart,
            ceremonyDurationMinutes = minutesBetween(ceremonyStart, ceremonyEnd),
            hasReception = reception != null,
            receptionVenueName = receptionMetadata?.venueName ?: "",
            receptionVenueAddress = reception?.location ?: "",
            receptionStart = receptionStart,
            receptionDurationMinutes = if (receptionStart != null && receptionEnd != null) {
                minutesBetween(receptionStart, receptionEnd)
            } else {
                240
            },
        )
    }

    private fun minutesBetween(start: Instant, end: Instant): Int =
        (end - start).inWholeMinutes.toInt().coerceAtLeast(1)

    private fun Instant.toGCalDateTime(): GCalEventDateTime {
        val zone = TimeZone.currentSystemDefault()
        return GCalEventDateTime(dateTime = "${toLocalDateTime(zone)}${zone.offsetAt(this)}")
    }

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
    }
}
