package ua.safetube.kids.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
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

/**
 * Порожній результат живе в кеші лише 10 хвилин, а не добу. Порожнім він буває
 * і тоді, коли канал не вдалося визначити через збій — раніше такий збій
 * зачиняв канал на цілу добу, навіть коли мережа вже працювала.
 */
private const val EMPTY_CACHE_TTL_MS = 10L * 60 * 1000

private val UNSAFE_FILENAME_CHARS = Regex("[^A-Za-z0-9_-]")

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

    /**
     * Ключ кешу = ім'я файлу, тому в ньому не має бути символів, заборонених у
     * назвах файлів. Кирилиця й пробіли перетворюються на «_», через що різні
     * канали могли б отримати один файл — додаємо хеш вихідного рядка.
     *
     * Раніше тут був error() при відсутності всіх ідентифікаторів. Це кидало
     * виняток із loadVideos, яка за задумом не має кидати нічого.
     */
    private fun cacheKey(channel: WhitelistChannel): String {
        val raw = channel.channelId ?: channel.username ?: channel.handle ?: channel.name
        val safe = raw.replace(UNSAFE_FILENAME_CHARS, "_")
        return if (safe == raw) safe else safe + "_" + Integer.toHexString(raw.hashCode())
    }

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

    /**
     * Те саме, але помилка повертається значенням, а не винятком.
     * Виняток усередині LaunchedEffect у Compose не ловиться і закриває застосунок —
     * досить було відпасти Wi-Fi, щоб на планшеті дитини все просто зникло.
     *
     * Якщо свіжих даних дістати не вдалося, віддаємо прострочений кеш, якщо він є:
     * старі відео краще, ніж порожній екран.
     */
    suspend fun loadVideos(channel: WhitelistChannel, forceRefresh: Boolean = false): Result<List<Video>> {
        val key = cacheKey(channel)
        return try {
            Result.success(getVideosForChannel(channel, forceRefresh))
        } catch (cancel: CancellationException) {
            throw cancel   // скасування корутини — не помилка, його треба пропустити далі
        } catch (error: Exception) {
            val stale = withContext(Dispatchers.IO) { readCacheIgnoringAge(key) }
            if (!stale.isNullOrEmpty()) Result.success(stale) else Result.failure(error)
        }
    }

    /**
     * Ідентифікатор плейлиста завантажень каналу.
     *
     * Для каналів із channelId запит до API не потрібен узагалі: плейлист
     * завантажень — це той самий ідентифікатор, де «UC» замінено на «UU».
     * Мінус один мережевий запит і одна одиниця квоти на кожен такий канал —
     * екран каналу відкривається помітно швидше.
     *
     * Для handle/username запит потрібен, але його результат кешується
     * НАЗАВЖДИ: ідентифікатор каналу не змінюється. Раніше він не кешувався
     * взагалі й питався щоразу.
     */
    private suspend fun resolveUploadsPlaylistId(channel: WhitelistChannel): String? {
        channel.channelId?.let { id ->
            if (id.startsWith("UC") && id.length > 2) return "UU" + id.substring(2)
        }

        val key = cacheKey(channel)
        uploadsPlaylistCache()[key]?.let { return it }

        val response = when {
            channel.channelId != null -> api.getChannelById(channelId = channel.channelId, apiKey = apiKey)
            channel.handle != null -> api.getChannelByHandle(handle = "@${channel.handle.removePrefix("@")}", apiKey = apiKey)
            channel.username != null -> api.getChannelByUsername(username = channel.username, apiKey = apiKey)
            else -> return null
        }
        val uploads = response.items.firstOrNull()?.contentDetails?.relatedPlaylists?.uploads
        if (uploads != null) rememberUploadsPlaylist(key, uploads)
        return uploads
    }

    /**
     * Пошук по YouTube із трьома рівнями захисту:
     *
     * 1. safeSearch=strict у самому запиті — відсіює відверте.
     * 2. Офіційна позначка YouTube «створено для дітей» (status.madeForKids).
     *    Головний рівень: її ставить автор або сам YouTube.
     * 3. Наш мовний фільтр — щоб не показувати російськомовне.
     *
     * Плюс відсіюються прямі трансляції (у них немає модерації в момент показу).
     *
     * Ціна: 100 одиниць квоти за пошук + 1 за перевірку відео = 101.
     * З денних 10 000 це близько 99 пошуків на добу — для родини вистачає,
     * але саме тому пошук робиться лише за прямою дією, ніколи у фоні.
     */
    suspend fun search(query: String): Result<List<Video>> {
        if (query.isBlank()) return Result.success(emptyList())
        return try {
            Result.success(withContext(Dispatchers.IO) { searchInternal(query) })
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun searchInternal(query: String): List<Video> {
        val found = api.search(query = query, apiKey = apiKey)
        val ids = found.items.mapNotNull { it.id?.videoId }.distinct()
        if (ids.isEmpty()) return emptyList()

        val details = api.getVideos(commaSeparatedIds = ids.joinToString(","), apiKey = apiKey)

        return details.items.mapNotNull { item ->
            val snippet = item.snippet ?: return@mapNotNull null

            // 1. без прямих трансляцій
            if (snippet.liveBroadcastContent != "none") return@mapNotNull null

            // 2. лише офіційно позначене як дитяче
            val forKids = item.status?.madeForKids ?: item.status?.selfDeclaredMadeForKids ?: false
            if (!forKids) return@mapNotNull null

            // 3. мовний фільтр
            val declaredLanguage = snippet.defaultAudioLanguage ?: snippet.defaultLanguage
            if (LangFilter.shouldHide(declaredLanguage, snippet.title, snippet.description)) {
                return@mapNotNull null
            }

            Video(
                videoId = item.id,
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

    // ---- постійний кеш «канал -> плейлист завантажень» ----

    private val uploadsFile = File(context.filesDir, "uploads_playlists.json")
    private var uploadsCache: MutableMap<String, String>? = null

    private fun uploadsPlaylistCache(): MutableMap<String, String> {
        uploadsCache?.let { return it }
        val loaded: MutableMap<String, String> = try {
            if (uploadsFile.exists()) {
                val type = object : TypeToken<MutableMap<String, String>>() {}.type
                gson.fromJson<MutableMap<String, String>>(uploadsFile.readText(), type) ?: mutableMapOf()
            } else {
                mutableMapOf()
            }
        } catch (e: Exception) {
            mutableMapOf()
        }
        uploadsCache = loaded
        return loaded
    }

    private fun rememberUploadsPlaylist(key: String, playlistId: String) {
        val cache = uploadsPlaylistCache()
        cache[key] = playlistId
        try {
            uploadsFile.writeText(gson.toJson(cache))
        } catch (e: Exception) {
            // не змогли зберегти — не біда, наступного разу спитаємо API знову
        }
    }

    private suspend fun fetchFromNetwork(channel: WhitelistChannel): List<Video> {
        val uploadsPlaylistId = resolveUploadsPlaylistId(channel) ?: return emptyList()

        val items = api.getPlaylistItems(playlistId = uploadsPlaylistId, apiKey = apiKey, maxResults = 25).items

        val videoIds = items.mapNotNull { it.snippet?.resourceId?.videoId }
        if (videoIds.isEmpty()) return emptyList()

        // Другий запит: беремо з нього і liveBroadcastContent (щоб відсіяти прямі
        // трансляції), і мову, яку вказав автор відео. Обидва поля в одній
        // відповіді — мова дістається без жодної додаткової одиниці квоти.
        val detailsByVideoId = api.getVideos(commaSeparatedIds = videoIds.joinToString(","), apiKey = apiKey)
            .items.mapNotNull { item -> item.snippet?.let { item.id to it } }.toMap()

        return items.mapNotNull { item ->
            val snippet = item.snippet ?: return@mapNotNull null
            val videoId = snippet.resourceId?.videoId ?: return@mapNotNull null

            val details = detailsByVideoId[videoId]
            if ((details?.liveBroadcastContent ?: "none") != "none") return@mapNotNull null

            val declaredLanguage = details?.defaultAudioLanguage ?: details?.defaultLanguage
            if (LangFilter.shouldHide(declaredLanguage, snippet.title, snippet.description)) {
                return@mapNotNull null
            }

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
            val entry = gson.fromJson(file.readText(), CacheEntry::class.java) ?: return null
            val ttl = if (entry.videos.isEmpty()) EMPTY_CACHE_TTL_MS else CACHE_TTL_MS
            if (System.currentTimeMillis() - entry.fetchedAt > ttl) null else entry.videos
        } catch (e: Exception) {
            null
        }
    }

    /** Кеш будь-якої давності — запасний варіант, коли мережа недоступна. */
    private fun readCacheIgnoringAge(channelId: String): List<Video>? {
        val file = File(cacheDir, "$channelId.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), CacheEntry::class.java)?.videos
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(channelId: String, videos: List<Video>) {
        val file = File(cacheDir, "$channelId.json")
        file.writeText(gson.toJson(CacheEntry(System.currentTimeMillis(), videos)))
    }
}
