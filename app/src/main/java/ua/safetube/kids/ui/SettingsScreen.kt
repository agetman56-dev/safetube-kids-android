package ua.safetube.kids.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.safetube.kids.AppState
import ua.safetube.kids.data.ChannelUrlParser

@Composable
fun SettingsScreen(appState: AppState, activity: Activity, onBack: () -> Unit) {
    val categories = appState.categories.value
    val scope = rememberCoroutineScope()

    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    var selectedCatIdx by remember { mutableStateOf(0) }
    var catMenuExpanded by remember { mutableStateOf(false) }

    val timeLimitEnabled by appState.parental.timeLimitEnabled.collectAsState(initial = false)
    val timeLimitMinutes by appState.parental.timeLimitMinutes.collectAsState(initial = 60)
    var minutesText by remember(timeLimitMinutes) { mutableStateOf(timeLimitMinutes.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
            Text("Налаштування для батьків", modifier = Modifier.padding(start = 8.dp))
        }

        Text("Ліміт часу перегляду", modifier = Modifier.padding(top = 16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = timeLimitEnabled, onCheckedChange = {
                scope.launch { appState.parental.setTimeLimitEnabled(it) }
            })
            Text("Увімкнено", modifier = Modifier.padding(start = 8.dp))
            OutlinedTextField(
                value = minutesText,
                onValueChange = { txt ->
                    minutesText = txt.filter { it.isDigit() }
                    minutesText.toIntOrNull()?.let { m -> scope.launch { appState.parental.setTimeLimitMinutes(m) } }
                },
                label = { Text("хвилин/день") },
                modifier = Modifier.padding(start = 16.dp).width(120.dp)
            )
        }

        Text("Канали", modifier = Modifier.padding(top = 24.dp))
        categories.forEachIndexed { catIdx, category ->
            Text(category.title, modifier = Modifier.padding(top = 8.dp))
            category.channels.forEach { channel ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 2.dp)
                ) {
                    Text(channel.name, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        val updated = categories.toMutableList()
                        val cat = updated[catIdx]
                        updated[catIdx] = cat.copy(channels = cat.channels.filter { it != channel })
                        appState.whitelistRepo.save(updated)
                        appState.reloadCategories()
                    }) { Icon(Icons.Filled.Close, contentDescription = "Прибрати") }
                }
            }
        }

        Text("Додати канал", modifier = Modifier.padding(top = 24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { catMenuExpanded = true }) {
                Text(categories.getOrNull(selectedCatIdx)?.title ?: "Категорія")
            }
            DropdownMenu(expanded = catMenuExpanded, onDismissRequest = { catMenuExpanded = false }) {
                categories.forEachIndexed { idx, cat ->
                    DropdownMenuItem(text = { Text(cat.title) }, onClick = {
                        selectedCatIdx = idx
                        catMenuExpanded = false
                    })
                }
            }
        }
        OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Назва каналу") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(
            value = newUrl,
            onValueChange = { newUrl = it },
            label = { Text("Посилання на канал (youtube.com/@...)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(
            onClick = {
                val parsed = ChannelUrlParser.parse(newName.ifBlank { newUrl }, newUrl)
                if (parsed != null && categories.isNotEmpty()) {
                    val updated = categories.toMutableList()
                    val cat = updated[selectedCatIdx]
                    updated[selectedCatIdx] = cat.copy(channels = cat.channels + parsed)
                    appState.whitelistRepo.save(updated)
                    appState.reloadCategories()
                    newName = ""
                    newUrl = ""
                }
            },
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        ) { Text("Додати") }
    }
}
