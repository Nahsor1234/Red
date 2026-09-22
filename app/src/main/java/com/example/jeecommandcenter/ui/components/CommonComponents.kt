package com.example.jeecommandcenter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.theme.*

enum class AppTab { HOME, SYLLABUS, TASKS, STATS }

@Composable
fun LinearStatBar(progress: Float, modifier: Modifier = Modifier, trackColor: Color = BgDivider, fillColor: Color = AccentBlue, height: androidx.compose.ui.unit.Dp = 5.dp) {
    Box(modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(trackColor)) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).clip(RoundedCornerShape(50)).background(fillColor))
    }
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onActionClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null) Text(actionLabel, color = AccentBlue, fontSize = 12.sp, modifier = Modifier.clickable { onActionClick() })
    }
}

data class TaskItem(val id: String, val title: String, val subtitle: String, val duration: String, val done: Boolean = false, val inProgress: Boolean = false)

@Composable
fun TaskRow(task: TaskItem, onToggle: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(if (task.done) AccentGreen else Color.Transparent).clickable { onToggle(task.id) }, contentAlignment = Alignment.Center) {
                if (task.done) Icon(Icons.Filled.CheckBox, contentDescription = "Completed", tint = AccentGreenDark, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(task.title, color = if (task.done) TextMuted else TextOnCard, fontSize = 13.sp, textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None)
                if (task.subtitle.isNotEmpty()) Text(task.subtitle, color = TextMuted, fontSize = 11.sp)
            }
        }
        Text(task.duration, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (selected) ChipSelectedBg else ChipUnselectedBg).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, color = if (selected) Color.White else TextSecondary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
}

@Composable
fun BottomNavBar(selected: AppTab, onTabSelected: (AppTab) -> Unit, onFabClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(BgApp).padding(top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        NavIcon(Icons.Filled.Home, "Home", selected == AppTab.HOME) { onTabSelected(AppTab.HOME) }
        NavIcon(Icons.Filled.MenuBook, "Syllabus", selected == AppTab.SYLLABUS) { onTabSelected(AppTab.SYLLABUS) }
        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(AccentBlue).clickable { onFabClick() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, contentDescription = "Add task", tint = Color.White)
        }
        NavIcon(Icons.Filled.CheckBox, "Tasks", selected == AppTab.TASKS) { onTabSelected(AppTab.TASKS) }
        NavIcon(Icons.Filled.BarChart, "Stats", selected == AppTab.STATS) { onTabSelected(AppTab.STATS) }
    }
}

@Composable
private fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Icon(icon, contentDescription = label, tint = if (selected) AccentBlue else TextMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 9.sp, color = if (selected) AccentBlue else TextMuted)
    }
}