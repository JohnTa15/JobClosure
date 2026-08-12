package gr.gtar.jobclosure.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Opens crash reports as issues on the project's own repository, reusing the GitHub token the
 *  updater already needs - no second service to sign up for or configure. */
interface GitHubIssuesApi {

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") authorization: String,
        @Body issue: NewIssueRequest,
    ): CreatedIssueResponse

    /** Used to avoid filing the same crash twice: searches open issues for an existing report. */
    @GET("search/issues")
    suspend fun searchIssues(
        @Query("q") query: String,
        @Header("Authorization") authorization: String,
    ): IssueSearchResponse
}

@JsonClass(generateAdapter = true)
data class NewIssueRequest(
    val title: String,
    val body: String,
    val labels: List<String> = listOf("crash"),
)

@JsonClass(generateAdapter = true)
data class CreatedIssueResponse(
    val number: Int = 0,
    @com.squareup.moshi.Json(name = "html_url") val htmlUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class IssueSearchResponse(
    @com.squareup.moshi.Json(name = "total_count") val totalCount: Int = 0,
)
