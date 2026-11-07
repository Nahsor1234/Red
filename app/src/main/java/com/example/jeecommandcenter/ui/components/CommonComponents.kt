package com.example.jeecommandcenter.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.ui.theme.BgCard
import com.example.jeecommandcenter.ui.theme.BgCardAlt
import com.example.jeecommandcenter.ui.theme.BgDivider
import com.example.jeecommandcenter.ui.theme.JeeShapes
import com.example.jeecommandcenter.ui.theme.OnPrimary
import com.example.jeecommandcenter.ui.theme.OnSuccess
import com.example.jeecommandcenter.ui.theme.OutlineStrong
import com.example.jeecommandcenter.ui.theme.Primary
import com.example.jeecommandcenter.ui.theme.PrimaryLight
import com.example.jeecommandcenter.ui.theme.Success
import com.example.jeecommandcenter.ui.theme.TextMuted
import com.example.jeecommandcenter.ui.theme.TextPrimary
import com.example.jeecommandcenter.ui.theme.TextSecondary

/** Standard content container. [featured] raises the surface and warms the border. */
@Composable
fun JeeCard(
    modifier: Modifier = Modifier,
    featured: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = if (featured) JeeShapes.large else JeeShapes.medium
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (featured) BgCardAlt else BgCard)
            .padding(16.dp),
        content = content
    )
}

/** Progress track. The animated value is read in the draw phase so animation never re-lays out. */
@Composable
fun LinearStatBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = BgDivider,
    fillColor: Color = Primary,
    height: Dp = 6.dp
) {
    val animated = animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(320), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(JeeShapes.pill)
            .background(trackColor)
            .drawBehind {
                val width = size.width * animated.value
                if (width > 0f) {
                    drawRoundRect(fillColor, size = Size(width, size.height), cornerRadius = CornerRadius(size.height / 2f))
                }
            }
    )
}

/** Circular progress. The animated sweep is read in the draw phase, so it never re-lays out. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 96.dp,
    strokeWidth: Dp = 9.dp,
    trackColor: Color = BgDivider,
    color: Color = Primary,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animated = animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(600), label = "ring")
    Box(
        modifier
            .size(diameter)
            .drawBehind {
                val stroke = strokeWidth.toPx()
                val inset = stroke / 2f
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                if (animated.value > 0f) {
                    drawArc(color, -90f, 360f * animated.value, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onActionClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryLight,
                modifier = Modifier
                    .clip(JeeShapes.small)
                    .premiumClick { onActionClick() }
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

data class TaskItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val duration: String,
    val done: Boolean = false
)

/** Checkbox with a 44dp touch target around a 22dp visual box. */
@Composable
fun TaskCheckbox(done: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val fill by animateColorAsState(if (done) Success else Color.Transparent, label = "task-check")
    Box(
        modifier
            .size(44.dp)
            .clip(JeeShapes.small)
            .semantics { stateDescription = if (done) "Completed" else "Not completed" }
            .premiumClick(
                haptic = if (!done) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.VIRTUAL_KEY,
                role = Role.Checkbox,
                onClick = onToggle
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(fill)
                .border(1.5.dp, if (done) Success else OutlineStrong, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = OnSuccess, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
fun TaskRow(task: TaskItem, onToggle: (String) -> Unit, onClick: () -> Unit = {}) {
    val rowAlpha by animateFloatAsState(if (task.done) .62f else 1f, label = "task-alpha")
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = rowAlpha }
            .premiumClick(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaskCheckbox(task.done) { onToggle(task.id) }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                task.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
            )
            if (task.subtitle.isNotEmpty()) Text(task.subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Text(task.duration, style = MaterialTheme.typography.labelMedium, color = TextMuted, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun JeeFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = JeeShapes.pill,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = BgCard,
            labelColor = TextSecondary,
            selectedContainerColor = Primary,
            selectedLabelColor = OnPrimary
        )
    )
}
