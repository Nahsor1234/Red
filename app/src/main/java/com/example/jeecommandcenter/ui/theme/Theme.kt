package com.example.jeecommandcenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JeeDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.Black,
    primaryContainer = AccentBlueSoft,
    onPrimaryContainer = TextPrimary,
    secondary = AccentGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF18181B),
    onSecondaryContainer = TextPrimary,
    tertiary = AccentBlueLight,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF1E1E21),
    onTertiaryContainer = TextPrimary,
    background = BgApp,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgCardAlt,
    onSurfaceVariant = TextSecondary,
    outline = BgCardBorder,
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
