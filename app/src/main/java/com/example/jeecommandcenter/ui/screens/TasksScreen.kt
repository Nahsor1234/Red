package com.example.jeecommandcenter.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun TasksScreen(repo: JeeRepository, selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit = {}) {
    var filter by remember { mutableStateOf("All") }; var refresh by remember { mutableIntStateOf(0) }; var showAdd by remember { mutableStateOf(false) }; var selectedTask by remember { mutableStateOf<AppTask?>(null) }
    val all = remember(refresh) { repo.getTasks() }; val visible = all.filter { when (filter) { "Today" -> it.dueDay == "Today"; "Upcoming" -> it.dueDay != "Today" && !it.done; "Completed" -> it.done; else -> true } }; val today = all.filter { it.dueDay == "Today" && !it.done }; val upcoming = all.filter { it.dueDay != "Today" && !it.done }; val completed = all.filter { it.done }
    Box(Modifier.fillMaxSize()) {
        JeeBackground()
        Scaffold(containerColor = BgApp.copy(alpha = 0f), bottomBar = { BottomNavBar(selectedTab, onTabSelected, onAiClick) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Tasks", style = MaterialTheme.typography.headlineSmall); Icon(Icons.Filled.Refresh, "Refresh", tint = TextSecondary, modifier = Modifier.premiumClick { refresh++ }) }
                Spacer(Modifier.height(12.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf("All", "Today", "Upcoming", "Completed")) { value -> JeeFilterChip(value, value == filter) { filter = value } } }; Spacer(Modifier.height(14.dp))
                if (filter != "All") { val count = visible.size; Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BgCardAlt).border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium).padding(17.dp)) { Text(filter.uppercase(), color = TextSecondary, fontSize = 11.sp); Text("$count ${if (count == 1) "task" else "tasks"}", style = MaterialTheme.typography.titleLarge); Text("${visible.sumOf { it.durationMin }} minutes planned", color = TextMuted, fontSize = 11.sp) }; Spacer(Modifier.height(8.dp)) }
                LazyColumn(Modifier.weight(1f)) {
                    if (filter == "All") { taskGroup("Today", today, repo, { selectedTask = it }, { refresh++ }); taskGroup("Upcoming", upcoming, repo, { selectedTask = it }, { refresh++ }); if (completed.isNotEmpty()) taskGroup("Completed", completed, repo, { selectedTask = it }, { refresh++ }); if (all.isEmpty()) emptyTaskState() }
                    else { if (visible.isEmpty()) emptyTaskState() else visible.forEach { task -> item(key = task.id) { TaskListItem(task, repo, { selectedTask = it }, { refresh++ }) } } }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Icon(Icons.Filled.Add, null, tint = Color(0xFF17120A)); Spacer(Modifier.width(7.dp)); Text("Add task", color = Color(0xFF17120A)) }; Spacer(Modifier.height(8.dp))
            }
        }
    }
    if (showAdd) AddTaskDialog(repo) { showAdd = false; refresh++ }
    selectedTask?.let { task -> AlertDialog(onDismissRequest = { selectedTask = null }, title = { Text(task.title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("${task.subject} · ${task.durationMin} min", color = TextSecondary); Text("${task.activityType.name.lowercase().replace('_', ' ')} · ${task.dueDay}", color = TextMuted, fontSize = 12.sp); Text(if (task.done) "Completed" else "Not completed", color = if (task.done) AccentGreen else TextSecondary, fontSize = 12.sp) } }, confirmButton = { TextButton(onClick = { repo.toggleTask(task.id); selectedTask = null; refresh++ }) { Text(if (task.done) "Mark incomplete" else "Complete") } }, dismissButton = { Row { TextButton(onClick = { repo.deleteTask(task.id); selectedTask = null; refresh++ }) { Text("Delete", color = Danger) }; TextButton(onClick = { selectedTask = null }) { Text("Close") } } }) }
}

private fun LazyListScope.taskGroup(title: String, tasks: List<AppTask>, repo: JeeRepository, onOpen: (AppTask) -> Unit, onRefresh: () -> Unit) { if (tasks.isEmpty()) return; item(key = "header_$title") { Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.width(6.dp)); Text("· ${tasks.size}", color = TextMuted, fontSize = 11.sp) } }; items(tasks, key = { it.id }) { task -> TaskListItem(task, repo, onOpen, onRefresh) } }

@Composable private fun TaskListItem(task: AppTask, repo: JeeRepository, onOpen: (AppTask) -> Unit, onRefresh: () -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).border(1.dp, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), RoundedCornerShape(16.dp)).premiumClick { onOpen(task) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(23.dp).clip(RoundedCornerShape(7.dp)).background(if (task.done) AccentGreen else Color.Transparent).premiumClick(haptic = HapticFeedbackConstants.KEYBOARD_TAP) { repo.toggleTask(task.id); onRefresh() }, Alignment.Center) { if (task.done) Icon(Icons.Filled.Check, null, tint = AccentGreenDark, modifier = Modifier.size(14.dp)) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(task.title, color = if (task.done) TextMuted else TextOnCard, fontSize = 15.sp); Row(verticalAlignment = Alignment.CenterVertically) { Text("${task.subject} · ${task.durationMin}m", color = TextMuted, fontSize = 11.sp); Spacer(Modifier.width(7.dp)); Box(Modifier.clip(RoundedCornerShape(6.dp)).background(AccentBlueSoft).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(task.activityType.name.lowercase().replace('_', ' '), color = AccentBlueLight, fontSize = 9.sp, fontWeight = FontWeight.Medium) } } }; Icon(Icons.Filled.ChevronRight, "Open task", tint = AccentBlue, modifier = Modifier.size(19.dp)) } }

private fun LazyListScope.emptyTaskState() { item { Column(Modifier.fillMaxWidth().padding(vertical = 62.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Nothing here yet", style = MaterialTheme.typography.titleMedium); Text("Add a task when you know what you want to study.", color = TextMuted, fontSize = 12.sp) } } }

@Composable
private fun AddTaskDialog(repo: JeeRepository, onDone: () -> Unit) {
    val subjects = listOf("Physics", "Chemistry", "Mathematics"); var title by remember { mutableStateOf("") }; var subject by remember { mutableStateOf("Physics") }; var duration by remember { mutableStateOf("30") }; var due by remember { mutableStateOf("Today") }; var activity by remember { mutableStateOf(ActivityType.REVISION) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDone) {
        Column(Modifier.fillMaxWidth().widthIn(max = 420.dp).wrapContentHeight().clip(RoundedCornerShape(24.dp)).background(BgCardAlt).border(1.dp, BgCardBorder.copy(alpha = .9f), RoundedCornerShape(24.dp)).padding(horizontal = 20.dp, vertical = 17.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Add task", style = MaterialTheme.typography.headlineSmall); Text("Create a focused study block", color = TextMuted, fontSize = 11.sp) }; IconButton(onClick = onDone, modifier = Modifier.size(40.dp)) { Icon(Icons.Filled.Close, "Close") } }
            Spacer(Modifier.height(12.dp)); Text("Task", color = TextSecondary, fontSize = 12.sp); Spacer(Modifier.height(5.dp)); OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("e.g. Revise electrostatics") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(11.dp)); Text("Subject", color = TextSecondary, fontSize = 12.sp); Spacer(Modifier.height(5.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(subjects) { value -> JeeFilterChip(value, value == subject) { subject = value } } }
            Spacer(Modifier.height(11.dp)); Text("Activity", color = TextSecondary, fontSize = 12.sp); Spacer(Modifier.height(5.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(listOf(ActivityType.LEARNING, ActivityType.PRACTICE, ActivityType.REVISION, ActivityType.TEST)) { type -> JeeFilterChip(type.name.lowercase().replace('_', ' '), type == activity) { activity = type } } }
            Spacer(Modifier.height(11.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) { Text("Duration", color = TextSecondary, fontSize = 12.sp); Spacer(Modifier.height(5.dp)); OutlinedTextField(value = duration, onValueChange = { duration = it.filter(Char::isDigit).take(4) }, suffix = { Text("min") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) }
                Column(Modifier.weight(1f)) { Text("Schedule", color = TextSecondary, fontSize = 12.sp); Spacer(Modifier.height(5.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { JeeFilterChip("Today", due == "Today") { due = "Today" }; JeeFilterChip("Later", due == "Upcoming") { due = "Upcoming" } } }
            }
            Spacer(Modifier.height(13.dp)); HorizontalDivider(color = BgDivider.copy(alpha = .55f)); Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDone) { Text("Cancel") }; Spacer(Modifier.width(4.dp)); val minutes = duration.toIntOrNull(); Button(enabled = title.isNotBlank() && minutes != null && minutes in 1..1440, onClick = { minutes?.let { repo.addTask(title.trim(), subject, it, due, null, activity); onDone() } }, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Add task") }
            }
        }
    }
}
