package com.example.jeecommandcenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Every Material 3 role is set explicitly so dialogs, sheets, text fields and menus
// match the app instead of falling back to the stock purple-grey baseline.
private val JeeDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFF4A3510),
    onPrimaryContainer = PrimaryLight,
    secondary = Color(0xFF9DB4FF),
    onSecondary = Color(0xFF0B1840),
    secondaryContainer = Color(0xFF26325C),
    onSecondaryContainer = Color(0xFFDCE4FF),
    tertiary = Success,
    onTertiary = OnSuccess,
    tertiaryContainer = Color(0xFF123F2E),
    onTertiaryContainer = Color(0xFFB8F2D8),
    error = Danger,
    onError = OnDanger,
    errorContainer = Color(0xFF5C1E24),
    onErrorContainer = Color(0xFFFFD9DB),
    background = BgAppBase,
    onBackground = TextPrimary,
    surface = BgAppBase,
    onSurface = TextPrimary,
    surfaceVariant = BgCardAlt,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Primary,
    inverseSurface = TextPrimary,
    inverseOnSurface = BgAppBase,
    inversePrimary = Color(0xFF7A5200),
    outline = OutlineStrong,
    outlineVariant = BgCardBorder,
    scrim = Color.Black,
    surfaceBright = BgCardHigh,
    surfaceDim = BgAppBase,
    surfaceContainerLowest = Color(0xFF0A0C18),
    surfaceContainerLow = BgCard,
    surfaceContainer = BgCard,
    surfaceContainerHigh = BgCardAlt,
    surfaceContainerHighest = BgCardHigh
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
