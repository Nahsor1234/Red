package com.example.jeecommandcenter.ui.theme

import androidx.compose.ui.graphics.Color

// Warm Graphite palette
val BgAppBase = Color(0xFF0C0C0D)
// Screen scaffolds are transparent so the global academic wallpaper can sit behind them.
val BgApp = Color.Transparent
val BgCard = Color(0xFF18181B)
val BgCardAlt = Color(0xFF1E1E21)
val BgCardBorder = Color(0xFF3F3F42)
val BgDivider = Color(0xFF3F3F42)
val BgPriority = Color(0xFF1E1E21)
val BorderPriority = Color(0xFF3F3F42)
val BgPriorityBadge = Color(0xFF1E1E21)

val TextPriorityBadge = Color(0xFFEF4444)
val TextAmber = Color(0xFFFCD34D)
val TextPrimary = Color(0xFFFAFAFA)
val TextSecondary = Color(0xFFA1A1A8)
val TextMuted = Color(0xFF6B6B70)
val TextOnCard = Color(0xFFFAFAFA)

// Semantic accent tokens. Prefer these names in new code.
val Primary = Color(0xFFF59E0B)
val PrimaryLight = Color(0xFFFCD34D)
val PrimarySoft = Color(0xFF1E1E21)
val Success = Color(0xFF4ADE80)
val SuccessDark = Color(0xFF18181B)
val Warning = Color(0xFFFACC15)
val Danger = Color(0xFFEF4444)
val ChipSelectedBg = Primary
val ChipUnselectedBg = BgCard

// Legacy aliases retained so existing screens can migrate incrementally.
@Deprecated("Use Primary")
val AccentBlue = Primary
@Deprecated("Use PrimaryLight")
val AccentBlueLight = PrimaryLight
@Deprecated("Use PrimarySoft")
val AccentBlueSoft = PrimarySoft
@Deprecated("Use Success")
val AccentGreen = Success
@Deprecated("Use SuccessDark")
val AccentGreenDark = SuccessDark
@Deprecated("Use Warning")
val AccentAmber = Warning
@Deprecated("Use Danger")
val AccentPink = Danger
