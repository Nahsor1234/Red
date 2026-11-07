package com.example.jeecommandcenter.ui.screens

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
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

private val TaskFilters = listOf("All", "Today", "Upcoming", "Completed")

private fun AppTask.activityLabel() = activityType.name.lowercase().replace('_', ' ')

@Composable
fun TasksScreen(repo: JeeRepository) {
    var filter by remember { mutableStateOf("All") }
    var refresh by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<AppTask?>(null) }

    val all = remember(refresh) { repo.getTasks() }
    val today = remember(all) { all.filter { it.dueDay == "Today" && !it.done } }
    val upcoming = remember(all) { all.filter { it.dueDay != "Today" && !it.done } }
    val completed = remember(all) { all.filter { it.done } }
    val visible = remember(all, filter) {
        when (filter) {
            "Today" -> all.filter { it.dueDay == "Today" }
            "Upcoming" -> upcoming
            "Completed" -> completed
            else -> all
        }
    }
    val openCount = today.size + upcoming.size
    val plannedMinutes = remember(all) { all.filter { !it.done }.sumOf { it.durationMin } }
    val toggle: (AppTask) -> Unit = { task -> repo.toggleTask(task.id); refresh++ }

    Scaffold(
        containerColor = BgApp,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                containerColor = Primary,
                contentColor = OnPrimary,
                shape = JeeShapes.medium
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add task", style = MaterialTheme.typography.labelLarge)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Column {
                    Text("Tasks", style = MaterialTheme.typography.headlineSmall)
                    Text("$openCount open · $plannedMinutes min planned", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { refresh++ }) {
                    Icon(Icons.Filled.Refresh, "Refresh", tint = TextSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TaskFilters) { value -> JeeFilterChip(value, value == filter) { filter = value } }
            }
            Spacer(Modifier.height(12.dp))
            if (filter != "All") {
                val count = visible.size
                JeeCard(featured = true) {
                    Text(filter, style = MaterialTheme.typography.labelLarge, color = PrimaryLight)
                    Text("$count ${if (count == 1) "task" else "tasks"}", style = MaterialTheme.typography.titleLarge)
                    Text("${visible.sumOf { it.durationMin }} minutes planned", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(4.dp))
            }
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
            ) {
                if (filter == "All") {
                    taskGroup("Today", today, { selectedTask = it }, toggle)
                    taskGroup("Upcoming", upcoming, { selectedTask = it }, toggle)
                    taskGroup("Completed", completed, { selectedTask = it }, toggle)
                    if (all.isEmpty()) emptyTaskState()
                } else if (visible.isEmpty()) {
                    emptyTaskState()
                } else {
                    items(visible, key = { it.id }) { task -> TaskListItem(task, { selectedTask = it }, toggle) }
                }
            }
        }
    }

    if (showAdd) AddTaskDialog(repo) { showAdd = false; refresh++ }

    selectedTask?.let { task ->
        AlertDialog(
            onDismissRequest = { selectedTask = null },
            title = { Text(task.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${task.subject} · ${task.durationMin} min", color = TextSecondary)
                    Text("${task.activityLabel()} · ${task.dueDay}", style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (task.done) "Completed" else "Not completed",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (task.done) Success else TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { repo.toggleTask(task.id); selectedTask = null; refresh++ }) {
                    Text(if (task.done) "Mark incomplete" else "Complete")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { repo.deleteTask(task.id); selectedTask = null; refresh++ }) {
                        Text("Delete", color = Danger)
                    }
                    TextButton(onClick = { selectedTask = null }) { Text("Close") }
                }
            }
        )
    }
}

private fun LazyListScope.taskGroup(
    title: String,
    tasks: List<AppTask>,
    onOpen: (AppTask) -> Unit,
    onToggle: (AppTask) -> Unit
) {
    if (tasks.isEmpty()) return
    item(key = "header_$title") {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Text(tasks.size.toString(), style = MaterialTheme.typography.labelMedium)
        }
    }
    items(tasks, key = { it.id }) { task -> TaskListItem(task, onOpen, onToggle) }
}

@Composable
private fun TaskListItem(task: AppTask, onOpen: (AppTask) -> Unit, onToggle: (AppTask) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(JeeShapes.medium)
            .background(BgCard)
            .border(1.dp, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
            .premiumClick { onOpen(task) }
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaskCheckbox(task.done) { onToggle(task) }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (task.done) TextMuted else TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${task.subject} · ${task.durationMin}m", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimarySoft)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(task.activityLabel(), style = MaterialTheme.typography.labelSmall, color = PrimaryLight)
                }
            }
        }
        Icon(Icons.Filled.ChevronRight, "Open task", tint = TextMuted)
    }
}

private fun LazyListScope.emptyTaskState() {
    item {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.AssignmentTurnedIn, null, tint = TextMuted, modifier = Modifier.size(36.dp))
            Text("Nothing here yet", style = MaterialTheme.typography.titleMedium)
            Text("Add a task when you know what you want to study.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun AddTaskDialog(repo: JeeRepository, onDone: () -> Unit) {
    val subjects = listOf("Physics", "Chemistry", "Mathematics")
    val activities = listOf(ActivityType.LEARNING, ActivityType.PRACTICE, ActivityType.REVISION, ActivityType.TEST)
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Physics") }
    var duration by remember { mutableStateOf("30") }
    var due by remember { mutableStateOf("Today") }
    var activity by remember { mutableStateOf(ActivityType.REVISION) }
    val minutes = duration.toIntOrNull()

    androidx.compose.ui.window.Dialog(onDismissRequest = onDone) {
        Column(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .wrapContentHeight()
                .clip(JeeShapes.large)
                .background(BgCardAlt)
                .border(1.dp, BgCardBorder.copy(alpha = .9f), JeeShapes.large)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Add task", style = MaterialTheme.typography.headlineSmall)
                    Text("Create a focused study block", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDone) { Icon(Icons.Filled.Close, "Close") }
            }
            Spacer(Modifier.height(14.dp))
            FieldLabel("Task")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("e.g. Revise electrostatics") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = JeeShapes.medium
            )
            Spacer(Modifier.height(12.dp))
            FieldLabel("Subject")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjects) { value -> JeeFilterChip(value, value == subject) { subject = value } }
            }
            Spacer(Modifier.height(12.dp))
            FieldLabel("Activity")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activities) { type ->
                    JeeFilterChip(type.name.lowercase().replace('_', ' '), type == activity) { activity = type }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("Duration")
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it.filter(Char::isDigit).take(4) },
                        suffix = { Text("min") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = JeeShapes.medium
                    )
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("Schedule")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        JeeFilterChip("Today", due == "Today") { due = "Today" }
                        JeeFilterChip("Later", due == "Upcoming") { due = "Upcoming" }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = BgDivider.copy(alpha = .55f))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDone) { Text("Cancel") }
                Spacer(Modifier.width(4.dp))
                Button(
                    enabled = title.isNotBlank() && minutes != null && minutes in 1..1440,
                    onClick = { minutes?.let { repo.addTask(title.trim(), subject, it, due, null, activity); onDone() } },
                    shape = JeeShapes.medium
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add task")
                }
            }
        }
    }
}
