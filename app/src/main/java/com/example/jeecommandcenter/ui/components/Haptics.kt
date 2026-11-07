package com.example.jeecommandcenter.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

/** Centralized, restrained tactile feedback for meaningful interactions. */
fun Modifier.premiumClick(
    onClick: () -> Unit
): Modifier = premiumClick(HapticFeedbackConstants.VIRTUAL_KEY, Role.Button, onClick)

fun Modifier.premiumClick(
    haptic: Int,
    onClick: () -> Unit
): Modifier = premiumClick(haptic, Role.Button, onClick)

fun Modifier.premiumClick(
    haptic: Int,
    role: Role?,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    clickable(role = role) {
        view.performHapticFeedback(haptic)
        onClick()
    }
}
