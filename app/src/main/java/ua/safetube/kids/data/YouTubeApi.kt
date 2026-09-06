package ua.safetube.kids.data

import retrofit2.http.GET
import retrofit2.http.Query

// ---- Відповіді YouTube Data API v3 (лише поля, що нам потрібні) ----

data class ChannelsListResponse(val items: List<ChannelItem> = emptyList())
data class ChannelItem(val id: String, val contentDetails: ChannelContentDetails?)
data class ChannelContentDetails(val relatedPlaylists: RelatedPlaylists?)
data class RelatedPlaylists(val uploads: String?)

data class PlaylistItemsResponse(
    val items: List<PlaylistItem> = emptyList(),
    val nextPageToken: String? = null
)
data class PlaylistItem(val snippet: PlaylistItemSnippet?)
data class PlaylistItemSnippet(
    val title: String = "",
    val description: String = "",
    val channelId: String = "",
    val channelTitle: String = "",
    val publishedAt: String = "",
    val resourceId: ResourceId?,
    val thumbnails: Thumbnails?
)
data class ResourceId(val videoId: String?)

// ---- пошук ----

data class SearchListResponse(val items: List<SearchItem> = emptyList())
data class SearchItem(val id: SearchItemId?)
data class SearchItemId(val videoId: String?)

data class VideosListResponse(val items: List<VideoItem> = emptyList())
data class VideoItem(val id: String, val snippet: VideoSnippet?, val status: VideoStatus?)

/**
 * madeForKids — офіційна позначка YouTube «створено для дітей». Її ставить
 * автор або сам YouTube, і вона куди надійніша за будь-яку нашу евристику.
 * Це головний рівень захисту пошуку.
 */
data class VideoStatus(
    val madeForKids: Boolean? = null,
    val selfDeclaredMadeForKids: Boolean? = null
)

/**
 * defaultAudioLanguage і defaultLanguage автор вказує сам при завантаженні відео.
 * Це найточніший сигнал про мову — точніший за будь-який аналіз назви. Обидва поля
 * приходять у ТІЙ САМІЙ відповіді videos.list, яку ми вже робимо заради
 * liveBroadcastContent, тобто дістаються безкоштовно, без додаткової квоти.
 * Заповнюють їх не всі автори, тому це не заміна текстовому фільтру, а перший рубіж.
 */
data class VideoSnippet(
    val liveBroadcastContent: String = "none",
    val defaultAudioLanguage: String? = null,
    val defaultLanguage: String? = null,
    // Потрібні для пошуку: там ці дані беруться саме звідси, а не з playlistItems
    val title: String = "",
    val description: String = "",
    val channelId: String = "",
    val channelTitle: String = "",
    val publishedAt: String = "",
    val thumbnails: Thumbnails? = null
)

data class Thumbnails(val medium: Thumbnail?, val high: Thumbnail?, val default: Thumbnail?)
data class Thumbnail(val url: String?)

interface YouTubeApi {

    @GET("channels")
    suspend fun getChannelById(
        @Query("id") channelId: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "contentDetails"
    ): ChannelsListResponse

    @GET("channels")
    suspend fun getChannelByUsername(
        @Query("forUsername") username: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "contentDetails"
    ): ChannelsListResponse

    @GET("channels")
    suspend fun getChannelByHandle(
        @Query("forHandle") handle: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "contentDetails"
    ): ChannelsListResponse

    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("playlistId") playlistId: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet",
        @Query("maxResults") maxResults: Int = 25,
        @Query("pageToken") pageToken: String? = null
    ): PlaylistItemsResponse

    /**
     * part=snippet,status — обидві частини в одному запиті. У YouTube Data API v3
     * вартість рахується за методом, а не за кількістю частин, тому status
     * (з ним і madeForKids) дістається без додаткової квоти.
     */
    @GET("videos")
    suspend fun getVideos(
        @Query("id") commaSeparatedIds: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet,status"
    ): VideosListResponse

    /**
     * Пошук коштує 100 одиниць квоти (проти 1 за playlistItems), тому
     * викликається лише за прямою дією користувача й ніколи у фоні.
     * safeSearch=strict — перший рівень захисту, найгрубіший.
     */
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "id",
        @Query("type") type: String = "video",
        @Query("safeSearch") safeSearch: String = "strict",
        @Query("maxResults") maxResults: Int = 40,
        @Query("relevanceLanguage") relevanceLanguage: String = "uk",
        @Query("videoEmbeddable") videoEmbeddable: String = "true"
    ): SearchListResponse
}
