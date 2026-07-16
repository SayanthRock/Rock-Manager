package com.example.ui.theme

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
    secondary = Color(0xFF4A4458),
    onSecondary = Color(0xFFE8DEF8),
    tertiary = Color(0xFF633B48),
    onTertiary = Color(0xFFFFD8E4),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFE6E1E5),
    outline = Color(0xFF49454F),
    surfaceVariant = Color(0xFF25232A),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Support dynamic Material 3 color system when applicable
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

