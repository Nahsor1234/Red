package com.example.jeecommandcenter.ui.theme

import androidx.compose.ui.graphics.Color

// Warm Graphite palette
val BgAppBase = Color(0xFF151916)
// Screen scaffolds are transparent so the global academic wallpaper can sit behind them.
val BgApp = Color.Transparent
val BgCard = Color(0xFF20251F)
val BgCardAlt = Color(0xFF2B312A)
val BgCardBorder = Color(0xFF444D43)
val BgDivider = Color(0xFF444D43)
val BgPriority = Color(0xFF2B312A)
val BorderPriority = Color(0xFF444D43)
val BgPriorityBadge = Color(0xFF2B312A)

val TextPriorityBadge = Color(0xFFC0E98B)
val TextAmber = Color(0xFFC0E98B)
val TextPrimary = Color(0xFFF2F6EF)
val TextSecondary = Color(0xFFAEB7A7)
val TextMuted = Color(0xFF7F8979)
val TextOnCard = Color(0xFFF2F6EF)

// Semantic accent tokens. Prefer these names in new code.
val Primary = Color(0xFF9CCB65)
val PrimaryLight = Color(0xFFC0E98B)
val PrimarySoft = Color(0xFF2B312A)
val Success = Color(0xFF4ADE80)
val SuccessDark = Color(0xFF151916)
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
