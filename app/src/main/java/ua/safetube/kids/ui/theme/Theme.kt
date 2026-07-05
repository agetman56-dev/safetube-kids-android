package ua.safetube.kids.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KidsGreen = Color(0xFF2E7D32)
private val KidsOrange = Color(0xFFEF6C00)
private val KidsBlue = Color(0xFF1565C0)

private val LightColors = lightColorScheme(
    primary = KidsGreen,
    secondary = KidsOrange,
    tertiary = KidsBlue
)

private val DarkColors = darkColorScheme(
    primary = KidsGreen,
    secondary = KidsOrange,
    tertiary = KidsBlue
)

@Composable
fun SafeTubeKidsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
