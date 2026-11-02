package com.example.jeecommandcenter.ui.theme

import androidx.compose.ui.graphics.Color

// Light-first editorial study palette: paper surfaces, ink hierarchy, focused blue, warm action.
val BgAppBase = Color(0xFFF6F7FB)
val BgApp = Color.Transparent
val BgCard = Color(0xFFFFFFFF)
val BgCardAlt = Color(0xFFEEF2F8)
val BgCardBorder = Color(0xFFE0E6F0)
val BgDivider = Color(0xFFE4E9F1)
val BgPriority = Color(0xFFFFF0E6)
val BorderPriority = Color(0xFFF2C8AD)
val BgPriorityBadge = Color(0xFFFFE2D1)
val BgFeature = Color(0xFFFFF4EA)
val BorderFeature = Color(0xFFF1D2BD)

val TextPriorityBadge = Color(0xFF8C3A0B)
val TextAmber = Color(0xFF9A4A08)
val TextPrimary = Color(0xFF192436)
val TextSecondary = Color(0xFF536176)
val TextMuted = Color(0xFF66758A)
val TextOnCard = Color(0xFF192436)

// Blue signals navigation, selection and study progress; warm orange owns the primary focus action.
val Primary = Color(0xFF2563EB)
val PrimaryLight = Color(0xFF1D4ED8)
val PrimarySoft = Color(0xFFE7EEFF)
val FocusAction = Color(0xFFEA580C)
val FocusActionSoft = Color(0xFFFFE8D8)
val OnFocusAction = Color(0xFF172033)
val Success = Color(0xFF16805D)
val SuccessDark = Color(0xFF063F2F)
val SuccessOnFill = Color.White
val Warning = Color(0xFFB45309)
val Danger = Color(0xFFB42318)
val ChipSelectedBg = Primary
val ChipUnselectedBg = BgCardAlt

// Legacy aliases retained while screen-specific Compose code migrates to semantic tokens.
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
