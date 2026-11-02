package com.example.jeecommandcenter.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object JeeSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object JeeShapes {
    val small = RoundedCornerShape(14.dp)
    val medium = RoundedCornerShape(20.dp)
    val large = RoundedCornerShape(28.dp)
    /** A true capsule silhouette; use this for the floating primary navigation. */
    val pill = CircleShape
}

object JeeSurfaceTokens {
    val borderWidth = 1.dp
    val cardBorderAlpha = 1f
    val featuredBorderAlpha = 1f
}

/** Shared durations for calm, purposeful motion; Compose follows the Android animator scale. */
object JeeMotion {
    const val fastMillis = 140
    const val standardMillis = 210
    const val layoutMillis = 280
    const val enterMillis = 300
    const val timerProgressMillis = 1000
}
