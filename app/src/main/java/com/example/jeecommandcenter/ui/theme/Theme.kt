package com.example.jeecommandcenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JeeDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlueSoft,
    onPrimaryContainer = TextPrimary,
    secondary = AccentGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF103A24),
    onSecondaryContainer = TextPrimary,
    tertiary = AccentPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF341447),
    onTertiaryContainer = TextPrimary,
    background = BgApp,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgCardAlt,
    onSurfaceVariant = TextSecondary,
    outline = BgDivider,
    outlineVariant = BgCardBorder,
    error = TextPriorityBadge,
    onError = Color.White
)

private val JeeMaterialShapes = Shapes(
    small = JeeShapes.small,
    medium = JeeShapes.medium,
    large = JeeShapes.large
)

@Composable
fun JeePrepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JeeDarkColorScheme,
        typography = AppTypography,
        shapes = JeeMaterialShapes,
        content = content
    )
}
