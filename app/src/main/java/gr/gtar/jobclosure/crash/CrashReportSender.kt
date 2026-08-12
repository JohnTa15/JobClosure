package gr.gtar.jobclosure.crash

import gr.gtar.jobclosure.network.GitHubIssuesApi
import gr.gtar.jobclosure.network.NewIssueRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

sealed interface CrashSendResult {
    data class Sent(val url: String) : CrashSendResult
    data object AlreadyReported : CrashSendResult
    data class Failed(val message: String) : CrashSendResult
}

/**
 * Files a crash report as a GitHub issue on the project repository.
 *
 * The token this reuses is the one Settings already asks for so the updater can read release
 * assets from the private repo, and a token scoped to just that will be refused here - opening
 * issues needs its own permission. That refusal is reported in those words rather than as a bare
 * 403, since it's a setting the user can fix in a minute and would otherwise look like a bug.
 */
class CrashReportSender(private val api: GitHubIssuesApi) {

    suspend fun send(report: CrashReport, gitHubToken: String): CrashSendResult =
        withContext(Dispatchers.IO) {
            if (gitHubToken.isBlank()) {
                return@withContext CrashSendResult.Failed(
                    "Χρειάζεται GitHub token στις Ρυθμίσεις για να σταλεί η αναφορά.",
                )
            }

            val auth = "Bearer $gitHubToken"
            val title = "Crash: ${report.summary.take(TITLE_LIMIT)}"

            try {
                // Repeated crashes of the same kind are the norm, and a fresh issue for each one
                // would bury the useful ones.
                val existing = api.searchIssues(
                    query = "repo:$OWNER/$REPO is:issue is:open in:title \"${report.summary.take(TITLE_LIMIT)}\"",
                    authorization = auth,
                )
                if (existing.totalCount > 0) return@withContext CrashSendResult.AlreadyReported
            } catch (e: Exception) {
                // A failed duplicate check shouldn't stop the report from being filed.
            }

            try {
                val created = api.createIssue(
                    owner = OWNER,
                    repo = REPO,
                    authorization = auth,
                    issue = NewIssueRequest(title = title, body = body(report)),
                )
                CrashSendResult.Sent(created.htmlUrl)
            } catch (e: HttpException) {
                CrashSendResult.Failed(
                    when (e.code()) {
                        401 -> "Το GitHub token απορρίφθηκε - έλεγξέ το στις Ρυθμίσεις."
                        403, 404 -> "Το GitHub token δεν έχει δικαίωμα να ανοίγει issues. " +
                            "Χρειάζεται 'Issues: Read and write' για αυτό το repository."
                        else -> "Αποτυχία αποστολής: HTTP ${e.code()}"
                    },
                )
            } catch (e: Exception) {
                CrashSendResult.Failed("Αποτυχία αποστολής: ${e.message ?: "άγνωστο σφάλμα"}")
            }
        }

    private fun body(report: CrashReport): String = buildString {
        appendLine("Αυτόματη αναφορά σφάλματος από την εφαρμογή.")
        appendLine()
        appendLine("```")
        append(report.text.take(BODY_LIMIT))
        appendLine()
        appendLine("```")
    }

    private companion object {
        const val OWNER = "JohnTa15"
        const val REPO = "JobClosure"
        const val TITLE_LIMIT = 120
        const val BODY_LIMIT = 30_000
    }
}
