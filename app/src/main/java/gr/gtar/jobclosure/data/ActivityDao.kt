package gr.gtar.jobclosure.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Query("SELECT * FROM activity_log ORDER BY id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ActivityEntry>>

    @Insert
    suspend fun insert(entry: ActivityEntry)

    /**
     * Keeps the newest [keep] rows and drops the rest. The log is a convenience, not an audit
     * trail, and an unbounded table on a phone eventually costs more than it is worth.
     */
    @Query("DELETE FROM activity_log WHERE id NOT IN (SELECT id FROM activity_log ORDER BY id DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int)

    @Query("DELETE FROM activity_log")
    suspend fun clear()
}
