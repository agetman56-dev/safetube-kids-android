package ua.safetube.kids.data

/**
 * Розбирає посилання, яке батько вставляє в налаштуваннях (наприклад, скопійоване
 * з адресного рядка на youtube.com), у потрібне поле WhitelistChannel.
 * Підтримує: /channel/UC..., /user/Ім'я, /@handle, а також голий "@handle" чи "UC..." текст.
 */
object ChannelUrlParser {

    fun parse(name: String, input: String): WhitelistChannel? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        Regex("youtube\\.com/channel/(UC[\\w-]+)").find(trimmed)?.let {
            return WhitelistChannel(name = name, channelId = it.groupValues[1])
        }
        Regex("youtube\\.com/user/([\\w.-]+)").find(trimmed)?.let {
            return WhitelistChannel(name = name, username = it.groupValues[1])
        }
        Regex("youtube\\.com/@([\\w.-]+)").find(trimmed)?.let {
            return WhitelistChannel(name = name, handle = it.groupValues[1])
        }
        if (trimmed.startsWith("UC") && trimmed.length in 20..30 && !trimmed.contains("/")) {
            return WhitelistChannel(name = name, channelId = trimmed)
        }
        if (trimmed.startsWith("@")) {
            return WhitelistChannel(name = name, handle = trimmed.removePrefix("@"))
        }
        // Голий текст без розпізнаного формату — пробуємо як @handle (найчастіший сучасний варіант).
        return WhitelistChannel(name = name, handle = trimmed)
    }
}
