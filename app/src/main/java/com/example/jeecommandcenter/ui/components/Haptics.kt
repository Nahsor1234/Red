package com.example.jeecommandcenter.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

/** Centralized, restrained tactile feedback for meaningful interactions. */
fun Modifier.premiumClick(
    onClick: () -> Unit,
    haptic: Int = HapticFeedbackConstants.VIRTUAL_KEY
): Modifier = composed {
    val view = LocalView.current
    clickable {
        view.performHapticFeedback(haptic)
        onClick()
    }
}

fun Modifier.premiumClick(haptic: Int?, onClick: () -> Unit): Modifier = composed {
    val view = LocalView.current
    clickable {
        haptic?.let(view::performHapticFeedback)
        onClick()
    }
}
