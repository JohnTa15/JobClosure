package gr.gtar.jobclosure.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface GitHubReleaseApi {
    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun getReleaseByTag(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tag") tag: String,
        @Header("Authorization") authorization: String? = null,
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
    /** The api.github.com URL for this specific asset - required (with an Authorization header
     *  and Accept: application/octet-stream) to actually download it from a private repo; the
     *  plain browser_download_url only works unauthenticated, which private repos reject. */
    val url: String,
    /** Final size of the uploaded asset, used to reject a download that came back short. */
    val size: Long = 0,
    /** "sha256:<hex>" once GitHub has finished processing the upload; absent on older releases. */
    val digest: String? = null,
    /** "uploaded" once the asset is complete - anything else means it is still being written. */
    val state: String = "",
)
