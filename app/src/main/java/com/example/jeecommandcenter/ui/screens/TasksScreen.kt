package com.example.jeecommandcenter.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun TasksScreen(repo: JeeRepository, selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit = {}) {
    var filter by remember { mutableStateOf("All") }; var refresh by remember { mutableIntStateOf(0) }; var showAdd by remember { mutableStateOf(false) }; var selectedTask by remember { mutableStateOf<AppTask?>(null) }
    val all = remember(refresh) { repo.getTasks() }; val visible = all.filter { when (filter) { "Today" -> it.dueDay == "Today"; "Upcoming" -> it.dueDay != "Today" && !it.done; "Completed" -> it.done; else -> true } }; val today = all.filter { it.dueDay == "Today" && !it.done }; val upcoming = all.filter { it.dueDay != "Today" && !it.done }; val completed = all.filter { it.done }
    Box(Modifier.fillMaxSize()) {
        Scaffold(containerColor = BgApp.copy(alpha = 0f), bottomBar = { AnimatedBottomNavBar(selectedTab, onTabSelected, visible = selectedTab != AppTab.AI) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(JeeSpacing.sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tasks", style = MaterialTheme.typography.headlineSmall)
                        AnimatedContent(targetState = "${all.count { it.done }} completed · ${all.count { !it.done }} remaining",
                            label = "task-summary") { summary ->
                            Text(summary, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add task", tint = PrimaryLight)
                    }
                }
                Spacer(Modifier.height(JeeSpacing.sm))
                LinearStatBar(if (all.isEmpty()) 0f else all.count { it.done }.toFloat() / all.size, height = 5.dp)
                Spacer(Modifier.height(JeeSpacing.md))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(JeeSpacing.sm)) {
                    items(listOf("All" to all.size, "Today" to today.size,
                        "Upcoming" to upcoming.size, "Completed" to completed.size)) { (label, count) ->
                        JeeFilterChip("$label $count", label == filter) { filter = label }
                    }
                }
                AnimatedContent(targetState = filter, label = "task-filter-summary") { activeFilter ->
                    if (activeFilter != "All") {
                        Text("${visible.size} ${if (visible.size == 1) "task" else "tasks"} · ${visible.sumOf { it.durationMin }} min planned",
                            color = TextSecondary, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.fillMaxWidth().padding(vertical = JeeSpacing.sm))
                    } else Spacer(Modifier.height(JeeSpacing.xs))
                }
                LazyColumn(Modifier.weight(1f).animateContentSize(animationSpec = tween(JeeMotion.layoutMillis))) {
                    if (filter == "All") { taskGroup("Today", today, repo, { selectedTask = it }, { refresh++ }); taskGroup("Upcoming", upcoming, repo, { selectedTask = it }, { refresh++ }); if (completed.isNotEmpty()) taskGroup("Completed", completed, repo, { selectedTask = it }, { refresh++ }); if (all.isEmpty()) emptyTaskState() }
                    else { if (visible.isEmpty()) emptyTaskState() else visible.forEach { task -> item(key = task.id) { TaskListItem(task, repo, { selectedTask = it }, { refresh++ }) } } }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
    if (showAdd) AddTaskDialog(repo) { showAdd = false; refresh++ }
    selectedTask?.let { task -> AlertDialog(onDismissRequest = { selectedTask = null }, title = { Text(task.title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("${task.subject} · ${task.durationMin} min", color = TextSecondary); Text("${task.activityType.name.lowercase().replace('_', ' ')} · ${task.dueDay}", color = TextMuted, fontSize = 12.sp); Text(if (task.done) "Completed" else "Not completed", color = if (task.done) AccentGreen else TextSecondary, fontSize = 12.sp) } }, confirmButton = { TextButton(onClick = { repo.toggleTask(task.id); selectedTask = null; refresh++ }) { Text(if (task.done) "Mark incomplete" else "Complete") } }, dismissButton = { Row { TextButton(onClick = { repo.deleteTask(task.id); selectedTask = null; refresh++ }) { Text("Delete", color = Danger) }; TextButton(onClick = { selectedTask = null }) { Text("Close") } } }) }
}

private fun LazyListScope.taskGroup(title: String, tasks: List<AppTask>, repo: JeeRepository, onOpen: (AppTask) -> Unit, onRefresh: () -> Unit) {
    if (tasks.isEmpty()) return
    item(key = "header_$title") {
        Row(Modifier.fillMaxWidth().padding(top = JeeSpacing.lg, bottom = JeeSpacing.xs),
            verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(JeeSpacing.sm))
            Text("${tasks.size}", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
    items(tasks, key = { it.id }) { task -> TaskListItem(task, repo, onOpen, onRefresh) }
}

@Composable
private fun TaskListItem(task: AppTask, repo: JeeRepository, onOpen: (AppTask) -> Unit, onRefresh: () -> Unit) {
    val checkColor by animateColorAsState(if (task.done) Success else Color.Transparent,
        animationSpec = tween(JeeMotion.standardMillis), label = "task-checkbox-color")
    val titleColor by animateColorAsState(if (task.done) TextSecondary else TextPrimary,
        animationSpec = tween(JeeMotion.standardMillis), label = "task-title-color")
    Column(Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(JeeMotion.standardMillis))) {
        Row(Modifier.fillMaxWidth().premiumClick { onOpen(task) }.padding(vertical = JeeSpacing.xs),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).semantics {
                role = Role.Checkbox
                contentDescription = "Mark ${task.title} complete"
                stateDescription = if (task.done) "Completed" else "Not completed"
            }.premiumClick(haptic = HapticFeedbackConstants.KEYBOARD_TAP) {
                repo.toggleTask(task.id)
                onRefresh()
            }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(checkColor)
                    .border(1.dp, if (task.done) Success else BgCardBorder, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center) {
                    AnimatedVisibility(visible = task.done, enter = fadeIn(tween(JeeMotion.fastMillis)),
                        exit = fadeOut(tween(JeeMotion.fastMillis))) {
                        Icon(Icons.Filled.Check, null, tint = SuccessDark, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(task.title, color = titleColor, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium, maxLines = 2)
                Spacer(Modifier.height(2.dp))
                Text("${task.subject} · ${task.durationMin} min · ${task.dueDay}",
                    color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text(task.activityType.name.lowercase().replace('_', ' '),
                    color = PrimaryLight, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Filled.ChevronRight, "Open task details", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(Modifier.padding(start = 48.dp), color = BgDivider.copy(alpha = .55f), thickness = 0.7.dp)
    }
}

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
