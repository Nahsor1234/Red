package com.example.jeecommandcenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Every Material 3 role is set explicitly so dialogs, sheets, text fields and menus
// match the app instead of falling back to the stock baseline.
private val JeeDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainerTone,
    onPrimaryContainer = PrimaryLight,
    secondary = PaletteSecondary,
    onSecondary = PaletteBackground,
    secondaryContainer = PrimarySoft,
    onSecondaryContainer = PrimaryLight,
    tertiary = PaletteAccent,
    onTertiary = PaletteBackground,
    tertiaryContainer = PrimaryContainerTone,
    onTertiaryContainer = TextPrimary,
    error = PaletteText,
    onError = PaletteBackground,
    errorContainer = PrimaryContainerTone,
    onErrorContainer = TextPrimary,
    background = BgAppBase,
    onBackground = TextPrimary,
    surface = BgAppBase,
    onSurface = TextPrimary,
    surfaceVariant = BgCardAlt,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Primary,
    inverseSurface = TextPrimary,
    inverseOnSurface = BgAppBase,
    inversePrimary = PaletteSecondary,
    outline = OutlineStrong,
    outlineVariant = BgCardBorder,
    scrim = Color.Black,
    surfaceBright = BgCardHigh,
    surfaceDim = BgAppBase,
    surfaceContainerLowest = BgAppBase,
    surfaceContainerLow = BgCard,
    surfaceContainer = BgCard,
    surfaceContainerHigh = BgCard,
    surfaceContainerHighest = BgCardAlt
)

private val JeeMaterialShapes = Shapes(
    extraSmall = JeeShapes.small,
    small = JeeShapes.small,
    medium = JeeShapes.medium,
    large = JeeShapes.large,
    extraLarge = JeeShapes.large
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
