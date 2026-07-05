package ua.safetube.kids.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ua.safetube.kids.AppState
import ua.safetube.kids.parental.ScreenPinHelper

private val categoryColors = listOf(
    Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFFEF6C00), Color(0xFF6A1B9A), Color(0xFFC62828)
)

@Composable
fun HomeScreen(appState: AppState, onOpenCategory: (Int) -> Unit, onOpenSettings: () -> Unit, activity: Activity) {
    val categories = appState.categories.value
    androidx.compose.runtime.LaunchedEffect(Unit) { appState.reloadCategories() }

    var pinned by remember { mutableStateOf(ScreenPinHelper.isPinned(activity)) }
    var showPinDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories.size) { idx ->
                val category = categories[idx]
                Box(
                    modifier = Modifier
                        .aspectRatio(1.3f)
                        .background(categoryColors[idx % categoryColors.size], RoundedCornerShape(24.dp))
                        .clickable { onOpenCategory(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.title,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Батьківські елементи керування — маленькі, у кутку, не привертають увагу дитини.
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(onClick = {
                if (pinned) {
                    showPinDialog = true
                } else {
                    ScreenPinHelper.pin(activity)
                    pinned = true
                }
            }) {
                Icon(
                    imageVector = if (pinned) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = "Дитячий режим",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            IconButton(onClick = { showPinDialog = true }) {
                Icon(Icons.Filled.Settings, contentDescription = "Налаштування", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showPinDialog) {
        PinDialog(
            appState = appState,
            title = "PIN батьків",
            onSuccess = {
                showPinDialog = false
                if (pinned) {
                    ScreenPinHelper.unpin(activity)
                    pinned = false
                }
                onOpenSettings()
            },
            onDismiss = { showPinDialog = false }
        )
    }
}
