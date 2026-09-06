package ua.safetube.kids.data

import retrofit2.HttpException
import java.io.IOException

/**
 * Перетворює виняток на текст, зрозумілий батькові.
 * Дитина побачить лише велику кнопку «Спробувати ще раз», але дорослому
 * важливо розрізняти «немає Wi-Fi» і «вичерпано квоту API» — лікуються по-різному.
 */
object NetworkErrors {

    fun message(error: Throwable): String = when {
        error is IOException ->
            "Немає зв'язку з інтернетом. Перевірте Wi-Fi."

        error is HttpException && error.code() == 403 ->
            "На сьогодні ліміт запитів до YouTube вичерпано. Спробуйте завтра."

        error is HttpException && error.code() == 404 ->
            "Канал не знайдено. Перевірте посилання в налаштуваннях."

        error is HttpException ->
            "YouTube не відповідає (помилка ${error.code()})."

        else ->
            "Не вдалося завантажити відео."
    }
}
