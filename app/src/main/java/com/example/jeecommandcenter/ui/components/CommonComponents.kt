package com.example.jeecommandcenter.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.theme.*

enum class AppTab { HOME, SYLLABUS, TASKS, STATS }

@Composable
fun LinearStatBar(progress: Float, modifier: Modifier = Modifier, trackColor: Color = BgDivider, fillColor: Color = AccentBlue, height: androidx.compose.ui.unit.Dp = 5.dp) {
    val animatedProgress = animateFloatAsState(progress.coerceIn(0f, 1f), label = "linear-progress").value
    Box(modifier.fillMaxWidth().height(height).clip(JeeShapes.pill).background(trackColor)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).clip(JeeShapes.pill).background(fillColor))
    }
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onActionClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null) Text(actionLabel, color = AccentBlue, fontSize = 12.sp, modifier = Modifier.premiumClick { onActionClick() })
    }
}

data class TaskItem(val id: String, val title: String, val subtitle: String, val duration: String, val done: Boolean = false, val inProgress: Boolean = false)

@Composable
fun TaskRow(task: TaskItem, onToggle: (String) -> Unit) {
    val scale = animateFloatAsState(if (task.done) 1f else 0.98f, label = "task-scale").value
    val checkColor = animateColorAsState(if (task.done) AccentGreen else Color.Transparent, label = "task-state").value
    Row(Modifier.fillMaxWidth().padding(vertical = JeeSpacing.sm).graphicsLayer(scaleX = scale, scaleY = scale), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).premiumClick(haptic = if (!task.done) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.VIRTUAL_KEY, onClick = { onToggle(task.id) }), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(checkColor), contentAlignment = Alignment.Center) {
                if (task.done) Icon(Icons.Filled.CheckBox, "Completed", tint = AccentGreenDark, modifier = Modifier.size(JeeSizes.iconSmall))
            }
            Spacer(Modifier.width(JeeSpacing.md))
            Column {
                Text(task.title, color = if (task.done) TextMuted else TextOnCard, style = MaterialTheme.typography.bodyMedium, textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None)
                if (task.subtitle.isNotEmpty()) Text(task.subtitle, color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(task.duration, color = TextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    M3FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, leadingIcon = if (selected) {{ Icon(Icons.Filled.Check, null, modifier = Modifier.size(JeeSizes.iconSmall)) }} else null)
}

@Composable
fun BottomNavBar(selected: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit) {
    val view = LocalView.current
    NavigationBar(modifier = Modifier.heightIn(min = 64.dp, max = 72.dp), containerColor = BgCard, tonalElevation = 0.dp, windowInsets = NavigationBarDefaults.windowInsets) {
        NavIcon(Icons.Filled.Home, "Home", selected == AppTab.HOME) { onTabSelected(AppTab.HOME) }
        NavIcon(Icons.Filled.MenuBook, "Syllabus", selected == AppTab.SYLLABUS) { onTabSelected(AppTab.SYLLABUS) }
        NavigationBarItem(
            selected = false,
            onClick = { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); onAiClick() },
            icon = {
                val transition = rememberInfiniteTransition(label = "ai-nav")
                val pulse = transition.animateFloat(0.98f, 1.03f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ai-pulse")
                Box(Modifier.size(44.dp).graphicsLayer(scaleX = pulse.value, scaleY = pulse.value).clip(CircleShape).background(AccentBlue), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AutoAwesome, "AI Hub", tint = Color.White, modifier = Modifier.size(21.dp))
                }
            },
            label = { Text("AI") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.White, selectedTextColor = AccentBlueLight, unselectedTextColor = TextMuted, indicatorColor = Color.Transparent)
        )
        NavIcon(Icons.Filled.CheckBox, "Tasks", selected == AppTab.TASKS) { onTabSelected(AppTab.TASKS) }
        NavIcon(Icons.Filled.Timer, "Timer", selected == AppTab.STATS) { onTabSelected(AppTab.STATS) }
    }
}

@Composable
private fun RowScope.NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val view = LocalView.current
    val scale = animateFloatAsState(if (selected) 1.10f else 1f, label = "nav-scale").value
    NavigationBarItem(
        selected = selected,
        onClick = { if (!selected) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); onClick() },
        icon = { Icon(icon, label, tint = if (selected) AccentBlue else TextMuted, modifier = Modifier.size(JeeSizes.iconLarge).graphicsLayer(scaleX = scale, scaleY = scale)) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentBlue, unselectedIconColor = TextMuted, selectedTextColor = AccentBlueLight, unselectedTextColor = TextMuted, indicatorColor = AccentBlueSoft)
    )
}
