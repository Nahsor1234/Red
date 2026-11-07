package com.example.jeecommandcenter.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.time.LocalDate
import kotlin.math.ceil

private enum class TimerPreset(val label: String, val sublabel: String, val minutes: Int) {
    POMODORO("Pomodoro", "25 min", 25), SHORT("Short", "15 min", 15), LONG("Long", "50 min", 50)
}

@Composable
fun StudyTimerScreen(repo: JeeRepository) {
    val initial = remember { repo.restoreTimerState() }
    var selected by remember { mutableStateOf(TimerPreset.values().firstOrNull { it.minutes * 60 == initial.totalSeconds }) }
    var custom by remember { mutableStateOf((initial.totalSeconds / 60).coerceAtLeast(1).toString()) }
    var showCustom by remember { mutableStateOf(false) }
    var total by remember { mutableIntStateOf(initial.totalSeconds) }
    var remaining by remember { mutableIntStateOf(initial.remainingSeconds) }
    var running by remember { mutableStateOf(initial.running) }
    var end by remember { mutableLongStateOf(initial.endAtMillis) }
    var refresh by remember { mutableIntStateOf(0) }
    var subject by rememberSaveable { mutableStateOf("General") }
    var chapter by rememberSaveable { mutableStateOf<String?>(null) }
    var activity by rememberSaveable { mutableStateOf(ActivityType.LEARNING) }
    var contextExpanded by remember { mutableStateOf(false) }
    var chapterPickerExpanded by remember { mutableStateOf(false) }
    val view = LocalView.current
    val subjects = listOf("General", "Physics", "Chemistry", "Mathematics")
    val chapters = remember(subject) { if (subject == "General") emptyList() else JeeCatalog.forSubject(subject) }
    var sessions by remember { mutableStateOf<List<StudySession>>(emptyList()) }
    var sessionsLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(refresh) {
        sessions = withContext(Dispatchers.IO) { repo.getSessions() }
        sessionsLoaded = true
    }
    val todayDate = LocalDate.now().toString()
    val todayMinutes = remember(sessions, todayDate) {
        sessions.asSequence().filter { it.date == todayDate }.sumOf { it.minutes }
    }
    val recentSessions = remember(sessions) { sessions.take(6) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (running) {
            val r = ceil((end - System.currentTimeMillis()).coerceAtLeast(0) / 1000.0).toInt().coerceIn(0, total)
            remaining = r
            if (r == 0) {
                running = false
                end = 0
                repo.completeTimer(total, subject, chapter, activity)
                refresh++
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                break
            }
            // The display is second-based; avoid triggering four recompositions per second.
            delay(1000)
        }
    }

    fun preset(p: TimerPreset) {
        selected = p
        total = p.minutes * 60
        remaining = total
        running = false
        end = 0
        repo.resetTimer(total)
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun toggleRunning() {
        if (running) {
            remaining = ceil((end - System.currentTimeMillis()).coerceAtLeast(0) / 1000.0).toInt().coerceIn(0, total)
            running = false
            end = 0
            repo.pauseTimer(total, remaining)
        } else {
            if (remaining <= 0) remaining = total
            repo.startTimer(total, remaining)
            end = System.currentTimeMillis() + remaining * 1000
            running = true
        }
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Scaffold(containerColor = BgApp) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Study session", style = MaterialTheme.typography.headlineSmall)
                    Text(if (sessionsLoaded) "$todayMinutes min today" else "Loading…", style = MaterialTheme.typography.labelMedium)
                }
            }
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), Alignment.Center) {
                    ProgressRing(
                        progress = if (total > 0) remaining.toFloat() / total else 0f,
                        diameter = 240.dp,
                        strokeWidth = 12.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp, fontFeatureSettings = "tnum")
                            )
                            Text(if (running) "Focus mode" else "Ready", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = ::toggleRunning,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = JeeShapes.medium
                ) {
                    Icon(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) "Pause" else "Start session", style = MaterialTheme.typography.labelLarge)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimerPreset.values().forEach { p -> Preset(p.label, p.sublabel, selected == p, Modifier.weight(1f)) { preset(p) } }
                    Preset("Custom", (total / 60).toString() + " min", selected == null, Modifier.weight(1f)) {
                        custom = (total / 60).coerceAtLeast(1).toString()
                        showCustom = true
                    }
                }
            }
            item {
                JeeCard {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Session context", style = MaterialTheme.typography.labelLarge)
                        TextButton(enabled = !running, onClick = { contextExpanded = !contextExpanded; if (!contextExpanded) chapterPickerExpanded = false }) {
                            Text(if (contextExpanded) "Done" else "Change", style = MaterialTheme.typography.labelLarge, color = if (running) TextMuted else PrimaryLight)
                        }
                    }
                    Text(subject, style = MaterialTheme.typography.titleMedium)
                    Text(chapter?.let { JeeCatalog.find(it)?.name } ?: "No chapter linked", style = MaterialTheme.typography.bodySmall)
                    Text("Activity: " + activity.name.lowercase().replace('_', ' '), style = MaterialTheme.typography.labelSmall)
                    AnimatedVisibility(visible = contextExpanded) {
                        Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Subject", style = MaterialTheme.typography.labelMedium)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                subjects.forEach { value ->
                                    JeeFilterChip(value, value == subject) { subject = value; chapter = null; chapterPickerExpanded = false }
                                }
                            }
                            Text("Chapter", style = MaterialTheme.typography.labelMedium)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(JeeShapes.medium)
                                    .border(1.dp, OutlineStrong.copy(alpha = .6f), JeeShapes.medium)
                                    .clickable(enabled = subject != "General", onClick = { chapterPickerExpanded = !chapterPickerExpanded }),
                                color = BgAppBase.copy(alpha = .72f),
                                shape = JeeShapes.medium
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        chapter?.let { JeeCatalog.find(it)?.name } ?: if (subject == "General") "General study" else "Select chapter",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (chapter != null) TextPrimary else TextMuted,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(if (chapterPickerExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                }
                            }
                            AnimatedVisibility(visible = chapterPickerExpanded && subject != "General") {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                        .clip(JeeShapes.medium)
                                        .background(BgAppBase.copy(alpha = .72f))
                                        .border(1.dp, BgCardBorder, JeeShapes.medium)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    chapters.forEach { value ->
                                        TextButton(onClick = { chapter = value.id; chapterPickerExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                                Text("${value.number}. ${value.name}", style = MaterialTheme.typography.bodyMedium, color = if (chapter == value.id) PrimaryLight else TextPrimary, modifier = Modifier.weight(1f))
                                                if (chapter == value.id) Icon(Icons.Filled.Check, "Selected", tint = PrimaryLight, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            Text("Activity", style = MaterialTheme.typography.labelMedium)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(ActivityType.LEARNING, ActivityType.PRACTICE, ActivityType.REVISION, ActivityType.TEST).forEach { value ->
                                    JeeFilterChip(value.name.lowercase().replace('_', ' '), value == activity) { activity = value }
                                }
                            }
                        }
                    }
                }
            }
            item { SectionHeader("Completed sessions") }
            if (recentSessions.isEmpty()) {
                item {
                    JeeCard {
                        Text("No completed sessions yet.", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text("Completed sessions will appear here after your first study block.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                items(recentSessions, key = { it.id }) { session ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(JeeShapes.medium)
                            .background(BgCard)
                            .border(1.dp, BgCardBorder.copy(alpha = .8f), JeeShapes.medium)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(session.subject, style = MaterialTheme.typography.titleSmall)
                            Text(session.chapter + " · " + session.activityType.name.lowercase().replace('_', ' '), style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(session.minutes.toString() + " min", style = MaterialTheme.typography.labelLarge, color = PrimaryLight)
                            Text(session.date, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
    if (showCustom) {
        val minutes = custom.toIntOrNull()
        AlertDialog(
            onDismissRequest = { showCustom = false },
            title = { Text("Custom timer") },
            text = { OutlinedTextField(custom, { custom = it.filter(Char::isDigit).take(4) }, label = { Text("Minutes") }, singleLine = true) },
            confirmButton = {
                TextButton(
                    enabled = minutes != null && minutes in 1..1440,
                    onClick = {
                        minutes?.let {
                            selected = null
                            total = it * 60
                            remaining = total
                            running = false
                            end = 0
                            repo.resetTimer(total)
                            showCustom = false
                        }
                    }
                ) { Text("Use") }
            },
            dismissButton = { TextButton(onClick = { showCustom = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Preset(label: String, sub: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(JeeShapes.medium)
            .background(if (selected) PrimarySoft else BgCard)
            .border(1.dp, if (selected) Primary else BgCardBorder.copy(alpha = .8f), JeeShapes.medium)
            .premiumClick(onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) PrimaryLight else TextPrimary)
        Text(sub, style = MaterialTheme.typography.labelSmall)
    }
}
