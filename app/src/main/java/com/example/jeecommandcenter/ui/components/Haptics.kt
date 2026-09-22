package com.example.jeecommandcenter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

fun Modifier.premiumClick(
    haptic: Int? = null,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    clickable {
        haptic?.let(view::performHapticFeedback)
        onClick()
    }
}
