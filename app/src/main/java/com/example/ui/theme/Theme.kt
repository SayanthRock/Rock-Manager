package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    primaryContainer = HeaderViolet,
    onPrimaryContainer = OnHeaderViolet,
    secondary = ActionZipsBg,
    onSecondary = ActionZipsText,
    tertiary = ActionPermsBg,
    onTertiary = ActionPermsText,
    background = BackgroundColor,
    onBackground = TextPrimary,
    surface = WhiteCardBg,
    onSurface = TextPrimary,
    outline = BorderColor,
    surfaceVariant = NavBackground,
    onSurfaceVariant = TextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = OnHeaderViolet,
    primaryContainer = AccentPurple,
    onPrimaryContainer = HeaderViolet,
    secondary = ActionZipsBg,
    onSecondary = ActionZipsText,
    tertiary = ActionPermsBg,
    onTertiary = ActionPermsText,
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFE6E1E5),
    outline = BorderColor,
    surfaceVariant = Color(0xFF25232A),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We want our beautiful Vibrant Palette to shine, so we default to manual color mapping
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
