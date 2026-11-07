package com.example.jeecommandcenter.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.ui.theme.BgCard
import com.example.jeecommandcenter.ui.theme.BgCardAlt
import com.example.jeecommandcenter.ui.theme.BgCardBorder
import com.example.jeecommandcenter.ui.theme.BgDivider
import com.example.jeecommandcenter.ui.theme.JeeShapes
import com.example.jeecommandcenter.ui.theme.JeeSurfaceTokens
import com.example.jeecommandcenter.ui.theme.OnPrimary
import com.example.jeecommandcenter.ui.theme.OnSuccess
import com.example.jeecommandcenter.ui.theme.Primary
import com.example.jeecommandcenter.ui.theme.PrimaryLight
import com.example.jeecommandcenter.ui.theme.PrimarySoft
import com.example.jeecommandcenter.ui.theme.Success
import com.example.jeecommandcenter.ui.theme.TextMuted
import com.example.jeecommandcenter.ui.theme.TextPrimary
import com.example.jeecommandcenter.ui.theme.TextSecondary

/** Top-level destinations. STATS is the study timer. */
enum class AppTab(val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    SYLLABUS("Syllabus", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    AI("AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    TASKS("Tasks", Icons.Filled.AssignmentTurnedIn, Icons.Outlined.AssignmentTurnedIn),
    STATS("Timer", Icons.Filled.Timer, Icons.Outlined.Timer)
}

/** Standard content container. [featured] raises the surface and warms the border. */
@Composable
fun JeeCard(
    modifier: Modifier = Modifier,
    featured: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = if (featured) JeeShapes.large else JeeShapes.medium
    val border = if (featured) Primary.copy(alpha = 0.35f) else BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (featured) BgCardAlt else BgCard)
            .border(JeeSurfaceTokens.borderWidth, border, shape)
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
    val done: Boolean = false,
    val inProgress: Boolean = false
)

@Composable
fun TaskRow(task: TaskItem, onToggle: (String) -> Unit, onClick: () -> Unit = {}) {
    val rowAlpha by animateFloatAsState(if (task.done) .62f else 1f, label = "task-alpha")
    val check by animateColorAsState(if (task.done) Success else Color.Transparent, label = "task-check")
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = rowAlpha }
            .premiumClick(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 44dp touch target around a 22dp visual checkbox.
        Box(
            Modifier
                .size(44.dp)
                .clip(JeeShapes.small)
                .semantics { role = Role.Checkbox }
                .premiumClick(haptic = if (!task.done) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.VIRTUAL_KEY) { onToggle(task.id) },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(check)
                    .border(1.5.dp, if (task.done) Success else BgCardBorder, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (task.done) Icon(Icons.Filled.Check, contentDescription = "Completed", tint = OnSuccess, modifier = Modifier.size(15.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                task.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
            )
            if (task.subtitle.isNotEmpty()) Text(task.subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Text(task.duration, color = TextMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
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

@Composable
fun BottomNavBar(selected: AppTab, onTabSelected: (AppTab) -> Unit) {
    val view = LocalView.current
    NavigationBar(containerColor = BgCard, contentColor = TextSecondary, tonalElevation = 0.dp) {
        AppTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onTabSelected(tab)
                    }
                },
                icon = { Icon(if (isSelected) tab.selectedIcon else tab.icon, contentDescription = null) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryLight,
                    selectedTextColor = PrimaryLight,
                    indicatorColor = PrimarySoft,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
