package gr.gtar.jobclosure.shared.model

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookingConflictTest {

    private fun booking(
        startEpochSeconds: Long,
        durationMinutes: Int = 60,
        hasReception: Boolean = false,
        receptionStartEpochSeconds: Long? = null,
        receptionDurationMinutes: Int = 240,
    ) = Booking(
        eventId = "e-$startEpochSeconds",
        title = "Test",
        type = BookingType.WEDDING,
        ceremonyStart = Instant.ofEpochSecond(startEpochSeconds),
        ceremonyDurationMinutes = durationMinutes,
        hasReception = hasReception,
        receptionStart = receptionStartEpochSeconds?.let { Instant.ofEpochSecond(it) },
        receptionDurationMinutes = receptionDurationMinutes,
    )

    @Test
    fun `overlapping ceremonies conflict`() {
        val a = booking(startEpochSeconds = 0, durationMinutes = 60)
        val b = booking(startEpochSeconds = 1800, durationMinutes = 60) // starts 30 min into a's window
        assertTrue(a.conflictsWith(b))
        assertEquals(1, findConflicts(b, listOf(a)).size)
    }

    @Test
    fun `back to back ceremonies do not conflict`() {
        val a = booking(startEpochSeconds = 0, durationMinutes = 60)
        val b = booking(startEpochSeconds = 3600, durationMinutes = 60) // starts exactly when a ends
        assertTrue(!a.conflictsWith(b))
        assertEquals(0, findConflicts(b, listOf(a)).size)
    }

    @Test
    fun `reception extends the busy window`() {
        val a = booking(
            startEpochSeconds = 0,
            durationMinutes = 60,
            hasReception = true,
            receptionStartEpochSeconds = 3600,
            receptionDurationMinutes = 240,
        )
        // Someone else booked 2 hours after the ceremony started - inside a's reception window.
        val b = booking(startEpochSeconds = 7200, durationMinutes = 60)
        assertTrue(a.conflictsWith(b))
    }

    @Test
    fun `a booking never conflicts with itself`() {
        val a = booking(startEpochSeconds = 0, durationMinutes = 60)
        assertEquals(0, findConflicts(a, listOf(a)).size)
    }
}
