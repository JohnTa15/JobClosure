package gr.gtar.jobclosure.ui.components

import java.io.File
import java.security.MessageDigest

/**
 * Disk cache for the remote images the booking detail screen shows - venue photos and map previews.
 *
 * Both used to be refetched every single time a booking was opened, which for the Google provider
 * means a billable Places/Static Maps call per glance at a venue whose photo never changes. Entries
 * are keyed by whatever identifies the request (query text, coordinates, provider) and kept in the
 * app's cache directory, so Android is free to reclaim the space under pressure.
 *
 * A lookup that found nothing is cached too, as a zero-length file: "this church has no photo" is
 * just as expensive to rediscover as a photo is to redownload, and it's the more common answer.
 */
object RemoteImageCache {

    private const val DIRECTORY = "remote-images"
    private const val MAX_AGE_MILLIS = 30L * 24 * 60 * 60 * 1000

    /** Null when nothing usable is cached; an empty array when a past lookup found no image. */
    fun load(cacheRoot: File, key: String): ByteArray? {
        val file = fileFor(cacheRoot, key)
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() > MAX_AGE_MILLIS) {
            file.delete()
            return null
        }
        return runCatching { file.readBytes() }.getOrNull()
    }

    /** Pass an empty array to remember that this key has no image. Failures are never stored -
     *  a rejected API call has to be retried once the user fixes it. */
    fun store(cacheRoot: File, key: String, bytes: ByteArray) {
        runCatching {
            val file = fileFor(cacheRoot, key)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
        }
    }

    private fun fileFor(cacheRoot: File, key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return File(File(cacheRoot, DIRECTORY), digest.joinToString("") { "%02x".format(it) })
    }
}
