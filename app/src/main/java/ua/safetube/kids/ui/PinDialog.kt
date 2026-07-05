package ua.safetube.kids.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.safetube.kids.AppState

/**
 * Модалка вводу PIN. Якщо PIN ще не встановлено — перший введений PIN одразу
 * зберігається як новий (перше налаштування батьківського коду).
 */
@Composable
fun PinDialog(appState: AppState, title: String, onSuccess: () -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) { pin = it; error = false } },
                    label = { Text("PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error,
                    modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)
                )
                if (error) Text("Невірний PIN")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val isSet = appState.parental.isPinSet()
                    if (!isSet) {
                        if (pin.length in 4..6) {
                            appState.parental.setPin(pin)
                            onSuccess()
                        } else {
                            error = true
                        }
                    } else if (appState.parental.verifyPin(pin)) {
                        onSuccess()
                    } else {
                        error = true
                    }
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}
