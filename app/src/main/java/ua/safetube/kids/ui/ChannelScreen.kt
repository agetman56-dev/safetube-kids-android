package ua.safetube.kids.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@Composable
fun ChannelScreen(
    appState: AppState,
    catIndex: Int,
    chIndex: Int,
    onOpenVideo: (String) -> Unit,
    onBack: () -> Unit
) {
    val channel = appState.categories.value.getOrNull(catIndex)?.channels?.getOrNull(chIndex) ?: return
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    // Зміна цього лічильника перезапускає завантаження — кнопка «Спробувати ще раз»
    var retryCount by remember { mutableStateOf(0) }

    LaunchedEffect(channel, retryCount) {
        loading = true
        errorText = null
        appState.youtubeRepo.loadVideos(channel, forceRefresh = retryCount > 0)
            .onSuccess { videos = it }
            .onFailure { errorText = NetworkErrors.message(it) }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Box {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
        }
        Text(text = channel.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (errorText != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😕", fontSize = 48.sp)
                    Text(
                        text = errorText.orEmpty(),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
                    )
                    Button(onClick = { retryCount++ }) { Text("Спробувати ще раз", fontSize = 18.sp) }
                }
            }
        } else if (videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Поки що немає відео.", fontSize = 16.sp)
                    Button(
                        onClick = { retryCount++ },
                        modifier = Modifier.padding(top = 20.dp)
                    ) { Text("Оновити", fontSize = 18.sp) }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videos.size) { idx ->
                    val video = videos[idx]
                    Column(modifier = Modifier.clickable { onOpenVideo(video.videoId) }) {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = video.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color(0xFFCFD8DC), RoundedCornerShape(12.dp))
                        )
                        Text(
                            text = video.title,
                            fontSize = 14.sp,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
