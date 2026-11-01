package com.example.jeecommandcenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JeeDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = BgAppBase,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = PrimaryLight,
    secondary = PrimaryLight,
    onSecondary = BgAppBase,
    secondaryContainer = BgCardAlt,
    onSecondaryContainer = TextPrimary,
    tertiary = PrimaryLight,
    onTertiary = BgAppBase,
    tertiaryContainer = BgCardAlt,
    onTertiaryContainer = TextPrimary,
    background = BgAppBase,
    onBackground = TextPrimary,
    surface = BgAppBase,
    onSurface = TextPrimary,
    surfaceVariant = BgCard,
    onSurfaceVariant = TextSecondary,
    outline = BgCardBorder,
    outlineVariant = BgCardBorder,
    error = Danger,
    onError = Color.Black
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
