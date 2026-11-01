package com.example.jeecommandcenter.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(
    repo: JeeRepository,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onAiClick: () -> Unit = {}
) {
    var filter by rememberSaveable { mutableStateOf("All") }
    var refresh by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<AppTask?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val all = remember(refresh) { repo.getTasks() }
    val today = all.filter { it.dueDay == "Today" && !it.done }
    val upcoming = all.filter { it.dueDay != "Today" && !it.done }
    val completed = all.filter { it.done }
    val visible = when (filter) {
        "Today" -> today
        "Upcoming" -> upcoming
        "Completed" -> completed
        else -> all
    }
    val completedCount = completed.size
    val remainingCount = all.size - completedCount
    val progress = if (all.isEmpty()) 0f else completedCount.toFloat() / all.size

    fun toggleWithUndo(task: AppTask) {
        repo.toggleTask(task.id)
        refresh++
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = if (task.done) "Task reopened" else "Task completed",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                repo.toggleTask(task.id)
                refresh++
            }
        }
    }

    Scaffold(
        containerColor = BgAppBase,
        bottomBar = {
            AnimatedBottomNavBar(selectedTab, onTabSelected, visible = selectedTab != AppTab.AI)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add task") },
                shape = JeeShapes.medium,
                containerColor = Primary,
                contentColor = BgAppBase
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = JeeSpacing.lg)
        ) {
            Spacer(Modifier.height(JeeSpacing.md))
            Column {
                Text("Tasks", style = MaterialTheme.typography.headlineSmall)
                AnimatedContent(
                    targetState = "$remainingCount remaining · $completedCount completed",
                    label = "task-summary"
                ) { summary ->
                    Text(summary, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(JeeSpacing.md))
            LinearStatBar(progress, height = 4.dp)
            Spacer(Modifier.height(JeeSpacing.md))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(JeeSpacing.sm),
                contentPadding = PaddingValues(bottom = JeeSpacing.xs)
            ) {
                items(
                    listOf(
                        "All" to all.size,
                        "Today" to today.size,
                        "Upcoming" to upcoming.size,
                        "Completed" to completed.size
                    )
                ) { (label, count) ->
                    JeeFilterChip("$label $count", label == filter) { filter = label }
                }
            }

            AnimatedContent(targetState = filter, label = "task-filter-details") { active ->
                Text(
                    when (active) {
                        "Today" -> "Tasks planned for today"
                        "Upcoming" -> "Tasks without a today schedule"
                        "Completed" -> "Finished study tasks"
                        else -> "Your study task list"
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = JeeSpacing.xs, bottom = JeeSpacing.xs)
                )
            }

            LazyColumn(
                Modifier.weight(1f).animateContentSize(animationSpec = tween(JeeMotion.layoutMillis)),
                contentPadding = PaddingValues(bottom = 92.dp)
            ) {
                if (filter == "All") {
                    taskGroup("Today", today, ::toggleWithUndo, { selectedTask = it })
                    taskGroup("Upcoming", upcoming, ::toggleWithUndo, { selectedTask = it })
                    if (completed.isNotEmpty()) {
                        taskGroup("Completed", completed, ::toggleWithUndo, { selectedTask = it })
                    }
                    if (all.isEmpty()) item { EmptyTaskState() }
                } else if (visible.isEmpty()) {
                    item { EmptyTaskState() }
                } else {
                    items(visible, key = { it.id }) { task ->
                        TaskListItem(task, onToggle = { toggleWithUndo(task) }, onOpen = { selectedTask = task })
                    }
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
                Column(verticalArrangement = Arrangement.spacedBy(JeeSpacing.xs)) {
                    Text("${task.subject} · ${task.durationMin} min", color = TextSecondary)
                    Text(
                        "${task.activityType.name.lowercase().replace('_', ' ')} · ${task.dueDay}",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        if (task.done) "Completed" else "Not completed",
                        color = if (task.done) Success else TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { toggleWithUndo(task); selectedTask = null }) {
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
    onToggle: (AppTask) -> Unit,
    onOpen: (AppTask) -> Unit
) {
    if (tasks.isEmpty()) return
    item(key = "header_$title") {
        Row(
            Modifier.fillMaxWidth().padding(top = JeeSpacing.lg, bottom = JeeSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(JeeSpacing.sm))
            Text(tasks.size.toString(), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
    items(tasks, key = { it.id }) { task ->
        TaskListItem(task, onToggle = { onToggle(task) }, onOpen = { onOpen(task) })
    }
}

@Composable
private fun TaskListItem(task: AppTask, onToggle: () -> Unit, onOpen: () -> Unit) {
    val checkColor by animateColorAsState(
        if (task.done) Success else Color.Transparent,
        animationSpec = tween(JeeMotion.standardMillis),
        label = "task-checkbox-color"
    )
    val titleColor by animateColorAsState(
        if (task.done) TextSecondary else TextPrimary,
        animationSpec = tween(JeeMotion.standardMillis),
        label = "task-title-color"
    )
    Column(Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(JeeMotion.standardMillis))) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).premiumClick(onClick = onOpen)
                .padding(vertical = JeeSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).semantics {
                    role = Role.Checkbox
                    contentDescription = "Mark ${task.title} complete"
                    stateDescription = if (task.done) "Completed" else "Not completed"
                }.premiumClick(haptic = HapticFeedbackConstants.KEYBOARD_TAP, onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(20.dp).clip(JeeShapes.small).background(checkColor)
                        .border(1.dp, if (task.done) Success else BgCardBorder, JeeShapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = task.done, label = "task-check-transition") { done ->
                        if (done) Icon(Icons.Filled.Check, null, tint = SuccessDark, modifier = Modifier.size(14.dp))
                        else Spacer(Modifier.size(14.dp))
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    color = titleColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                Text(
                    "${task.subject} · ${task.durationMin} min · ${task.dueDay}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Icon(Icons.Filled.ChevronRight, "Open task details", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(Modifier.padding(start = 48.dp), color = BgDivider.copy(alpha = 0.55f), thickness = 0.7.dp)
    }
}

@Composable
private fun EmptyTaskState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JeeSpacing.xs)
    ) {
        Text("Nothing here yet", style = MaterialTheme.typography.titleMedium)
        Text("Add a task when you know what you want to study.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AddTaskDialog(repo: JeeRepository, onDone: () -> Unit) {
    val subjects = listOf("Physics", "Chemistry", "Mathematics")
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Physics") }
    var duration by remember { mutableStateOf("30") }
    var due by remember { mutableStateOf("Today") }
    var activity by remember { mutableStateOf(ActivityType.REVISION) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDone) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 420.dp).wrapContentHeight()
                .clip(JeeShapes.large).background(BgCardAlt)
                .border(1.dp, BgCardBorder, JeeShapes.large)
                .padding(horizontal = JeeSpacing.lg, vertical = JeeSpacing.md)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Add task", style = MaterialTheme.typography.titleLarge)
                    Text("Create a focused study block", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDone) { Icon(Icons.Filled.Close, "Close") }
            }
            Spacer(Modifier.height(JeeSpacing.sm))
            Text("Task", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                placeholder = { Text("e.g. Revise electrostatics") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = JeeShapes.medium
            )
            Spacer(Modifier.height(JeeSpacing.md))
            Text("Subject", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(JeeSpacing.xs)) {
                items(subjects) { value -> JeeFilterChip(value, value == subject) { subject = value } }
            }
            Spacer(Modifier.height(JeeSpacing.md))
            Text("Activity", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(JeeSpacing.xs)) {
                items(listOf(ActivityType.LEARNING, ActivityType.PRACTICE, ActivityType.REVISION, ActivityType.TEST)) { type ->
                    JeeFilterChip(type.name.lowercase().replace('_', ' '), type == activity) { activity = type }
                }
            }
            Spacer(Modifier.height(JeeSpacing.md))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(JeeSpacing.sm)) {
                Column(Modifier.weight(1f)) {
                    Text("Duration", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = duration, onValueChange = { duration = it.filter(Char::isDigit).take(4) },
                        suffix = { Text("min") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = JeeShapes.medium
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Schedule", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(JeeSpacing.xs)) {
                        JeeFilterChip("Today", due == "Today") { due = "Today" }
                        JeeFilterChip("Later", due == "Upcoming") { due = "Upcoming" }
                    }
                }
            }
            Spacer(Modifier.height(JeeSpacing.md))
            HorizontalDivider(color = BgDivider)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDone) { Text("Cancel") }
                val minutes = duration.toIntOrNull()
                Button(
                    enabled = title.isNotBlank() && minutes != null && minutes in 1..1440,
                    onClick = {
                        minutes?.let {
                            repo.addTask(title.trim(), subject, it, due, null, activity)
                            onDone()
                        }
                    },
                    shape = JeeShapes.medium
                ) { Text("Add task") }
            }
        }
    }
}
