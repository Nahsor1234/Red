package com.example.jeecommandcenter.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip as MaterialFilterChip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.theme.*

enum class AppTab { HOME, SYLLABUS, TASKS, STATS }

@Composable
fun JeeCard(modifier: Modifier = Modifier, featured: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val shape = if (featured) JeeShapes.large else JeeShapes.medium
    Column(modifier.fillMaxWidth().clip(shape).background(if (featured) BgCardAlt else BgCard).border(1.dp, BgCardBorder.copy(alpha = if (featured) .9f else .72f), shape).padding(16.dp), content = content)
}

@Composable
fun LinearStatBar(progress: Float, modifier: Modifier = Modifier, trackColor: Color = BgDivider, fillColor: Color = AccentBlue, height: androidx.compose.ui.unit.Dp = 5.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(320), label = "progress")
    Box(modifier.fillMaxWidth().height(height).clip(JeeShapes.pill).background(trackColor)) { Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(JeeShapes.pill).background(fillColor)) }
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onActionClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleMedium); if (actionLabel != null) Text(actionLabel, color = AccentBlueLight, fontSize = 12.sp, modifier = Modifier.premiumClick { onActionClick() }) }
}

data class TaskItem(val id: String, val title: String, val subtitle: String, val duration: String, val done: Boolean = false, val inProgress: Boolean = false)

@Composable
fun TaskRow(task: TaskItem, onToggle: (String) -> Unit, onClick: () -> Unit = {}) {
    val alpha by animateFloatAsState(if (task.done) .62f else 1f, label = "task-alpha")
    val check by animateColorAsState(if (task.done) AccentGreen else Color.Transparent, label = "task-check")
    val scale by animateFloatAsState(if (task.done) .985f else 1f, animationSpec = tween(180), label = "task-scale")
    Row(Modifier.fillMaxWidth().graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale).premiumClick(onClick = onClick).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(check).border(1.dp, if (task.done) AccentGreen else BgCardBorder, RoundedCornerShape(7.dp)).premiumClick(haptic = if (!task.done) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.VIRTUAL_KEY) { onToggle(task.id) }, Alignment.Center) { if (task.done) Icon(Icons.Filled.Check, null, tint = AccentGreenDark, modifier = Modifier.size(14.dp)) }
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(task.title, color = TextOnCard, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None); if (task.subtitle.isNotEmpty()) Text(task.subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall) }; Text(task.duration, color = TextMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) { MaterialFilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal) }, shape = JeeShapes.pill, colors = FilterChipDefaults.filterChipColors(containerColor = BgCard, labelColor = TextSecondary, selectedContainerColor = AccentBlue, selectedLabelColor = Color(0xFF17120A))) }

@Composable
fun BottomNavBar(selected: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit) {
    val view = LocalView.current
    NavigationBar(modifier = Modifier.height(64.dp), containerColor = BgCard, tonalElevation = 0.dp, windowInsets = NavigationBarDefaults.windowInsets) {
        NavIcon(Icons.Filled.Home, "Home", selected == AppTab.HOME) { onTabSelected(AppTab.HOME) }
        NavIcon(Icons.Filled.MenuBook, "Syllabus", selected == AppTab.SYLLABUS) { onTabSelected(AppTab.SYLLABUS) }
        NavigationBarItem(selected = false, onClick = { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); onAiClick() }, icon = {
            val pulse by rememberInfiniteTransition(label = "ai-pulse").animateFloat(0.96f, 1.04f, androidx.compose.animation.core.infiniteRepeatable(tween(900), androidx.compose.animation.core.RepeatMode.Reverse), label = "ai-scale")
            Box(Modifier.size(42.dp).graphicsLayer(scaleX = pulse, scaleY = pulse).clip(CircleShape).background(AccentBlue), Alignment.Center) { Icon(Icons.Filled.AutoAwesome, "AI", tint = Color(0xFF17120A), modifier = Modifier.size(20.dp)) }
        }, label = { Text("AI", fontSize = 9.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentBlue, unselectedIconColor = AccentBlue, selectedTextColor = AccentBlueLight, unselectedTextColor = TextSecondary, indicatorColor = Color.Transparent))
        NavIcon(Icons.Filled.CheckCircle, "Tasks", selected == AppTab.TASKS) { onTabSelected(AppTab.TASKS) }
        NavIcon(Icons.Filled.Timer, "Timer", selected == AppTab.STATS) { onTabSelected(AppTab.STATS) }
    }
}

@Composable
private fun RowScope.NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val view = LocalView.current
    val scale by animateFloatAsState(if (selected) 1.08f else 1f, animationSpec = tween(180), label = "nav-scale")
    NavigationBarItem(selected = selected, onClick = { if (!selected) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); onClick() }, icon = { Icon(icon, label, tint = if (selected) AccentBlue else TextMuted, modifier = Modifier.size(21.dp).graphicsLayer(scaleX = scale, scaleY = scale)) }, label = { Text(label, fontSize = 9.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentBlue, unselectedIconColor = TextMuted, selectedTextColor = AccentBlueLight, unselectedTextColor = TextSecondary, indicatorColor = AccentBlueSoft))
}
