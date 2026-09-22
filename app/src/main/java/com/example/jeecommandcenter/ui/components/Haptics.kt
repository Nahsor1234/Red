package com.example.jeecommandcenter.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

fun Modifier.premiumClick(onClick:()->Unit):Modifier=composed{
 val view=LocalView.current
 combinedClickable(onClick={view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);onClick()})
}
