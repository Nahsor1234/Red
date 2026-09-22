package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.ActivityType
import com.example.jeecommandcenter.data.AppTask
import com.example.jeecommandcenter.data.JeeCatalog
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun TasksScreen(
    repo: JeeRepository,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onAiClick: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var refresh by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<AppTask?>(null) }

    val filters = listOf("All", "Today", "Upcoming", "Completed")
    val all = remember(refresh) { repo.getTasks() }
    val visible = all.filter {
        when (selectedFilter) {
            "Today" -> it.dueDay == "Today"
            "Upcoming" -> it.dueDay != "Today" && !it.done
            "Completed" -> it.done
            else -> true
        }
    }

    Scaffold(
        containerColor = BgApp,
        bottomBar = { BottomNavBar(selectedTab, onTabSelected, onAiClick) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                containerColor = AccentBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add task")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(JeeSpacing.md))
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    "Tasks",
                    style = MaterialTheme.typography.headlineSmall
                )
                Icon(
                    Icons.Filled.Refresh,
                    "Refresh",
                    tint = TextSecondary,
                    modifier = Modifier.premiumClick { refresh++ }
                )
            }

            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    FilterChip(filter, filter == selectedFilter) {
                        selectedFilter = filter
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                if (visible.isEmpty()) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Nothing here yet",
                                color = TextOnCard,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Add a task when you know what you want to study.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(visible, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = {
                                repo.toggleTask(task.id)
                                refresh++
                            },
                            onDelete = { taskToDelete = task }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAdd) {
        AddTaskDialog(
            onDismiss = { showAdd = false },
            onAdd = { title, subject, duration, due, chapterId, activityType ->
                if (repo.addTask(title, subject, duration, due, chapterId, activityType)) {
                    showAdd = false
                    refresh++
                }
            }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete task?") },
            text = { Text("This removes “${task.title}” from your local plan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        repo.deleteTask(task.id)
                        taskToDelete = null
                        refresh++
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TaskCard(
    task: AppTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .padding(12.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .premiumClick(onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (task.done) AccentGreen.copy(alpha = 0.2f) else AccentBlueSoft
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (task.done) Icons.Filled.Check else Icons.Filled.MenuBook,
                    null,
                    tint = if (task.done) AccentGreen else AccentBlueLight,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    task.title,
                    color = if (task.done) TextMuted else TextOnCard,
                    fontSize = 13.sp
                )
                Text(
                    task.subject + " · " + task.durationMin + "m · " + task.dueDay,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.DeleteOutline,
                "Delete",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, String, String?, ActivityType) -> Unit
) {
    val subjects = listOf("Physics", "Chemistry", "Mathematics")
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(subjects.first()) }
    var selectedChapterId by remember { mutableStateOf<String?>(null) }
    var chapterExpanded by remember { mutableStateOf(false) }
    var activityType by remember { mutableStateOf(ActivityType.OTHER) }
    var duration by remember { mutableStateOf("30") }
    var due by remember { mutableStateOf("Today") }
    val chapters = JeeCatalog.forSubject(subject)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add task") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("Subject", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjects) { item ->
                        FilterChip(item, item == subject) {
                            subject = item
                            selectedChapterId = null
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box {
                    OutlinedButton(
                        onClick = { chapterExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedChapterId?.let { id ->
                            JeeCatalog.find(id)?.name ?: "Chapter"
                        } ?: "No chapter linked")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.ExpandMore, null)
                    }
                    DropdownMenu(
                        expanded = chapterExpanded,
                        onDismissRequest = { chapterExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No chapter linked") },
                            onClick = {
                                selectedChapterId = null
                                chapterExpanded = false
                            }
                        )
                        chapters.forEach { chapter ->
                            DropdownMenuItem(
                                text = { Text(chapter.number.toString() + ". " + chapter.name) },
                                onClick = {
                                    selectedChapterId = chapter.id
                                    chapterExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Activity", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(ActivityType.LEARNING, ActivityType.PRACTICE, ActivityType.REVISION, ActivityType.TEST)) { type ->
                        FilterChip(
                            type.name.lowercase().replace('_', ' '),
                            type == activityType
                        ) { activityType = type }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit).take(4) },
                    label = { Text("Minutes") },
                    supportingText = { Text("1–1440 minutes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("When", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip("Today", due == "Today") { due = "Today" }
                    FilterChip("Upcoming", due == "Upcoming") { due = "Upcoming" }
                }
            }
        },
        confirmButton = {
            val minutes = duration.toIntOrNull()
            TextButton(
                enabled = title.isNotBlank() && minutes != null && minutes in 1..1440,
                onClick = {
                    minutes?.let {
                        onAdd(title.trim(), subject, it, due, selectedChapterId, activityType)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
