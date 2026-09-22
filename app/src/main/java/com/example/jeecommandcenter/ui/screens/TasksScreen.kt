package com.example.jeecommandcenter.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun TasksScreen(repo: JeeRepository, selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit = {}) {
    var filter by remember { mutableStateOf("All") }
    var refresh by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<AppTask?>(null) }
    val all = remember(refresh) { repo.getTasks() }
    val visible = all.filter {
        when (filter) {
            "Today" -> it.dueDay == "Today"
            "Upcoming" -> it.dueDay != "Today" && !it.done
            "Completed" -> it.done
            else -> true
        }
    }
    val today = all.filter { it.dueDay == "Today" && !it.done }
    val upcoming = all.filter { it.dueDay != "Today" && !it.done }
    val completed = all.filter { it.done }
    val view = LocalView.current

    Scaffold(containerColor = BgApp, bottomBar = { BottomNavBar(selectedTab, onTabSelected, onAiClick) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Tasks", style = MaterialTheme.typography.headlineSmall)
                Icon(Icons.Filled.Refresh, "Refresh", tint = TextSecondary, modifier = Modifier.premiumClick { refresh++ })
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Today", "Upcoming", "Completed")) { value -> FilterChip(value, value == filter) { filter = value } }
            }
            Spacer(Modifier.height(14.dp))
            if (filter != "All") {
                val count = visible.size
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BgCardAlt).padding(17.dp)) {
                    Text(filter.uppercase(), color = TextSecondary, fontSize = 11.sp)
                    Text("$count ${if (count == 1) "task" else "tasks"}", style = MaterialTheme.typography.titleLarge)
                    Text("${visible.sumOf { it.durationMin }} minutes planned", color = TextMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
            LazyColumn(Modifier.weight(1f)) {
                if (filter == "All") {
                    taskGroup("Today", today, repo, view, { selectedTask = it })
                    taskGroup("Upcoming", upcoming, repo, view, { selectedTask = it })
                    if (completed.isNotEmpty()) taskGroup("Completed", completed, repo, view, { selectedTask = it })
                    if (all.isEmpty()) emptyTaskState()
                } else {
                    if (visible.isEmpty()) emptyTaskState()
                    else visible.forEach { task ->
                        item(key = task.id) { TaskListItem(task, repo, view) { selectedTask = task } }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            Button(
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Icon(Icons.Filled.Add, null, tint = Color(0xFF17120A)); Spacer(Modifier.width(7.dp)); Text("Add task", color = Color(0xFF17120A))
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAdd) AddTaskDialogPreview(repo) { showAdd = false; refresh++ }
    selectedTask?.let { task ->
        AlertDialog(
            onDismissRequest = { selectedTask = null },
            title = { Text(task.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${task.subject} · ${task.durationMin} min", color = TextSecondary)
                    Text("${task.activityType.name.lowercase().replace('_', ' ')} · ${task.dueDay}", color = TextMuted, fontSize = 12.sp)
                    Text(if (task.done) "Completed" else "Not completed", color = if (task.done) AccentGreen else TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    repo.toggleTask(task.id)
                    selectedTask = null
                    refresh++
                }) { Text(if (task.done) "Mark incomplete" else "Complete") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { repo.deleteTask(task.id); selectedTask = null; refresh++ }) { Text("Delete", color = AccentPink) }
                    TextButton(onClick = { selectedTask = null }) { Text("Close") }
                }
            }
        )
    }
}

private fun LazyListScope.taskGroup(title: String, tasks: List<AppTask>, repo: JeeRepository, view: androidx.compose.ui.platform.AndroidComposeView?, onOpen: (AppTask) -> Unit) {
    if (tasks.isEmpty()) return
    item(key = "header_$title") {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(6.dp)); Text("· ${tasks.size}", color = TextMuted, fontSize = 11.sp)
        }
    }
    items(tasks, key = { it.id }) { task -> TaskListItem(task, repo, view, onOpen) }
}

@Composable
private fun TaskListItem(task: AppTask, repo: JeeRepository, view: androidx.compose.ui.platform.AndroidComposeView?, onOpen: (AppTask) -> Unit) {
    Row(
        Modifier.fillMaxWidth().premiumClick { onOpen(task) }.padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(23.dp).clip(RoundedCornerShape(7.dp)).background(if (task.done) AccentGreen else Color.Transparent)
                .premiumClick(haptic = HapticFeedbackConstants.KEYBOARD_TAP) { repo.toggleTask(task.id) },
            Alignment.Center
        ) {
            if (task.done) Icon(Icons.Filled.Check, null, tint = AccentGreenDark, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(task.title, color = if (task.done) TextMuted else TextOnCard, fontSize = 15.sp)
            Text("${task.subject} · ${task.durationMin}m", color = TextMuted, fontSize = 11.sp)
        }
        Icon(Icons.Filled.ChevronRight, "Open task", tint = AccentBlue, modifier = Modifier.size(19.dp))
    }
    HorizontalDivider(color = BgDivider, thickness = .5.dp)
}

private fun LazyListScope.emptyTaskState() {
    item {
        Column(Modifier.fillMaxWidth().padding(vertical = 62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nothing here yet", style = MaterialTheme.typography.titleMedium)
            Text("Add a task when you know what you want to study.", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AddTaskDialogPreview(repo: JeeRepository, onDone: () -> Unit) {
    val subjects = listOf("Physics", "Chemistry", "Mathematics")
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Physics") }
    var duration by remember { mutableStateOf("30") }
    var due by remember { mutableStateOf("Today") }
    var chapter by remember { mutableStateOf<String?>(null) }
    var activity by remember { mutableStateOf(ActivityType.REVISION) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Add task") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Task") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp)); Text("Subject", color = TextMuted, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(subjects) { FilterChip(it, it == subject) { subject = it } } }
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf(ActivityType.LEARNING, ActivityType.PRACTICE, ActivityType.REVISION, ActivityType.TEST)) { FilterChip(it.name.lowercase().replace('_', ' '), it == activity) { activity = it } } }
                Spacer(Modifier.height(10.dp)); OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(4) }, label = { Text("Minutes") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip("Today", due == "Today") { due = "Today" }; FilterChip("Upcoming", due == "Upcoming") { due = "Upcoming" } }
            }
        },
        confirmButton = {
            val m = duration.toIntOrNull()
            TextButton(enabled = title.isNotBlank() && m != null && m in 1..1440, onClick = { m?.let { repo.addTask(title.trim(), subject, it, due, chapter, activity); onDone() } }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancel") } }
    )
}
