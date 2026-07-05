package ua.safetube.kids.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ua.safetube.kids.BuildConfig
import ua.safetube.kids.filter.LangFilter
import java.io.File
import java.util.concurrent.TimeUnit

private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000 // 1 день

private data class CacheEntry(val fetchedAt: Long, val videos: List<Video>)

/**
 * Тягне відео тільки зі схвалених каналів (uploads-плейлист, дешевше по квоті за search.list),
 * пропускає прямі трансляції й усе, що LangFilter визначив як російськомовне,
 * і кешує результат на добу у filesDir застосунку.
 */
class YouTubeRepository(private val context: Context) {

    private val apiKey = BuildConfig.YOUTUBE_API_KEY

    private val api: YouTubeApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YouTubeApi::class.java)
    }

    private val gson = Gson()
    private val cacheDir = File(context.cacheDir, "channel_videos").apply { mkdirs() }

    private fun cacheKey(channel: WhitelistChannel): String =
        channel.channelId ?: channel.username ?: channel.handle
        ?: error("WhitelistChannel '${channel.name}' не має жодного з channelId/username/handle")

    suspend fun getVideosForChannel(channel: WhitelistChannel, forceRefresh: Boolean = false): List<Video> =
        withContext(Dispatchers.IO) {
            val key = cacheKey(channel)
            if (!forceRefresh) {
                readCache(key)?.let { return@withContext it }
            }
            val fresh = fetchFromNetwork(channel)
            writeCache(key, fresh)
            fresh
        }

    private suspend fun resolveUploadsPlaylistId(channel: WhitelistChannel): String? {
        val response = when {
            channel.channelId != null -> api.getChannelById(channelId = channel.channelId, apiKey = apiKey)
            channel.handle != null -> api.getChannelByHandle(handle = "@${channel.handle.removePrefix("@")}", apiKey = apiKey)
            channel.username != null -> api.getChannelByUsername(username = channel.username, apiKey = apiKey)
            else -> return null
        }
        return response.items.firstOrNull()?.contentDetails?.relatedPlaylists?.uploads
    }

    private suspend fun fetchFromNetwork(channel: WhitelistChannel): List<Video> {
        val uploadsPlaylistId = resolveUploadsPlaylistId(channel) ?: return emptyList()

        val items = api.getPlaylistItems(playlistId = uploadsPlaylistId, apiKey = apiKey, maxResults = 25).items

        val videoIds = items.mapNotNull { it.snippet?.resourceId?.videoId }
        if (videoIds.isEmpty()) return emptyList()

        // Другий запит: liveBroadcastContent, щоб відсіяти прямі трансляції/анонси.
        val liveStatusByVideoId = api.getVideos(commaSeparatedIds = videoIds.joinToString(","), apiKey = apiKey)
            .items.associate { it.id to (it.snippet?.liveBroadcastContent ?: "none") }

        return items.mapNotNull { item ->
            val snippet = item.snippet ?: return@mapNotNull null
            val videoId = snippet.resourceId?.videoId ?: return@mapNotNull null
            if (liveStatusByVideoId[videoId] != "none") return@mapNotNull null
            if (LangFilter.isRussian(snippet.title, snippet.description)) return@mapNotNull null

            Video(
                videoId = videoId,
                title = snippet.title,
                description = snippet.description,
                thumbnailUrl = snippet.thumbnails?.medium?.url
                    ?: snippet.thumbnails?.default?.url.orEmpty(),
                channelId = snippet.channelId,
                channelName = snippet.channelTitle,
                publishedAt = snippet.publishedAt
            )
        }
    }

    private fun readCache(channelId: String): List<Video>? {
        val file = File(cacheDir, "$channelId.json")
        if (!file.exists()) return null
        return try {
            val entry = gson.fromJson(file.readText(), CacheEntry::class.java)
            if (System.currentTimeMillis() - entry.fetchedAt > CACHE_TTL_MS) null else entry.videos
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(channelId: String, videos: List<Video>) {
        val file = File(cacheDir, "$channelId.json")
        file.writeText(gson.toJson(CacheEntry(System.currentTimeMillis(), videos)))
    }
}
