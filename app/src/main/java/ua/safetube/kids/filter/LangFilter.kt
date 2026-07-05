package ua.safetube.kids.filter

/**
 * Kotlin-порт мовного детектора з SafeTube UA (розширення Chrome, content.js:detectLang).
 * Другий рубіж захисту: перевіряє назву/опис відео навіть у схваленого каналу,
 * бо канал у білому списку інколи публікує щось не тим мовою.
 */
object LangFilter {

    // Літери, яких немає в українській / немає в російській
    private val RU_LETTERS = Regex("[ыъэё]", RegexOption.IGNORE_CASE)
    private val UA_LETTERS = Regex("[іїєґ]", RegexOption.IGNORE_CASE)

    // Слова, що існують лише в одній із мов (нижній регістр) — той самий список, що в content.js
    private val RU_WORDS = setOf(
        "что", "как", "это", "этот", "очень", "сегодня", "сейчас", "если", "когда", "здесь", "есть",
        "был", "была", "было", "новый", "самый", "лучший", "обзор", "выпуск", "серия", "игра",
        "играем", "играю", "прохождение", "против", "человек", "сделать", "делаем", "можно", "нужно",
        "нельзя", "который", "еще", "или", "меня", "тебя", "смотрим", "смотреть", "русский", "россия",
        "росия", "деньги", "строим", "строю", "всё", "встреча", "прямой", "война", "военные", "русские",
        "российский", "российские", "путин", "путина", "говорит", "сказал", "сказала", "будет", "может",
        "хочет", "после", "перед", "почему", "теперь", "потом", "вместе", "больше", "меньше", "новости",
        "итоги", "главное", "последние", "нет", "русское", "русских", "русским", "русской", "кино",
        "сериал", "сериалы", "фильм", "фильмы", "время", "никто", "любовь", "любви", "серіал", "берущий",
        "узнает", "эфир", "прямом", "полностью", "выпуски", "премьера", "комедия", "мелодрама"
    )
    private val RU_PATTERN = Regex("ии|ие|ое\\b|ый\\b")

    private val UA_WORDS = setOf(
        "що", "як", "це", "цей", "дуже", "сьогодні", "зараз", "якщо", "коли", "тут", "був", "була",
        "було", "новий", "найкращий", "огляд", "випуск", "серія", "гра", "граємо", "граю", "проходження",
        "проти", "людина", "зробити", "робимо", "можна", "потрібно", "треба", "який", "ще", "або", "мене",
        "тебе", "дивимось", "дивитися", "українська", "україна", "гроші", "будуємо", "зеленський", "заява",
        "новини", "війна", "війни", "зробив", "зробила", "буде", "може", "після", "перед", "разом",
        "більше", "менше", "підсумки", "головне", "останні", "українські", "зсу", "всу", "ворог",
        "ворога", "перемога", "місто", "нього", "наш", "наша", "наші"
    )

    private val WORD_SPLIT = Regex("[^а-яёіїєґ']+", RegexOption.IGNORE_CASE)
    private val CYR_LETTER = Regex("[а-яёіїєґ]", RegexOption.IGNORE_CASE)

    private fun countWords(text: String, list: Set<String>): Int =
        text.lowercase().split(WORD_SPLIT).count { it in list }

    private fun cyrWordCount(text: String): Int =
        text.split(WORD_SPLIT).count { CYR_LETTER.containsMatchIn(it) }

    enum class Lang { UA, RU, OTHER }

    /**
     * aggressive = true (дитячий режим): кирилиця без укр. літер і без розпізнаних слів
     * все одно вважається російською, якщо кириличних слів ≥ 2 — краще пропустити
     * сумнівне відео, ніж показати дитині щось російськомовне.
     */
    fun detectLang(text: String, aggressive: Boolean = true): Lang {
        if (UA_LETTERS.containsMatchIn(text)) return Lang.UA
        if (RU_LETTERS.containsMatchIn(text)) return Lang.RU

        val ru = countWords(text, RU_WORDS) + if (RU_PATTERN.containsMatchIn(text.lowercase())) 1 else 0
        val ua = countWords(text, UA_WORDS)
        if (ua > 0 && ua >= ru) return Lang.UA
        if (ru > 0) return Lang.RU
        if (aggressive && cyrWordCount(text) >= 2) return Lang.RU
        return Lang.OTHER
    }

    /** true, якщо відео варто приховати від дитини (мова — російська). */
    fun isRussian(title: String, description: String): Boolean =
        detectLang("$title $description") == Lang.RU
}
