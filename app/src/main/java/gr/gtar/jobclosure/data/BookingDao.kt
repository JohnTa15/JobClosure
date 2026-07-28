package gr.gtar.jobclosure.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {

    @Query("SELECT * FROM bookings ORDER BY occupiedStartEpochMinute ASC")
    fun observeAll(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getById(id: Long): Booking?

    @Insert
    suspend fun insert(booking: Booking): Long

    @Update
    suspend fun update(booking: Booking)

    @Delete
    suspend fun delete(booking: Booking)

    /**
     * Any existing booking whose busy window overlaps [start, end), excluding [excludeId]
     * (used when editing a booking so it doesn't conflict with itself).
     */
    @Query(
        """
        SELECT * FROM bookings
        WHERE occupiedStartEpochMinute < :end
          AND occupiedEndEpochMinute > :start
          AND id != :excludeId
        ORDER BY occupiedStartEpochMinute ASC
        """
    )
    suspend fun findOverlapping(start: Long, end: Long, excludeId: Long = 0): List<Booking>
}
