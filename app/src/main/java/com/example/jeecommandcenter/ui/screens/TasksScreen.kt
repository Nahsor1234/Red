package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

data class TaskGroup(val title: String, val count: Int, val tasks: List<TaskDetail>)
data class TaskDetail(val icon: ImageVector, val iconBg: Color, val iconTint: Color, val title: String, val subject: String, val duration: String, val done: Boolean = false)

private val taskGroups = listOf(
    TaskGroup("Today", 3, listOf(
        TaskDetail(Icons.Filled.MenuBook, AccentGreen.copy(alpha = 0.2f), AccentGreen, "Study kinematics theory", "Physics", "1h", done = true),
        TaskDetail(Icons.Filled.Edit, BgDivider, TextSecondary, "Solve 30 questions", "Physics", "45m"),
        TaskDetail(Icons.Filled.Refresh, BgDivider, TextSecondary, "Revise formula sheet", "Physics", "30m")
    )),
    TaskGroup("Tomorrow", 2, listOf(
        TaskDetail(Icons.Filled.Science, AccentPink.copy(alpha = 0.2f), AccentPink, "Chemical bonding notes", "Chemistry", "1h"),
        TaskDetail(Icons.Filled.Functions, AccentGreen.copy(alpha = 0.2f), AccentGreen, "Quadratic equations practice", "Mathematics", "1h")
    )),
    TaskGroup("Upcoming", 2, listOf(
        TaskDetail(Icons.Filled.TrackChanges, AccentBlue.copy(alpha = 0.2f), AccentBlue, "Laws of motion questions", "Physics", "1h"),
        TaskDetail(Icons.Filled.Schedule, AccentAmber.copy(alpha = 0.2f), AccentAmber, "Periodic table revision", "Chemistry", "45m")
    ))
)

@Composable
fun TasksScreen(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, onFabClick: () -> Unit = {}) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Today", "Upcoming", "Completed")
    Scaffold(
        containerColor = BgApp,
        bottomBar = { BottomNavBar(selectedTab, onTabSelected, onFabClick) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { }, containerColor = AccentBlue, contentColor = Color.White) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add task")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Tasks", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp))
                Row {
                    Icon(Icons.Filled.Search, "Search", tint = TextSecondary)
                    Spacer(Modifier.width(14.dp))
                    Icon(Icons.Filled.MoreVert, "More", tint = TextSecondary)
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter -> FilterChip(filter, filter == selectedFilter) { selectedFilter = filter } }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                taskGroups.forEach { group ->
                    item { Text(group.title + " · " + group.count + " tasks", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp)) }
                    items(group.tasks) { task -> TaskCard(task) }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskDetail) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(14.dp)).background(BgCard).padding(12.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(task.iconBg), contentAlignment = Alignment.Center) {
                Icon(task.icon, null, tint = task.iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(task.title, color = TextOnCard, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(task.subject + " · " + task.duration, color = TextMuted, fontSize = 11.sp)
            }
        }
        Icon(Icons.Filled.MoreVert, "Task options", tint = TextMuted, modifier = Modifier.size(16.dp))
    }
}