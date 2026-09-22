package com.example.jeecommandcenter.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.theme.*

enum class AppTab { HOME, SYLLABUS, TASKS, STATS }

@Composable
fun LinearStatBar(progress: Float, modifier: Modifier = Modifier, trackColor: Color = BgDivider, fillColor: Color = AccentBlue, height: androidx.compose.ui.unit.Dp = 5.dp) {
    Box(modifier = modifier.fillMaxWidth().height(height).clip(JeeShapes.pill).background(trackColor)) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animateFloatAsState(progress.coerceIn(0f, 1f), label = "linear-progress").value).clip(JeeShapes.pill).background(fillColor))
    }
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onActionClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null) Text(actionLabel, color = AccentBlue, fontSize = 12.sp, modifier = Modifier.premiumClick { onActionClick() })
    }
}

data class TaskItem(val id: String, val title: String, val subtitle: String, val duration: String, val done: Boolean = false, val inProgress: Boolean = false)

@Composable
fun TaskRow(task: TaskItem, onToggle: (String) -> Unit) {
    val scale = animateFloatAsState(if (task.done) 1f else 0.98f, label = "task-scale").value
    val checkColor = animateColorAsState(if (task.done) AccentGreen else Color.Transparent, label = "task-state").value
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = JeeSpacing.sm).graphicsLayer(scaleX = scale, scaleY = scale), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f).premiumClick(onClick = { onToggle(task.id) }), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(checkColor), contentAlignment = Alignment.Center) {
                AnimatedContent(targetState = task.done, label = "task-check") { done -> if (done) Icon(Icons.Filled.CheckBox, "Completed", tint = AccentGreenDark, modifier = Modifier.size(JeeSizes.iconSmall)) }
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
    Box(modifier = Modifier.clip(JeeShapes.pill).background(if (selected) ChipSelectedBg else ChipUnselectedBg).sizeIn(minHeight = 40.dp).premiumClick { onClick() }.padding(horizontal = JeeSpacing.lg, vertical = JeeSpacing.sm), contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) Color.White else TextSecondary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
}

@Composable
fun BottomNavBar(selected: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "ai-pulse").animateFloat(1f, 1.06f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "ai-scale")
    Row(modifier = Modifier.fillMaxWidth().background(BgApp).navigationBarsPadding().padding(top = JeeSpacing.md, bottom = JeeSpacing.xs), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        NavIcon(Icons.Filled.Home, "Home", selected == AppTab.HOME) { onTabSelected(AppTab.HOME) }
        NavIcon(Icons.Filled.MenuBook, "Syllabus", selected == AppTab.SYLLABUS) { onTabSelected(AppTab.SYLLABUS) }
        Box(modifier = Modifier.size(JeeSizes.navButton).clip(CircleShape).background(AccentBlue).premiumClick(onAiClick).graphicsLayer(scaleX = pulse, scaleY = pulse), contentAlignment = Alignment.Center) { Icon(Icons.Filled.AutoAwesome, "Open AI Hub", tint = Color.White) }
        NavIcon(Icons.Filled.CheckBox, "Tasks", selected == AppTab.TASKS) { onTabSelected(AppTab.TASKS) }
        NavIcon(Icons.Filled.Timer, "Timer", selected == AppTab.STATS) { onTabSelected(AppTab.STATS) }
    }
}

@Composable
private fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val scale = animateFloatAsState(if (selected) 1.12f else 1f, label = "nav-scale").value
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = JeeSizes.navButton).premiumClick(onClick)) {
        Icon(icon, label, tint = if (selected) AccentBlue else TextMuted, modifier = Modifier.size(JeeSizes.icon).graphicsLayer(scaleX = scale, scaleY = scale))
        Spacer(Modifier.height(JeeSpacing.xs))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) AccentBlue else TextMuted)
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.AutoAwesome, "Empty", tint = AccentBlue, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun LoadingState(message: String = "Loading…", modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = AccentBlue)
        Spacer(Modifier.height(10.dp))
        Text(message, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.WarningAmber, "Error", tint = AccentAmber, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(8.dp))
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
        Text(message, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
