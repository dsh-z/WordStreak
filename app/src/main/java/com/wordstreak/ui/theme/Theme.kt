package com.wordstreak.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = White,
    secondary = GreenDark,
    onSecondary = White,
    background = White,
    onBackground = InkBlack,
    surface = White,
    onSurface = InkBlack,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = MutedGray,
    outline = LineGray
)

@Composable
fun WordStreakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
