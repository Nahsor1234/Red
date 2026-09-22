package com.example.jeecommandcenter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

fun Modifier.premiumClick(
    onClick: () -> Unit,
    haptic: Int? = null
): Modifier = composed {
    val view = LocalView.current
    clickable {
        haptic?.let(view::performHapticFeedback)
        onClick()
    }
}
