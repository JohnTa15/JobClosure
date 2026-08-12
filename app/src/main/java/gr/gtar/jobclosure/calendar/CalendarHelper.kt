package gr.gtar.jobclosure.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.TimeZone

/**
 * Wraps the Android Calendar Provider (CalendarContract). This is deliberately used instead of
 * the Google Calendar or Samsung Calendar APIs directly: both of those apps register their
 * accounts as calendars with the system Calendar Provider, so writing an event (with a reminder)
 * through this one API shows up - and fires its 2-hours-before notification - in whichever
 * calendar app the user actually uses, without needing separate integrations for each.
 */
object CalendarHelper {

    fun hasCalendarPermissions(context: Context): Boolean {
        val read = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
        val write = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR)
        return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
    }

    /** Calendars the app can write events into (e.g. a Google account or Samsung Calendar). */
    fun getWritableCalendars(context: Context): List<CalendarInfo> {
        if (!hasCalendarPermissions(context)) return emptyList()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        val result = mutableListOf<CalendarInfo>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            while (cursor.moveToNext()) {
                result += CalendarInfo(
                    id = cursor.getLong(idIdx),
                    displayName = cursor.getString(nameIdx) ?: "",
                    accountName = cursor.getString(accountIdx) ?: "",
                )
            }
        }
        return result
    }

    /**
     * Every event between [fromMillis] and [toMillis], across all readable calendars - including
     * ones this app can't write to, since the sacraments being imported were typed by hand into
     * whatever calendar the user had open at the time.
     */
    fun readEvents(context: Context, fromMillis: Long, toMillis: Long): List<CalendarEvent> {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? " +
            "AND ${CalendarContract.Events.DELETED} = 0"
        val selectionArgs = arrayOf(fromMillis.toString(), toMillis.toString())

        val result = mutableListOf<CalendarEvent>()
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC",
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val calendarIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)
            val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val descriptionIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
            val locationIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
            val startIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            while (cursor.moveToNext()) {
                val start = cursor.getLong(startIdx)
                // Recurring events store no DTEND; an hour is the app's own default ceremony length.
                val end = if (cursor.isNull(endIdx)) start + 60 * 60 * 1000L else cursor.getLong(endIdx)
                result += CalendarEvent(
                    id = cursor.getLong(idIdx),
                    calendarId = cursor.getLong(calendarIdIdx),
                    title = cursor.getString(titleIdx) ?: "",
                    description = cursor.getString(descriptionIdx) ?: "",
                    location = cursor.getString(locationIdx) ?: "",
                    startMillis = start,
                    endMillis = end,
                )
            }
        }
        return result
    }

    /**
     * Inserts an event with a single ALERT reminder [reminderMinutesBefore] minutes before start.
     * Returns the new event's id, or null if calendar permissions are missing or the insert failed.
     */
    fun insertEvent(
        context: Context,
        calendarId: Long,
        title: String,
        location: String,
        description: String,
        startMillis: Long,
        endMillis: Long,
        reminderMinutesBefore: Int,
    ): Long? {
        if (!hasCalendarPermissions(context)) return null

        val eventValues = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }
        val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, eventValues)
            ?: return null
        val eventId = ContentUris.parseId(eventUri)

        val reminderValues = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, reminderMinutesBefore)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)

        return eventId
    }

    /** Updates an already-inserted event's core fields and replaces its reminder. */
    fun updateEvent(
        context: Context,
        eventId: Long,
        title: String,
        location: String,
        description: String,
        startMillis: Long,
        endMillis: Long,
        reminderMinutesBefore: Int,
    ): Boolean {
        if (!hasCalendarPermissions(context)) return false

        val eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val eventValues = ContentValues().apply {
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        val updated = context.contentResolver.update(eventUri, eventValues, null, null)
        if (updated <= 0) return false

        context.contentResolver.delete(
            CalendarContract.Reminders.CONTENT_URI,
            "${CalendarContract.Reminders.EVENT_ID} = ?",
            arrayOf(eventId.toString()),
        )
        val reminderValues = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, reminderMinutesBefore)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        return true
    }

    fun deleteEvent(context: Context, eventId: Long): Boolean {
        if (!hasCalendarPermissions(context)) return false
        val eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return context.contentResolver.delete(eventUri, null, null) > 0
    }

    /**
     * Adds or removes [email] as an attendee on [eventId]. On a Google-synced calendar this is
     * what makes the event show up on the attendee's own calendar too, and is what makes Google
     * Calendar send them an invitation email - there's no separate email-sending step needed.
     */
    fun setAttendee(context: Context, eventId: Long, email: String, present: Boolean) {
        if (!hasCalendarPermissions(context) || email.isBlank()) return

        val selection = "${CalendarContract.Attendees.EVENT_ID} = ? AND ${CalendarContract.Attendees.ATTENDEE_EMAIL} = ?"
        val selectionArgs = arrayOf(eventId.toString(), email)

        val alreadyPresent = context.contentResolver.query(
            CalendarContract.Attendees.CONTENT_URI,
            arrayOf(CalendarContract.Attendees._ID),
            selection,
            selectionArgs,
            null,
        )?.use { it.moveToFirst() } ?: false

        if (present && !alreadyPresent) {
            val values = ContentValues().apply {
                put(CalendarContract.Attendees.EVENT_ID, eventId)
                put(CalendarContract.Attendees.ATTENDEE_EMAIL, email)
                put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP, CalendarContract.Attendees.RELATIONSHIP_ATTENDEE)
                put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_REQUIRED)
                put(CalendarContract.Attendees.ATTENDEE_STATUS, CalendarContract.Attendees.ATTENDEE_STATUS_INVITED)
            }
            context.contentResolver.insert(CalendarContract.Attendees.CONTENT_URI, values)
        } else if (!present && alreadyPresent) {
            context.contentResolver.delete(CalendarContract.Attendees.CONTENT_URI, selection, selectionArgs)
        }
    }
}
