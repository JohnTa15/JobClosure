package gr.gtar.jobclosure.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubReleaseApi {
    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun getReleaseByTag(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tag") tag: String,
    ): GitHubReleaseResponse

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}

@JsonClass(generateAdapter = true)
data class GitHubReleaseResponse(
    val name: String?,
    @Json(name = "tag_name") val tagName: String,
    val assets: List<GitHubReleaseAsset> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseAsset(
    val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
)
