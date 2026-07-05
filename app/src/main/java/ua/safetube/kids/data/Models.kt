package ua.safetube.kids.data

/** Одна категорія в застосунку (Мультфільми / Розвиток / Музика). */
data class WhitelistCategory(
    val id: String,
    val title: String,
    val channels: List<WhitelistChannel>
)

/**
 * Один схвалений батьком канал. YouTube віддає канали за різними ідентифікаторами
 * залежно від того, коли їх створили — заповнюємо рівно те поле, яке видно в URL:
 * youtube.com/channel/UC...      -> channelId
 * youtube.com/user/Ім'я          -> username
 * youtube.com/@Ім'я              -> handle (без "@")
 */
data class WhitelistChannel(
    val name: String,
    val channelId: String? = null,
    val username: String? = null,
    val handle: String? = null
)

/** Відео, готове до показу дитині (уже пройшло LangFilter). */
data class Video(
    val videoId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelId: String,
    val channelName: String,
    val publishedAt: String
)
