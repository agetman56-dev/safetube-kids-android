package ua.safetube.kids.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Головна вимога до фільтра в цьому застосунку: НЕ ховати український контент.
 * Канали вже схвалені батьком, тому приховати відео зі схваленого каналу —
 * чиста втрата, а пропустити сумнівне — дрібниця.
 *
 * Попередня версія фільтра провалювала 10 із 23 назв нижче.
 */
class LangFilterTest {

    /** Типові назви з каналів білого списку: Казкарик, Патрон, Малятко ТВ, HeyKids. */
    private val ukrainianKidsTitles = listOf(
        "Казка про кота у чоботях",
        "Мультик про машинки для малюків",
        "Колискова для малят",
        "Вчимо кольори з Гавчиком",
        "Пригоди Патрона",
        "Рахуємо до десяти",
        "Пісня про маму",
        "Веселі пісеньки для дітей",
        "Абетка для дошкільнят",
        "Домашні улюбленці",
        "Танцюємо разом",
        "Про що мріють діти",
        "Три поросятка",
        "Червона шапочка",
        "Як зробити годівницю",
        "Хто живе у лісі",
        "Смачна каша для Каю",
        "Новий друг у школі",
        "Дощик, дощик, крапельки",
        "Наша улюблена гра",
        "Зимова казка",
        "Пори року для малюків",
        "Транспорт: машинки та трактори"
    )

    private val englishKidsTitles = listOf(
        "Baby Shark Dance",
        "Wheels on the Bus",
        "Peppa Pig Full Episodes",
        "ABC Song for Children"
    )

    private val russianKidsTitles = listOf(
        "Колыбельная для малышей",
        "Учим цвета с машинками",
        "Сказка про кота в сапогах",
        "Развивающие мультфильмы для детей",
        "Песенка про алфавит"
    )

    @Test
    fun `український дитячий контент не ховається`() {
        val hidden = ukrainianKidsTitles.filter { LangFilter.shouldHide(null, it, "") }
        assertTrue("Помилково приховано: $hidden", hidden.isEmpty())
    }

    @Test
    fun `англомовний контент не ховається`() {
        val hidden = englishKidsTitles.filter { LangFilter.shouldHide(null, it, "") }
        assertTrue("Помилково приховано: $hidden", hidden.isEmpty())
    }

    @Test
    fun `український контент не ховається і в суворому режимі`() {
        val hidden = ukrainianKidsTitles.filter { LangFilter.shouldHide(null, it, "", strict = true) }
        // У суворому режимі короткі нейтральні назви («Червона шапочка») чесно
        // потрапляють під «не визначено» — це очікувано. Вимога м'якша за
        // основний тест: не більше третини набору.
        assertTrue(
            "У суворому режимі приховано забагато (${hidden.size} з ${ukrainianKidsTitles.size}): $hidden",
            hidden.size <= ukrainianKidsTitles.size / 3
        )
    }

    @Test
    fun `російський дитячий контент здебільшого ховається`() {
        val shown = russianKidsTitles.filter { !LangFilter.shouldHide(null, it, "") }
        assertTrue(
            "Пропущено забагато російського (${shown.size} з ${russianKidsTitles.size}): $shown",
            shown.size <= 2
        )
    }

    // ---- мова, вказана автором відео, має пріоритет над текстом ----

    @Test
    fun `вказана українська мова перекриває текстовий аналіз`() {
        assertFalse(LangFilter.shouldHide("uk", "Колыбельная для малышей", ""))
        assertFalse(LangFilter.shouldHide("uk-UA", "Мультики для детей", ""))
    }

    @Test
    fun `вказана російська мова ховає навіть за української назви`() {
        assertTrue(LangFilter.shouldHide("ru", "Казка про кота", ""))
        assertTrue(LangFilter.shouldHide("ru-RU", "Пісенька для малюків", ""))
    }

    @Test
    fun `інші мови не ховаємо`() {
        assertFalse(LangFilter.shouldHide("en", "Baby Shark Dance", ""))
        assertFalse(LangFilter.shouldHide("pl", "Kolorowe piosenki", ""))
    }

    @Test
    fun `порожня вказана мова не заважає текстовому аналізу`() {
        assertTrue(LangFilter.shouldHide("", "Колыбельная для малышей", ""))
        assertTrue(LangFilter.shouldHide(null, "Колыбельная для малышей", ""))
    }

    // ---- окремі перевірки самого детектора ----

    @Test
    fun `унікальні літери визначають мову напевно`() {
        assertEquals(LangFilter.Lang.RU, LangFilter.detectLang("Мультики для детей ёлка"))
        assertEquals(LangFilter.Lang.UA, LangFilter.detectLang("Українські пісні для дітей"))
    }

    @Test
    fun `текст без кирилиці — OTHER`() {
        assertEquals(LangFilter.Lang.OTHER, LangFilter.detectLang("Baby Shark Dance"))
        assertEquals(LangFilter.Lang.OTHER, LangFilter.detectLang(""))
    }

    /**
     * Регресія: у попередній версії стояло Regex("ии|ие|ое\\b|ый\\b"), і межа
     * слова \b з кирилицею не працює — ці правила не спрацьовували жодного разу.
     * Тут перевіряємо, що закінчення справді розпізнаються.
     */
    @Test
    fun `російські закінчення розпізнаються попри особливості word boundary`() {
        assertEquals(LangFilter.Lang.RU, LangFilter.detectLang("Большое красная задание"))
    }

    @Test
    fun `українські закінчення розпізнаються`() {
        assertEquals(LangFilter.Lang.UA, LangFilter.detectLang("прибирання робимо разом"))
    }
}
