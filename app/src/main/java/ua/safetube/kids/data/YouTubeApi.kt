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

data class VideosListResponse(val items: List<VideoItem> = emptyList())
data class VideoItem(val id: String, val snippet: VideoSnippet?)
data class VideoSnippet(
    val liveBroadcastContent: String = "none"
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

    @GET("videos")
    suspend fun getVideos(
        @Query("id") commaSeparatedIds: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet"
    ): VideosListResponse
}
