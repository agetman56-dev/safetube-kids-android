package ua.safetube.kids

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import ua.safetube.kids.data.WhitelistCategory
import ua.safetube.kids.data.WhitelistRepository
import ua.safetube.kids.data.YouTubeRepository
import ua.safetube.kids.parental.ParentalControls

/**
 * Спільний стан застосунку: одна копія на весь час життя MainActivity.
 * Екран орієнтації зафіксовано на landscape (див. AndroidManifest), тому
 * пересоздання при повороті екрана не відбувається — тримати це у простому
 * remember{}, без ViewModel, для цього застосунку достатньо.
 */
class AppState(context: Context) {
    val whitelistRepo = WhitelistRepository(context)
    val youtubeRepo = YouTubeRepository(context)
    val parental = ParentalControls(context)

    val categories = mutableStateOf<List<WhitelistCategory>>(emptyList())

    fun reloadCategories() {
        categories.value = whitelistRepo.load()
    }
}
