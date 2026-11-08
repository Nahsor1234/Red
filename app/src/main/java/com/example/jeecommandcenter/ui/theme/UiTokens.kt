package com.example.jeecommandcenter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Corner radii: smaller radii for dense controls, larger for containers. */
object JeeShapes {
    val small = RoundedCornerShape(10.dp)
    val medium = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(50)
}

object JeeSurfaceTokens {
    val borderWidth = 1.dp
    const val cardBorderAlpha = 0.8f
}
