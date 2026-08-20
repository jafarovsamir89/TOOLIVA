package az.simplesoft.tooliva.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import az.simplesoft.tooliva.core.settings.AppearanceMode
import az.simplesoft.tooliva.core.settings.ToolivaPreferences

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

private val ToolivaTypography = androidx.compose.material3.Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Black),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun ToolivaTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context) { ToolivaPreferences(context) }
    val appearance by preferences.appearance.collectAsState(initial = AppearanceMode.SYSTEM)
    val resolvedDarkTheme = darkTheme ?: when (appearance) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.DARK -> true
        AppearanceMode.LIGHT -> false
    }
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && resolvedDarkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        resolvedDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = ToolivaTypography,
        content = content,
    )
}
