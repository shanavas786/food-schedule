package dotin.shanavasm.usthadchelav.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary       = Color(0xFF1565C0)
private val PrimaryDark   = Color(0xFF0D47A1)
private val Secondary     = Color(0xFF42A5F5)
private val Background    = Color(0xFFF5F5F5)
private val Surface       = Color(0xFFFFFFFF)
private val OnPrimary     = Color(0xFFFFFFFF)
private val OnBackground  = Color(0xFF1A1A1A)
private val OnSurface     = Color(0xFF1A1A1A)

private val LightColors = lightColorScheme(
    primary         = Primary,
    onPrimary       = OnPrimary,
    primaryContainer = PrimaryDark,
    secondary       = Secondary,
    background      = Background,
    surface         = Surface,
    onBackground    = OnBackground,
    onSurface       = OnSurface
)

@Composable
fun ChelavScheduleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
