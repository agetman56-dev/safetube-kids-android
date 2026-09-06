package ua.safetube.kids.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import ua.safetube.kids.AppState

/**
 * Ввід PIN власною екранною клавіатурою.
 *
 * Раніше тут було звичайне поле з системною клавіатурою. Застосунок примусово
 * в альбомній орієнтації, а системна клавіатура в ній займає пів екрана і
 * накриває кнопку OK — на смартфоні ввести PIN було просто неможливо.
 * Власні кнопки цієї проблеми не мають узагалі й розкладаються в рядок,
 * як і належить в альбомному екрані.
 *
 * Якщо PIN ще не встановлено — перший введений стає новим (перше налаштування).
 */
@Composable
fun PinDialog(appState: AppState, title: String, onSuccess: () -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        scope.launch {
            if (!appState.parental.isPinSet()) {
                if (pin.length in 4..6) {
                    appState.parental.setPin(pin)
                    onSuccess()
                } else {
                    error = "Задайте PIN із 4–6 цифр"
                    pin = ""
                }
            } else if (appState.parental.verifyPin(pin)) {
                onSuccess()
            } else {
                error = "Невірний PIN"
                pin = ""
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {

                // Ліва частина: заголовок, крапки-індикатор, помилка, скасування
                Column(
                    modifier = Modifier.width(200.dp).padding(end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    PinDots(count = pin.length)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = error ?: " ",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                    TextButton(onClick = onDismiss) { Text("Скасувати") }
                }

                // Права частина: клавіатура 3 в ряд
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))) {
                        Row {
                            for (key in row) {
                                PinKey(key) {
                                    if (pin.length < 6) { pin += key; error = null }
                                }
                            }
                        }
                    }
                    Row {
                        PinKey("←") { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                        PinKey("0") { if (pin.length < 6) { pin += "0"; error = null } }
                        PinKey("OK", highlighted = true) { if (pin.isNotEmpty()) submit() }
                    }
                }
            }
        }
    }
}

/** Крапки замість цифр: видно довжину, але не сам код. */
@Composable
private fun PinDots(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(6) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        if (index < count) MaterialTheme.colorScheme.primary else Color(0x33000000),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun PinKey(label: String, highlighted: Boolean = false, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(width = 74.dp, height = 52.dp)
    ) {
        Text(
            text = label,
            fontSize = if (label.length > 1) 16.sp else 22.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
        )
    }
}
