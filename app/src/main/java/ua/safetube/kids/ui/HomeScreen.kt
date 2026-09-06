package ua.safetube.kids.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.safetube.kids.AppState

/**
 * Пари кольорів для плиток категорій — градієнт замість пласкої заливки.
 * Теплі й насичені: застосунок має виглядати як дитячий, а не як таблиця.
 */
private val tileGradients = listOf(
    listOf(Color(0xFF42A5F5), Color(0xFF1565C0)),
    listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)),
    listOf(Color(0xFFFFA726), Color(0xFFEF6C00)),
    listOf(Color(0xFFAB47BC), Color(0xFF6A1B9A)),
    listOf(Color(0xFFEF5350), Color(0xFFC62828)),
    listOf(Color(0xFF26C6DA), Color(0xFF00838F))
)

@Composable
fun HomeScreen(
    appState: AppState,
    onOpenCategory: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val categories = appState.categories.value
    var showPinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { appState.reloadCategories() }

    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Пошук — перша плитка, щоб дитина знаходила його одразу
            item {
                CategoryTile(
                    icon = "🔍",
                    title = "Пошук",
                    colors = listOf(Color(0xFF78909C), Color(0xFF37474F)),
                    onClick = onOpenSearch
                )
            }
            items(categories.size) { idx ->
                val category = categories[idx]
                CategoryTile(
                    icon = category.icon ?: "⭐",
                    title = category.title,
                    colors = tileGradients[idx % tileGradients.size],
                    onClick = { onOpenCategory(idx) }
                )
            }
        }

        // Єдина батьківська кнопка. Закріплення екрана прибрано: воно замикало
        // планшет у застосунку, а виходити доводилось системним жестом.
        IconButton(
            onClick = { showPinDialog = true },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Налаштування для батьків",
                tint = Color(0x55000000)   // ледь помітна, щоб не вабила дитину
            )
        }
    }

    if (showPinDialog) {
        PinDialog(
            appState = appState,
            title = "PIN батьків",
            onSuccess = { showPinDialog = false; onOpenSettings() },
            onDismiss = { showPinDialog = false }
        )
    }
}

/** Плитка категорії: великий знак, під ним підпис. Натискання дає віддачу. */
@Composable
private fun CategoryTile(icon: String, title: String, colors: List<Color>, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "tile-press")

    Box(
        modifier = Modifier
            .aspectRatio(1.15f)
            .scale(scale)
            .background(Brush.verticalGradient(colors), RoundedCornerShape(28.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 56.sp)
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp)
            )
        }
    }
}
