package com.example.jeecommandcenter.ui.theme

import androidx.compose.ui.graphics.Color

// "Ink & Lamp" palette: deep blue-black ink surfaces lit by a warm amber accent.
// All text/surface pairs below meet WCAG AA (4.5:1) for normal text.

// Surfaces, darkest to lightest.
val BgAppBase = Color(0xFF0E1120)
/** Screen scaffolds are transparent; the root surface in MainActivity paints [BgAppBase]. */
val BgApp = Color.Transparent
val BgCard = Color(0xFF161A2C)
val BgCardAlt = Color(0xFF1F2438)
val BgCardHigh = Color(0xFF2A3150)
val BgCardBorder = Color(0xFF2C3354)
val BgDivider = BgCardHigh

// Text.
val TextPrimary = Color(0xFFECEFFA)
val TextSecondary = Color(0xFFB0B7D3)
val TextMuted = Color(0xFF8790B3)

// Accents.
val Primary = Color(0xFFF2B65A)
val PrimaryLight = Color(0xFFFFD594)
val PrimarySoft = Color(0xFF332A1A)
val OnPrimary = Color(0xFF2A1A00)
val Success = Color(0xFF5FD3A1)
val OnSuccess = Color(0xFF06281B)
val Warning = Color(0xFFF2C94C)
val Danger = Color(0xFFF2777F)
val OnDanger = Color(0xFF2E0A0D)
