package com.example.jeecommandcenter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JeeDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentGreen,
    background = BgApp,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgCardAlt,
    onSurfaceVariant = TextSecondary,
    outline = BgDivider,
    error = TextPriorityBadge
)

@Composable
fun JeePrepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JeeDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}