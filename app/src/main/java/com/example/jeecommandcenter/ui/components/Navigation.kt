package com.example.jeecommandcenter.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.ui.theme.BgCardHigh
import com.example.jeecommandcenter.ui.theme.JeeShapes
import com.example.jeecommandcenter.ui.theme.OnPrimary
import com.example.jeecommandcenter.ui.theme.Primary
import com.example.jeecommandcenter.ui.theme.TextMuted

/** Top-level destinations. STATS is the study timer. */
enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    SYLLABUS("Syllabus", Icons.Rounded.MenuBook),
    AI("AI", Icons.Rounded.AutoAwesome),
    TASKS("Tasks", Icons.Rounded.AssignmentTurnedIn),
    STATS("Timer", Icons.Rounded.Timer)
}

/**
 * Floating pill navigation. The selected destination expands to show its label; colour,
 * scale and label are driven by a single transition so they stay in sync.
 */
@Composable
fun FloatingNavBar(selected: AppTab, onTabSelected: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(shape = JeeShapes.pill, color = BgCardHigh) {
            Row(
                Modifier.padding(6.dp).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTab.entries.forEach { tab ->
                    NavItem(tab, tab == selected) {
                        if (tab != selected) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onTabSelected(tab)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(tab: AppTab, selected: Boolean, onClick: () -> Unit) {
    val transition = updateTransition(selected, label = "nav-item")
    val container by transition.animateColor(label = "container", transitionSpec = { tween(220) }) {
        if (it) Primary else Color.Transparent
    }
    val content by transition.animateColor(label = "content", transitionSpec = { tween(220) }) {
        if (it) OnPrimary else TextMuted
    }
    val iconScale by transition.animateFloat(label = "scale", transitionSpec = { tween(220) }) {
        if (it) 1f else 0.92f
    }
    Row(
        Modifier
            .clip(JeeShapes.pill)
            .background(container)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = content, modifier = Modifier.size(24.dp).scale(iconScale))
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(tween(220)) + fadeIn(tween(220)),
            exit = shrinkHorizontally(tween(180)) + fadeOut(tween(120))
        ) {
            Text(
                tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
