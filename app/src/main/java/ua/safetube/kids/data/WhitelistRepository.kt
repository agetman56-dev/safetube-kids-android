package ua.safetube.kids.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class WhitelistFile(
    @SerializedName("categories") val categories: List<WhitelistCategory> = emptyList()
)

/**
 * Список схвалених батьком каналів. Стартовий каталог лежить у assets/whitelist.json
 * (наповнюється при збірці). Якщо в застосунку через екран налаштувань додали/прибрали
 * канал — зберігається копія у внутрішньому сховищі (filesDir), яка має пріоритет.
 *
 * Усі операції — suspend і виконуються в Dispatchers.IO: раніше читання й запис
 * файлу йшли з головного потоку (з onClick та з реколбека Compose), що давало
 * ривки, а на повільному пристрої могло призвести до ANR.
 */
class WhitelistRepository(private val context: Context) {

    private val gson = Gson()
    private val overrideFile = File(context.filesDir, "whitelist_override.json")

    /** Чи батько вже змінював список у застосунку. */
    suspend fun hasOverride(): Boolean = withContext(Dispatchers.IO) { overrideFile.exists() }

    suspend fun load(): List<WhitelistCategory> = withContext(Dispatchers.IO) {
        // Спершу власний список батька, потім — стартовий з assets.
        // Пошкоджений файл не має валити застосунок, тому кожен крок окремо.
        readOverride() ?: readBundled() ?: emptyList()
    }

    suspend fun save(categories: List<WhitelistCategory>) = withContext(Dispatchers.IO) {
        overrideFile.writeText(gson.toJson(WhitelistFile(categories)))
    }

    /**
     * Повернути стартовий каталог. Раніше цього не було: щойно з'являвся
     * whitelist_override.json, файл з assets більше не читався ніколи — батько,
     * який випадково видалив усі канали, лишався з порожнім застосунком.
     */
    suspend fun resetToBundled() = withContext(Dispatchers.IO) {
        if (overrideFile.exists()) overrideFile.delete()
        Unit
    }

    private fun readOverride(): List<WhitelistCategory>? = try {
        if (overrideFile.exists()) parse(overrideFile.readText()) else null
    } catch (e: Exception) {
        null
    }

    private fun readBundled(): List<WhitelistCategory>? = try {
        parse(context.assets.open("whitelist.json").bufferedReader().use { it.readText() })
    } catch (e: Exception) {
        null
    }

    private fun parse(json: String): List<WhitelistCategory>? =
        gson.fromJson(json, WhitelistFile::class.java)?.categories
}
