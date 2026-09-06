package ua.safetube.kids.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import ua.safetube.kids.AppState

/** Крок лічильника часу перегляду. Менший крок — точніше, але частіші записи. */
private const val TICK_MS = 30_000L

/**
 * Офіційний YouTube IFrame Player у WebView — не власний плеєр, тому лишається сумісним
 * з умовами використання YouTube. rel=0 не показує чужі відео в кінці, лише з того ж каналу.
 * shouldOverrideUrlLoading блокує будь-яку спробу вивести дитину за межі вбудованого плеєра.
 */
@Composable
fun PlayerScreen(appState: AppState, videoId: String, onBack: () -> Unit) {
    var limitReached by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(false) }

    LaunchedEffect(videoId) {
        val enabled = appState.parental.timeLimitEnabled.first()
        if (enabled) {
            val limitMinutes = appState.parental.timeLimitMinutes.first()
            val watchedSeconds = appState.parental.watchedSecondsToday.first()
            limitReached = watchedSeconds >= limitMinutes * 60
        }
        checked = true
    }

    if (!checked) return

    if (limitReached) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("На сьогодні відео закінчились. Побачимось завтра! 🌙")
        }
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
        }
        return
    }

    // Лічильник часу перегляду.
    //
    // Раніше він крутився, поки екран був у композиції — тобто рахував і тоді,
    // коли застосунок згорнули чи заблокували планшет. Денний ліміт вигорав
    // намарно. Тепер лічильник живе лише у стані RESUMED: згорнули — пауза.
    //
    // Друга зміна: ліміт перевіряється НА ХОДУ, а не лише при відкритті відео.
    // Раніше дитина на 59-й хвилині могла відкрити 40-хвилинний мультик
    // і додивитися його повністю.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(videoId) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(TICK_MS)
                appState.parental.addWatchedSeconds((TICK_MS / 1000).toInt())

                if (appState.parental.timeLimitEnabled.first()) {
                    val limitMinutes = appState.parental.timeLimitMinutes.first()
                    val watchedSeconds = appState.parental.watchedSecondsToday.first()
                    if (watchedSeconds >= limitMinutes * 60) {
                        limitReached = true   // екран сам перемкнеться, WebView знищиться
                        break
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        EmbeddedYouTubePlayer(videoId = videoId)
        Box(modifier = Modifier.padding(12.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
        }
    }
}

// Реальна HTTPS-сторінка на GitHub Pages (окремий репозиторій safetube-kids-player-page),
// що вантажить офіційний YouTube IFrame Player API. Локальний HTML прямо у WebView
// (loadDataWithBaseURL з підробленим base=youtube.com) YouTube приймає лише частково: базовий
// плеєр стартує (це усувало "помилку 153"), але перевірка авторизації відтворення робить власні
// запити зі справжнім Origin/Referer, якого в такої сторінки немає (вона не прийшла з мережі) —
// це й давало "помилку 152-4" на деяких пристроях. З реальної адреси браузер сам підставляє
// коректний Referer, як зі звичайного сайту.
private const val PLAYER_PAGE_URL = "https://agetman56-dev.github.io/safetube-kids-player-page/player.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbeddedYouTubePlayer(videoId: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.userAgentString = settings.userAgentString.replace("; wv", "")
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        // Обмежуємо тільки головний фрейм — сам плеєр і його внутрішні
                        // запити (youtube.com, ytimg.com тощо) повинні вільно вантажитись.
                        if (!request.isForMainFrame) return false
                        return !request.url.toString().startsWith(PLAYER_PAGE_URL)
                    }
                }
                loadUrl("$PLAYER_PAGE_URL?v=$videoId")
            }
        },
        // Без цього WebView не отримував ні onPause(), ні destroy(): після кнопки
        // «Назад» звук міг грати далі, а сам WebView лишався в пам'яті.
        // Порядок важливий: спершу зупинити відтворення, потім від'єднати, потім знищити.
        onRelease = { webView ->
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.onPause()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.removeAllViews()
            webView.destroy()
        }
    )
}
