package gr.gtar.jobclosure.data

import kotlinx.coroutines.flow.Flow

class BookingRepository(private val dao: BookingDao) {

    fun observeAll(): Flow<List<Booking>> = dao.observeAll()

    suspend fun getById(id: Long): Booking? = dao.getById(id)

    /** Bookings whose busy window overlaps this one's, i.e. would be double-booked. */
    suspend fun findConflicts(booking: Booking): List<Booking> {
        val computed = booking.withComputedOccupiedWindow()
        return dao.findOverlapping(
            start = computed.occupiedStartEpochMinute,
            end = computed.occupiedEndEpochMinute,
            excludeId = booking.id,
        )
    }

    /** Inserts or updates the booking, recomputing its busy window first. Returns the saved id. */
    suspend fun save(booking: Booking): Long {
        val computed = booking.withComputedOccupiedWindow()
        return if (computed.id == 0L) {
            dao.insert(computed)
        } else {
            dao.update(computed)
            computed.id
        }
    }

    suspend fun delete(booking: Booking) = dao.delete(booking)
}
