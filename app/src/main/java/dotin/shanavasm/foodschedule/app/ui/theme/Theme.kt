package dotin.shanavasm.foodschedule.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary          = Color(0xFF1565C0),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0D47A1),
    secondary        = Color(0xFF42A5F5),
    background       = Color(0xFFF5F5F5),
    surface          = Color(0xFFFFFFFF),
    onBackground     = Color(0xFF1A1A1A),
    onSurface        = Color(0xFF1A1A1A)
)

@Composable
fun FoodScheduleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
