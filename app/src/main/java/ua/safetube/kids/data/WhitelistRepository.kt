package ua.safetube.kids.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File

private data class WhitelistFile(
    @SerializedName("categories") val categories: List<WhitelistCategory> = emptyList()
)

/**
 * Список схвалених батьком каналів. Стартовий каталог лежить у assets/whitelist.json
 * (наповнюється при збірці). Якщо в застосунку через екран налаштувань додали/прибрали
 * канал — зберігається копія у внутрішньому сховищі (filesDir), яка має пріоритет.
 */
class WhitelistRepository(private val context: Context) {

    private val gson = Gson()
    private val overrideFile = File(context.filesDir, "whitelist_override.json")

    fun load(): List<WhitelistCategory> {
        val json = if (overrideFile.exists()) {
            overrideFile.readText()
        } else {
            context.assets.open("whitelist.json").bufferedReader().use { it.readText() }
        }
        return gson.fromJson(json, WhitelistFile::class.java).categories
    }

    fun save(categories: List<WhitelistCategory>) {
        val json = gson.toJson(WhitelistFile(categories))
        overrideFile.writeText(json)
    }
}
