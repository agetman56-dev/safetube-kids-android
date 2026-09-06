package ua.safetube.kids.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ua.safetube.kids.AppState
import ua.safetube.kids.data.NetworkErrors
import ua.safetube.kids.data.Video

/**
 * Готові теми пошуку. Дитина у 4 роки не набирає текст, тому пошук — це
 * великі кнопки, а не поле вводу. Запити навмисно українською: параметр
 * relevanceLanguage=uk і так підштовхує видачу, а українське слово в запиті
 * підштовхує ще сильніше.
 */
private val presets = listOf(
    Triple("🎬", "Мультики", "мультики українською для дітей"),
    Triple("🎵", "Пісеньки", "дитячі пісні українською"),
    Triple("📖", "Казки", "українські казки для дітей"),
    Triple("🐾", "Тварини", "тварини для дітей українською"),
    Triple("🚗", "Машинки", "машинки для дітей українською"),
    Triple("🎨", "Малювання", "малювання для дітей українською")
)

@Composable
fun SearchScreen(appState: AppState, onOpenVideo: (String) -> Unit, onBack: () -> Unit) {
    var query by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<Video>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        val q = query ?: return@LaunchedEffect
        loading = true
        errorText = null
        appState.youtubeRepo.search(q)
            .onSuccess { results = it }
            .onFailure { errorText = NetworkErrors.message(it) }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
            Text("Що подивимось?", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp))
        }

        // Теми
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            items(presets.size) { idx ->
                val (icon, label, q) = presets[idx]
                val selected = query == q
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            if (selected) Color(0xFF1565C0) else Color(0xFFE3F2FD),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { query = q }
                        .padding(vertical = 10.dp)
                ) {
                    Text(icon, fontSize = 30.sp)
                    Text(
                        label,
                        fontSize = 13.sp,
                        color = if (selected) Color.White else Color(0xFF0D47A1),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

            errorText != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😕", fontSize = 44.sp)
                    Text(errorText.orEmpty(), modifier = Modifier.padding(vertical = 12.dp))
                    Button(onClick = { val q = query; query = null; query = q }) { Text("Спробувати ще раз") }
                }
            }

            query == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Обери тему вгорі 👆", fontSize = 18.sp, color = Color(0xFF607D8B))
            }

            results.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Нічого не знайшлось. Спробуй іншу тему.", fontSize = 16.sp)
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results.size) { idx ->
                    val video = results[idx]
                    Column(modifier = Modifier.clickable { onOpenVideo(video.videoId) }) {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = video.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color(0xFFCFD8DC), RoundedCornerShape(12.dp))
                        )
                        Text(video.title, fontSize = 13.sp, maxLines = 2,
                            modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
