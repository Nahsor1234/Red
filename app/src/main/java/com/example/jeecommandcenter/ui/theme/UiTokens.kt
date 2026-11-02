package com.example.jeecommandcenter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object JeeSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 18.dp
    val xl = 26.dp
    val xxl = 34.dp
}

object JeeShapes {
    val small = RoundedCornerShape(12.dp)
    val medium = RoundedCornerShape(18.dp)
    val large = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(50.dp)
}

object JeeSurfaceTokens {
    val borderWidth = 1.dp
    val cardBorderAlpha = 0.82f
    val featuredBorderAlpha = 0.96f
}

/** Shared durations for purposeful, low-distraction interface motion. */
object JeeMotion {
    const val fastMillis = 160
    const val standardMillis = 220
    const val layoutMillis = 300
    const val timerProgressMillis = 1000
}
