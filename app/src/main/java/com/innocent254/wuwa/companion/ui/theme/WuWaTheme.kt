package com.innocent254.wuwa.companion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.innocent254.wuwa.companion.ui.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A70),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF0F5),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF675083),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEDBFF),
    onSecondaryContainer = Color(0xFF22143A),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF2C1600),
    background = Color(0xFFF6FAFA),
    onBackground = Color(0xFF171D1E),
    surface = Color(0xFFF6FAFA),
    onSurface = Color(0xFF171D1E),
    surfaceVariant = Color(0xFFDCE4E5),
    onSurfaceVariant = Color(0xFF404849),
    outline = Color(0xFF707879),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D4D9),
    onPrimary = Color(0xFF00373A),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = Color(0xFF9CF0F5),
    secondary = Color(0xFFD4B9F1),
    onSecondary = Color(0xFF38294F),
    secondaryContainer = Color(0xFF4F4067),
    onSecondaryContainer = Color(0xFFEEDBFF),
    tertiary = Color(0xFFFFB95C),
    onTertiary = Color(0xFF492900),
    tertiaryContainer = Color(0xFF683C00),
    onTertiaryContainer = Color(0xFFFFDDB8),
    background = Color(0xFF0E1415),
    onBackground = Color(0xFFDEE4E5),
    surface = Color(0xFF0E1415),
    onSurface = Color(0xFFDEE4E5),
    surfaceVariant = Color(0xFF3F4849),
    onSurfaceVariant = Color(0xFFBFC8C9),
    outline = Color(0xFF899293),
)

@Composable
fun WuWaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
