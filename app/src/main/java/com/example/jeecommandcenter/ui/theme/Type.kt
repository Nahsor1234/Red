package com.example.jeecommandcenter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppFont = FontFamily.Default

val AppTypography = Typography(
    displayLarge = TextStyle(AppFont, FontWeight.Medium, 34.sp, color = TextPrimary),
    displayMedium = TextStyle(AppFont, FontWeight.Medium, 30.sp, color = TextPrimary),
    displaySmall = TextStyle(AppFont, FontWeight.Medium, 28.sp, color = TextPrimary),
    headlineLarge = TextStyle(AppFont, FontWeight.Medium, 26.sp, color = TextPrimary),
    headlineMedium = TextStyle(AppFont, FontWeight.Medium, 24.sp, color = TextPrimary),
    headlineSmall = TextStyle(AppFont, FontWeight.Medium, 22.sp, color = TextPrimary),
    titleLarge = TextStyle(AppFont, FontWeight.Medium, 18.sp, color = TextPrimary),
    titleMedium = TextStyle(AppFont, FontWeight.Medium, 16.sp, color = TextPrimary),
    titleSmall = TextStyle(AppFont, FontWeight.Medium, 14.sp, color = TextPrimary),
    bodyLarge = TextStyle(AppFont, FontWeight.Normal, 16.sp, color = TextOnCard),
    bodyMedium = TextStyle(AppFont, FontWeight.Normal, 14.sp, color = TextSecondary),
    bodySmall = TextStyle(AppFont, FontWeight.Normal, 12.sp, color = TextSecondary),
    labelLarge = TextStyle(AppFont, FontWeight.Medium, 14.sp, color = TextOnCard),
    labelMedium = TextStyle(AppFont, FontWeight.Medium, 12.sp, color = TextSecondary),
    labelSmall = TextStyle(AppFont, FontWeight.Normal, 11.sp, color = TextMuted)
)
