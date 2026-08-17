package az.simplesoft.tooliva.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Color(0xFF65E6C4),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF0A4F43),
    onPrimaryContainer = Color(0xFFB6F5E4),
    secondary = Color(0xFF9FB5FF),
    background = Color(0xFF0B0F14),
    surface = Color(0xFF11171F),
    surfaceVariant = Color(0xFF1B232D),
    onBackground = Color(0xFFE7EDF5),
    onSurface = Color(0xFFE7EDF5),
    outline = Color(0xFF84909F),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B58),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2D8),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF40558F),
    background = Color(0xFFF6F8FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EEF5),
    onBackground = Color(0xFF171C22),
    onSurface = Color(0xFF171C22),
    outline = Color(0xFF737B87),
)

@Composable
fun ToolivaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
