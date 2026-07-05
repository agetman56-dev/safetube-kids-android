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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import ua.safetube.kids.AppState

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

    // Лічильник часу перегляду: рахуємо, поки екран плеєра відкритий (просте наближення,
    // без точного стеження за play/pause у самому iframe).
    LaunchedEffect(videoId) {
        while (true) {
            delay(60_000)
            appState.parental.addWatchedSeconds(60)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        EmbeddedYouTubePlayer(videoId = videoId)
        Box(modifier = Modifier.padding(12.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbeddedYouTubePlayer(videoId: String) {
    val embedUrl = "https://www.youtube.com/embed/$videoId" +
        "?rel=0&modestbranding=1&iv_load_policy=3&fs=0&playsinline=1&autoplay=1&enablejsapi=1"

    // YouTube-плеєр очікує, що /embed/... відкриють у <iframe> на чужій сторінці (як на
    // звичайних сайтах), а не як головну адресу WebView напряму — інакше видає "помилку 153".
    // Тому загортаємо у мінімальну HTML-сторінку з іфреймом і вантажимо через
    // loadDataWithBaseURL(base = youtube.com), щоб плеєр бачив "легітимний" origin.
    val html = """
        <!DOCTYPE html>
        <html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
        <body style="margin:0;padding:0;background:#000;overflow:hidden;">
        <iframe src="$embedUrl" width="100%" height="100%" frameborder="0"
          allow="autoplay; encrypted-media; fullscreen" allowfullscreen></iframe>
        </body></html>
    """.trimIndent()

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
                        // Обмежуємо тільки головний фрейм — сам iframe із embed-плеєром
                        // повинен вільно вантажитись і посилатись на свої внутрішні ресурси.
                        if (!request.isForMainFrame) return false
                        return !request.url.toString().startsWith("https://www.youtube.com/embed/")
                    }
                }
                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
            }
        }
    )
}
