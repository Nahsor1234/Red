package com.example.jeecommandcenter.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Brand palette. These six values are the only source colors in the app; every other
// token below is a tonal variation derived from them with lerp().
val PaletteBackground = Color(0xFF151719)
val PaletteSurface = Color(0xFF24282B)
val PalettePrimary = Color(0xFFA7B3BC)
val PaletteSecondary = Color(0xFF858F97)
val PaletteAccent = Color(0xFFD4DCE1)
val PaletteText = Color(0xFFEDF0F2)

// Surfaces, darkest to lightest. Cards, dialogs and sheets are tonal (no borders needed).
val BgAppBase = PaletteBackground
/** Screen scaffolds are transparent; the root surface in MainActivity paints [BgAppBase]. */
val BgApp = Color.Transparent
val BgCard = PaletteSurface
val BgCardAlt = lerp(PaletteSurface, PaletteText, 0.05f)
val BgCardHigh = lerp(PaletteSurface, PaletteText, 0.10f)
val BgCardBorder = lerp(PaletteSurface, PaletteText, 0.12f)
val BgDivider = BgCardHigh
/** Selected / active container: surface lifted toward Primary. */
val PrimarySoft = lerp(PaletteSurface, PalettePrimary, 0.18f)
val PrimaryContainerTone = lerp(PaletteSurface, PalettePrimary, 0.26f)
/** Visible outline for interactive controls (3:1 against Surface). */
val OutlineStrong = lerp(PaletteSurface, PaletteText, 0.38f)

// Text.
val TextPrimary = PaletteText
val TextSecondary = PalettePrimary
/** Secondary lifted 25% toward Text so small captions keep 4.5:1 on raised surfaces. */
val TextMuted = lerp(PaletteSecondary, PaletteText, 0.25f)

// Accents.
val Primary = PalettePrimary
val PrimaryLight = PaletteAccent
val OnPrimary = PaletteBackground

// Status is carried by brightness, labels and icons rather than extra hues.
val Success = PaletteAccent
val OnSuccess = PaletteBackground
val Warning = PaletteText
val Danger = PaletteText
