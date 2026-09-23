package com.example.jeecommandcenter.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.theme.*
import kotlin.math.abs

private val PatternSymbols = listOf("π", "Σ", "√", "∫", "Δ", "θ", "λ", "μ", "→", "↗", "∇", "∞", "α", "β", "γ", "Ω", "∂", "≈", "≠", "x²", "F=ma", "PV=nRT", "E=mc²")

@Composable
fun JeeBackground(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Canvas(modifier.fillMaxSize()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.White.copy(alpha = 0.055f).toArgb(); textSize = with(density) { 10.dp.toPx() }; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
        val stepX = with(density) { 58.dp.toPx() }
        val stepY = with(density) { 48.dp.toPx() }
        val rows = (size.height / stepY).toInt() + 2
        val cols = (size.width / stepX).toInt() + 2
        for (row in 0 until rows) for (col in 0 until cols) {
            val index = abs((row * 31 + col * 17 + row * col * 3) % PatternSymbols.size)
            val xJitter = ((row * 19 + col * 7) % 17) - 8
            val yJitter = ((row * 11 + col * 13) % 15) - 7
            val x = col * stepX + with(density) { xJitter.dp.toPx() }
            val y = row * stepY + with(density) { yJitter.dp.toPx() }
            canvas.nativeCanvas.drawText(PatternSymbols[index], x, y, textPaint)
            if ((row + col) % 4 == 0) {
                val gx = x + with(density) { 22.dp.toPx() }; val gy = y - with(density) { 4.dp.toPx() }
                drawLine(Color.White.copy(alpha = 0.035f), androidx.compose.ui.geometry.Offset(gx, gy), androidx.compose.ui.geometry.Offset(gx + with(density) { 10.dp.toPx() }, gy), strokeWidth = with(density) { 1.dp.toPx() })
                drawLine(Color.White.copy(alpha = 0.035f), androidx.compose.ui.geometry.Offset(gx, gy), androidx.compose.ui.geometry.Offset(gx, gy - with(density) { 9.dp.toPx() }), strokeWidth = with(density) { 1.dp.toPx() })
            }
        }
    }
}

enum class AppTab { HOME, SYLLABUS, TASKS, STATS }

@Composable
fun JeeCard(modifier: Modifier = Modifier, featured: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val shape = if (featured) JeeShapes.large else JeeShapes.medium
    Column(modifier.fillMaxWidth().clip(shape).background(if (featured) BgCardAlt else BgCard).border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = if (featured) JeeSurfaceTokens.featuredBorderAlpha else JeeSurfaceTokens.cardBorderAlpha), shape).padding(16.dp), content = content)
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
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(task.title, color = TextOnCard, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None); if (task.subtitle.isNotEmpty()) Text(task.subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall) }
        Text(task.duration, color = TextMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun JeeFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal) }, shape = JeeShapes.pill, colors = FilterChipDefaults.filterChipColors(containerColor = BgCard, labelColor = TextSecondary, selectedContainerColor = AccentBlue, selectedLabelColor = Color(0xFF17120A)))
}

@Composable
fun BottomNavBar(selected: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit) {
    val view = LocalView.current
    val items = listOf(AppTab.HOME to (Icons.Filled.Home to "Home"), AppTab.SYLLABUS to (Icons.Filled.MenuBook to "Syllabus"), AppTab.TASKS to (Icons.Filled.CheckCircle to "Tasks"), AppTab.STATS to (Icons.Filled.Timer to "Timer"))
    val selectedIndex = items.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(24.dp)).background(BgCard).border(1.dp, BgCardBorder.copy(alpha = .9f), RoundedCornerShape(24.dp)).padding(5.dp)) {
            val slotWidth = maxWidth / 5
            val indicatorX by animateDpAsState(slotWidth * selectedIndex, animationSpec = tween(280), label = "nav-indicator-x")
            Box(Modifier.offset(x = indicatorX).width(slotWidth).fillMaxHeight().clip(RoundedCornerShape(19.dp)).background(BgCardAlt).border(1.dp, BgCardBorder.copy(alpha = .8f), RoundedCornerShape(19.dp)))
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                NavDestination(items[0].second.first, items[0].second.second, selected == AppTab.HOME, Modifier.weight(1f)) { onTabSelected(AppTab.HOME) }
                NavDestination(items[1].second.first, items[1].second.second, selected == AppTab.SYLLABUS, Modifier.weight(1f)) { onTabSelected(AppTab.SYLLABUS) }
                AiDestination(Modifier.weight(1f), view, onAiClick)
                NavDestination(items[2].second.first, items[2].second.second, selected == AppTab.TASKS, Modifier.weight(1f)) { onTabSelected(AppTab.TASKS) }
                NavDestination(items[3].second.first, items[3].second.second, selected == AppTab.STATS, Modifier.weight(1f)) { onTabSelected(AppTab.STATS) }
            }
        }
    }
}

@Composable
private fun RowScope.NavDestination(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val view = LocalView.current
    val contentScale by animateFloatAsState(if (selected) 1.04f else 1f, animationSpec = tween(220), label = "nav-content-scale")
    Box(modifier.fillMaxHeight().premiumClick { if (!selected) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); onClick() }, contentAlignment = Alignment.Center) { Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer(scaleX = contentScale, scaleY = contentScale)) { Icon(icon, label, tint = if (selected) AccentBlueLight else TextSecondary, modifier = Modifier.size(22.dp)); if (selected) { Spacer(Modifier.width(6.dp)); Text(label, color = AccentBlueLight, fontSize = 11.sp, fontWeight = FontWeight.Medium) } } }
}

@Composable
private fun RowScope.AiDestination(modifier: Modifier, view: android.view.View, onClick: () -> Unit) {
    val scale by animateFloatAsState(1f, animationSpec = tween(220), label = "ai-nav-scale")
    Box(modifier.fillMaxHeight().premiumClick { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); onClick() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.AutoAwesome, "AI", tint = AccentBlueLight, modifier = Modifier.size(23.dp).graphicsLayer(scaleX = scale, scaleY = scale)) }
}
