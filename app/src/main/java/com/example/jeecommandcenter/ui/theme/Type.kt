package com.example.jeecommandcenter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.R

/** Manrope (SIL OFL 1.1), bundled as static weights. Glyphs outside Latin fall back to the system font. */
val AppFont = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold)
)

private fun style(
    weight: FontWeight,
    size: Int,
    line: Int,
    spacing: Double = 0.0,
    color: androidx.compose.ui.graphics.Color = TextPrimary
) = TextStyle(
    fontFamily = AppFont,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp,
    color = color
)

// Default text colors are part of the type roles (primary for titles, secondary for body
// copy, muted for captions) so existing screens keep their visual hierarchy.
val AppTypography = Typography(
    displayLarge = style(FontWeight.SemiBold, 40, 46, -0.8),
    displayMedium = style(FontWeight.SemiBold, 34, 40, -0.6),
    displaySmall = style(FontWeight.SemiBold, 28, 34, -0.4),
    headlineLarge = style(FontWeight.SemiBold, 26, 32, -0.3),
    headlineMedium = style(FontWeight.SemiBold, 24, 30, -0.2),
    headlineSmall = style(FontWeight.SemiBold, 22, 28, -0.2),
    titleLarge = style(FontWeight.SemiBold, 19, 25, -0.1),
    titleMedium = style(FontWeight.SemiBold, 16, 22),
    titleSmall = style(FontWeight.SemiBold, 14, 20, 0.1),
    bodyLarge = style(FontWeight.Normal, 16, 24, 0.0),
    bodyMedium = style(FontWeight.Normal, 14, 21, 0.1, TextSecondary),
    bodySmall = style(FontWeight.Normal, 12, 17, 0.2, TextSecondary),
    labelLarge = style(FontWeight.SemiBold, 14, 20, 0.1),
    labelMedium = style(FontWeight.Medium, 12, 16, 0.3, TextSecondary),
    labelSmall = style(FontWeight.Medium, 11, 15, 0.3, TextMuted)
)
