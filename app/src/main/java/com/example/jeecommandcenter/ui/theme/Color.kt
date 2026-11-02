package com.example.jeecommandcenter.ui.theme

import androidx.compose.ui.graphics.Color

// Midnight study-studio palette: quiet surfaces, crisp blue focus, green progress.
val BgAppBase = Color(0xFF0A0F17)
// Screen scaffolds are transparent so the shared ambient background can sit behind them.
val BgApp = Color.Transparent
val BgCard = Color(0xFF111B29)
val BgCardAlt = Color(0xFF172438)
val BgCardBorder = Color(0xFF29384C)
val BgDivider = Color(0xFF263448)
val BgPriority = Color(0xFF192742)
val BorderPriority = Color(0xFF344D73)
val BgPriorityBadge = Color(0xFF22314D)

val TextPriorityBadge = Color(0xFFD7E3FF)
val TextAmber = Color(0xFFFFD28A)
val TextPrimary = Color(0xFFF3F6FC)
val TextSecondary = Color(0xFFA8B5C7)
val TextMuted = Color(0xFF8493A7)
val TextOnCard = Color(0xFFEAF0F8)

// Semantic accents: blue means focus; green is reserved for completion and success.
val Primary = Color(0xFF8FAEFF)
val PrimaryLight = Color(0xFFBFD1FF)
val PrimarySoft = Color(0xFF202F4B)
val Success = Color(0xFF63D6A2)
val SuccessDark = Color(0xFF08130E)
val Warning = Color(0xFFF4C56D)
val Danger = Color(0xFFFF7285)
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
