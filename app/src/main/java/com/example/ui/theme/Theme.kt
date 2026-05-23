package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ThakiroonDarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color(0xFF1E1E1E),
    secondary = EmeraldActive,
    onSecondary = Color.White,
    tertiary = GoldAccentMuted,
    background = DarkBg,
    onBackground = TextWhite,
    surface = SurfaceOlive,
    onSurface = TextWhite,
    surfaceVariant = SurfaceOliveLight,
    onSurfaceVariant = TextWhite,
    outline = BorderGreen
)

// We want a unified brand theme, so we'll use Dark theme as the main experience!
@Composable
fun ThakiroonTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ThakiroonDarkColorScheme,
        typography = Typography,
        content = content
    )
}
