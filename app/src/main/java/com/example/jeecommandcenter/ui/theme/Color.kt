package com.example.jeecommandcenter.ui.theme

import androidx.compose.ui.graphics.Color

// Warm Graphite palette
val BgApp = Color(0xFF0C0C0D)          // Background
val BgCard = Color(0xFF18181B)         // Surface
val BgCardAlt = Color(0xFF1E1E21)      // Surface Alt
val BgCardBorder = Color(0xFF3F3F42)   // Border / Divider
val BgDivider = Color(0xFF3F3F42)     // Border / Divider
val BgPriority = Color(0xFF1E1E21)
val BorderPriority = Color(0xFF3F3F42)
val BgPriorityBadge = Color(0xFF1E1E21)
val TextPriorityBadge = Color(0xFFEF4444) // Danger
val TextAmber = Color(0xFFFCD34D)         // Primary Light
val TextPrimary = Color(0xFFFAFAFA)
val TextSecondary = Color(0xFFA1A1A8)
val TextMuted = Color(0xFF6B6B70)
val TextOnCard = Color(0xFFFAFAFA)
val AccentBlue = Color(0xFFF59E0B)      // Primary / Accent
val AccentBlueLight = Color(0xFFFCD34D) // Primary Light
val AccentBlueSoft = Color(0xFF1E1E21)
val AccentGreen = Color(0xFF4ADE80)     // Success
val AccentGreenDark = Color(0xFF18181B)
val AccentAmber = Color(0xFFFACC15)     // Warning
val AccentPurple = Color(0xFFFCD34D)
val AccentPink = Color(0xFFEF4444)
val ChipSelectedBg = AccentBlue
val ChipUnselectedBg = BgCard

object JeeColors {
    val background = BgApp
    val surface = BgCard
    val elevatedSurface = BgCardAlt
    val primary = AccentBlue
    val primarySoft = AccentBlueSoft
    val secondary = AccentGreen
    val accent = AccentBlueLight
    val success = AccentGreen
    val warning = AccentAmber
    val error = TextPriorityBadge
    val info = AccentBlueLight
    val primaryText = TextPrimary
    val secondaryText = TextSecondary
    val mutedText = TextMuted
    val onSurface = TextOnCard
    val divider = BgDivider
    val outline = BgCardBorder
}
