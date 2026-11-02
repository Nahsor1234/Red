package com.example.jeecommandcenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JeeLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = PrimaryLight,
    secondary = FocusAction,
    onSecondary = OnFocusAction,
    secondaryContainer = FocusActionSoft,
    onSecondaryContainer = OnFocusAction,
    tertiary = Success,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE4F4EC),
    onTertiaryContainer = SuccessDark,
    background = BgAppBase,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgCardAlt,
    onSurfaceVariant = TextSecondary,
    outline = BgCardBorder,
    outlineVariant = BgDivider,
    error = Danger,
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
        colorScheme = JeeLightColorScheme,
        typography = AppTypography,
        shapes = JeeMaterialShapes,
        content = content
    )
}
