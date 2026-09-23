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
            drawContext.canvas.nativeCanvas.drawText(PatternSymbols[index], x, y, textPaint)
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