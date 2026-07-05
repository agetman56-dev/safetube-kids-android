package ua.safetube.kids.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.safetube.kids.AppState

@Composable
fun CategoryScreen(appState: AppState, catIndex: Int, onOpenChannel: (Int) -> Unit, onBack: () -> Unit) {
    val category = appState.categories.value.getOrNull(catIndex) ?: return

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Box {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
        }
        Text(text = category.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(category.channels.size) { idx ->
                val channel = category.channels[idx]
                Box(
                    modifier = Modifier
                        .aspectRatio(1.1f)
                        .background(Color(0xFF37474F), RoundedCornerShape(20.dp))
                        .clickable { onOpenChannel(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = channel.name, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
