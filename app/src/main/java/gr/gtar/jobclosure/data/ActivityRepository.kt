package gr.gtar.jobclosure.data

import kotlinx.coroutines.flow.Flow

/** How many entries the history keeps and shows. Months of use fit comfortably. */
private const val MAX_ENTRIES = 500

class ActivityRepository(private val dao: ActivityDao) {

    fun observeRecent(): Flow<List<ActivityEntry>> = dao.observeRecent(MAX_ENTRIES)

    /**
     * Records something the user did. Failures are swallowed on purpose: the history is a record
     * of the app's work, and it must never be the reason a save or an import reports an error.
     */
    suspend fun log(action: ActivityAction, subject: String = "", details: String = "") {
        runCatching {
            dao.insert(ActivityEntry(action = action, subject = subject, details = details))
            dao.trimTo(MAX_ENTRIES)
        }
    }

    suspend fun clear() {
        runCatching { dao.clear() }
    }
}
