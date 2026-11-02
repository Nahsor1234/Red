package com.example.jeecommandcenter.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun JeeBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(BgAppBase, Color(0xFFF0F4FB)), endY = size.height))
        val glowRadius = size.width * 0.76f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD5B8).copy(alpha = 0.24f), Color.Transparent),
                center = Offset(size.width * 0.94f, size.height * 0.06f),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(size.width * 0.94f, size.height * 0.06f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFBBD0FF).copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.02f, size.height * 0.78f),
                radius = glowRadius * 0.78f
            ),
            radius = glowRadius * 0.78f,
            center = Offset(size.width * 0.02f, size.height * 0.78f)
        )
    }
}

enum class AppTab { HOME, SYLLABUS, AI, TASKS, STATS }

@Composable
fun JeeCard(modifier: Modifier = Modifier, featured: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val shape = if (featured) JeeShapes.large else JeeShapes.medium
    val surfaceBrush = if (featured) {
        Brush.linearGradient(listOf(Color.White, BgFeature))
    } else {
        Brush.linearGradient(listOf(BgCard, BgCard))
    }
    Column(
        modifier.fillMaxWidth()
            .shadow(if (featured) 12.dp else 4.dp, shape, clip = false)
            .clip(shape)
            .background(surfaceBrush)
            .border(
                JeeSurfaceTokens.borderWidth,
                if (featured) BorderFeature else BgCardBorder,
                shape
            )
            .padding(if (featured) JeeSpacing.xl else JeeSpacing.lg),
        content = content
    )
}
@Composable fun LinearStatBar(progress: Float, modifier: Modifier = Modifier, trackColor: Color = BgDivider, fillColor: Color = Primary, height: androidx.compose.ui.unit.Dp = 5.dp) { val p by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(320), label = "progress"); Box(modifier.fillMaxWidth().height(height).clip(JeeShapes.pill).background(trackColor).semantics { progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f) }) { Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(JeeShapes.pill).background(fillColor)) } }
@Composable fun SectionHeader(title: String, actionLabel: String? = null, onActionClick: () -> Unit = {}) { Row(Modifier.fillMaxWidth().padding(vertical = JeeSpacing.sm), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleMedium); if (actionLabel != null) Text(actionLabel, style = MaterialTheme.typography.labelLarge.copy(color = PrimaryLight), modifier = Modifier.semantics { role = Role.Button }.premiumClick { onActionClick() }) } }

@Composable
fun JeePageHeader(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            eyebrow?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = PrimaryLight, letterSpacing = 1.sp)
            }
            Text(title, style = MaterialTheme.typography.headlineLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        trailing?.invoke()
    }
}
data class TaskItem(val id: String, val title: String, val subtitle: String, val duration: String, val done: Boolean = false, val inProgress: Boolean = false)
@Composable
fun TaskRow(
    task: TaskItem,
    onToggle: (String) -> Unit,
    onClick: () -> Unit = {},
    expressiveMotion: Boolean = false
) {
    val alpha by animateFloatAsState(
        if (task.done) 0.62f else 1f,
        animationSpec = tween(300),
        label = "task-alpha"
    )
    val check by animateColorAsState(
        if (task.done) Success else Color.Transparent,
        animationSpec = tween(300),
        label = "task-check"
    )
    val scale by animateFloatAsState(
        if (task.done) 0.985f else 1f,
        animationSpec = tween(300),
        label = "task-scale"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (task.done) 1f else 0.72f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "task-check-scale"
    )
    Row(
        Modifier.fillMaxWidth()
            .graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale)
            .then(if (expressiveMotion) Modifier.animateContentSize() else Modifier)
            .semantics { role = Role.Button }
            .premiumClick(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = if (expressiveMotion) Alignment.Top else Alignment.CenterVertically
    ) {
        Box(
            Modifier.then(if (expressiveMotion) Modifier.padding(top = 2.dp) else Modifier).size(48.dp)
                .semantics {
                    role = Role.Checkbox
                    contentDescription = "Mark ${task.title} complete"
                    stateDescription = if (task.done) "Completed" else "Not completed"
                }
                .premiumClick(
                    haptic = if (!task.done) HapticFeedbackConstants.KEYBOARD_TAP
                    else HapticFeedbackConstants.VIRTUAL_KEY,
                    onClick = { onToggle(task.id) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(20.dp)
                    .clip(JeeShapes.small)
                    .background(check)
                    .border(1.dp, if (task.done) Success else BgCardBorder, JeeShapes.small),
                contentAlignment = Alignment.Center
            ) {
                if (task.done) {
                    Icon(
                        Icons.Filled.Check,
                        null,
                        tint = SuccessOnFill,
                        modifier = Modifier.size(14.dp).graphicsLayer(scaleX = checkScale, scaleY = checkScale)
                    )
                }
            }
        }
        Spacer(Modifier.width(JeeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                color = TextOnCard,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
            )
            if (task.subtitle.isNotEmpty()) {
                Text(task.subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"))
            }
        }
        Text(task.duration, color = TextMuted, style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"))
    }
}

@Composable
fun JeeFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (selected) Primary else BgCardAlt,
        animationSpec = tween(JeeMotion.standardMillis),
        label = "filter-chip-container"
    )
    val foreground by animateColorAsState(
        if (selected) Color.White else TextSecondary,
        animationSpec = tween(JeeMotion.standardMillis),
        label = "filter-chip-foreground"
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = foreground,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            )
        },
        shape = JeeShapes.pill,
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = BgCardBorder,
            selectedBorderColor = Primary.copy(alpha = 0.62f)
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = BgCardAlt,
            labelColor = TextSecondary,
            selectedContainerColor = container,
            selectedLabelColor = foreground
        )
    )
}

private enum class NavVisual { HOME, SYLLABUS, AI, TASKS, TIMER }

@Composable
fun BottomNavBar(selected: AppTab, onTabSelected: (AppTab) -> Unit) {
    val view = LocalView.current
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 7.dp)
            .shadow(14.dp, JeeShapes.pill, clip = false),
        color = BgCard,
        shape = JeeShapes.pill,
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder)
    ) {
        Row(
            Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIconSlot(NavVisual.HOME, selected == AppTab.HOME, Modifier.weight(1f)) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onTabSelected(AppTab.HOME)
            }
            NavIconSlot(NavVisual.SYLLABUS, selected == AppTab.SYLLABUS, Modifier.weight(1f)) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onTabSelected(AppTab.SYLLABUS)
            }
            NavIconSlot(NavVisual.AI, selected == AppTab.AI, Modifier.weight(1f)) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onTabSelected(AppTab.AI)
            }
            NavIconSlot(NavVisual.TASKS, selected == AppTab.TASKS, Modifier.weight(1f)) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onTabSelected(AppTab.TASKS)
            }
            NavIconSlot(NavVisual.TIMER, selected == AppTab.STATS, Modifier.weight(1f)) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onTabSelected(AppTab.STATS)
            }
        }
    }
}

@Composable
fun AnimatedBottomNavBar(selected: AppTab, onTabSelected: (AppTab) -> Unit, visible: Boolean) {
    if (visible) BottomNavBar(selected, onTabSelected)
}

@Composable
private fun RowScope.NavIconSlot(
    visual: NavVisual,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val contentColor by animateColorAsState(
        if (selected) Primary else TextMuted,
        animationSpec = tween(JeeMotion.standardMillis),
        label = "navigation-content-color"
    )
    val indicatorColor by animateColorAsState(
        if (selected) PrimarySoft else Color.Transparent,
        animationSpec = tween(JeeMotion.standardMillis),
        label = "navigation-indicator-color"
    )
    val iconScale by animateFloatAsState(
        if (selected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "navigation-icon-scale"
    )
    val icon = when (visual) {
        NavVisual.HOME -> Icons.Filled.Home
        NavVisual.SYLLABUS -> Icons.Filled.MenuBook
        NavVisual.AI -> Icons.Filled.AutoAwesome
        NavVisual.TASKS -> Icons.Filled.AssignmentTurnedIn
        NavVisual.TIMER -> Icons.Filled.Timer
    }
    val label = when (visual) {
        NavVisual.HOME -> "Home"
        NavVisual.SYLLABUS -> "Syllabus"
        NavVisual.AI -> "AI"
        NavVisual.TASKS -> "Tasks"
        NavVisual.TIMER -> "Focus"
    }

    Column(
        modifier
            .fillMaxHeight()
            .clip(JeeShapes.pill)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 1.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .clip(JeeShapes.pill)
                .background(indicatorColor)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp).graphicsLayer(scaleX = iconScale, scaleY = iconScale)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
    }
}
