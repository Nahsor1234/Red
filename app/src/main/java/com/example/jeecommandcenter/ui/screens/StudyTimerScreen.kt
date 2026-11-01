package com.example.jeecommandcenter.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
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
fun StudyTimerScreen(repo: JeeRepository, selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit = {}) {
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

    Scaffold(containerColor = BgApp, bottomBar = { AnimatedBottomNavBar(selectedTab, onTabSelected, visible = selectedTab != AppTab.AI) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Study session", style = MaterialTheme.typography.headlineSmall)
                    Text(if (sessionsLoaded) "$todayMinutes min today" else "Loading…", color = TextMuted, fontSize = 11.sp)
                }
            }
            item {
                Box(Modifier.fillMaxWidth().height(250.dp), Alignment.Center) {
                    val progress = if (total > 0) remaining.toFloat() / total else 0f
                    val animatedProgress by animateFloatAsState(progress,
                        animationSpec = tween(JeeMotion.timerProgressMillis), label = "timer-ring-progress")
                    Canvas(Modifier.size(224.dp)) {
                        val st = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                        drawArc(BgDivider, -90f, 360f, false, style = st)
                        drawArc(Primary, -90f, 360f * animatedProgress, false, style = st)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60), fontSize = 46.sp, fontWeight = FontWeight.Normal)
                        AnimatedContent(targetState = when {
                            running -> "Focus mode"
                            remaining == 0 -> "Session complete"
                            remaining < total -> "Paused"
                            else -> "Ready"
                        }, label = "timer-session-state") { state ->
                            Text(state, color = if (running) PrimaryLight else TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(JeeMotion.layoutMillis))
                        .clip(RoundedCornerShape(18.dp)).background(BgCardAlt)
                        .border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
                        .padding(15.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("SESSION CONTEXT", color = TextSecondary, fontSize = 11.sp)
                        TextButton(enabled = !running, onClick = { contextExpanded = !contextExpanded; if (!contextExpanded) chapterPickerExpanded = false }) { Text(if (contextExpanded) "Done" else "Change", fontSize = 11.sp) }
                    }
                    Text(subject, style = MaterialTheme.typography.titleMedium)
                    Text(chapter?.let { JeeCatalog.find(it)?.name } ?: "No chapter linked", color = TextMuted, fontSize = 11.sp)
                    Text("Activity: " + activity.name.lowercase().replace('_', ' '), color = TextMuted, fontSize = 10.sp)
                    AnimatedVisibility(visible = contextExpanded) {
                        Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Subject", color = TextSecondary, fontSize = 12.sp)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                subjects.forEach { value ->
                                    JeeFilterChip(value, value == subject) { subject = value; chapter = null; chapterPickerExpanded = false }
                                }
                            }
                            Text("Chapter", color = TextSecondary, fontSize = 12.sp)
                            Surface(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, BgCardBorder.copy(alpha = .9f), RoundedCornerShape(14.dp)).clickable(enabled = subject != "General", onClick = { chapterPickerExpanded = !chapterPickerExpanded }),
                                color = BgAppBase.copy(alpha = .72f), shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(chapter?.let { JeeCatalog.find(it)?.name } ?: if (subject == "General") "General study" else "Select chapter", color = if (chapter != null) TextPrimary else TextMuted, fontSize = 12.sp, maxLines = 1)
                                    Icon(if (chapterPickerExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                            AnimatedVisibility(visible = chapterPickerExpanded && subject != "General") {
                                Column(Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(14.dp)).background(BgAppBase.copy(alpha = .72f)).border(1.dp, BgCardBorder.copy(alpha = .75f), RoundedCornerShape(14.dp)).verticalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
                                    chapters.forEach { value ->
                                        TextButton(onClick = { chapter = value.id; chapterPickerExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                                Text("${value.number}. ${value.name}", color = if (chapter == value.id) PrimaryLight else TextOnCard, fontSize = 12.sp)
                                                if (chapter == value.id) Icon(Icons.Filled.Check, null, tint = PrimaryLight, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            Text("Activity", color = TextSecondary, fontSize = 12.sp)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(ActivityType.LEARNING, ActivityType.PRACTICE, ActivityType.REVISION, ActivityType.TEST).forEach { value ->
                                    JeeFilterChip(value.name.lowercase().replace('_', ' '), value == activity) { activity = value }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimerPreset.values().forEach { p -> Preset(p.label, p.sublabel, selected == p, Modifier.weight(1f)) { preset(p) } }
                    Preset("Custom", (total / 60).toString() + " min", selected == null, Modifier.weight(1f)) { custom = (total / 60).coerceAtLeast(1).toString(); showCustom = true }
                }
            }
            item {
                Button(onClick = {
                    if (running) {
                        remaining = ceil((end - System.currentTimeMillis()).coerceAtLeast(0) / 1000.0).toInt().coerceIn(0, total); running = false; end = 0; repo.pauseTimer(total, remaining)
                    } else {
                        if (remaining <= 0) remaining = total
                        repo.startTimer(total, remaining); end = System.currentTimeMillis() + remaining * 1000; running = true
                    }
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) {
                    AnimatedContent(targetState = running, label = "timer-primary-action") { isRunning ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color(0xFF17120A))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isRunning) "Pause" else "Start session", color = Color(0xFF17120A))
                        }
                    }
                }
            }
            item { SectionHeader("Completed sessions") }
            if (recentSessions.isEmpty()) {
                item { JeeCard { Text("No completed sessions yet.", color = TextMuted, fontSize = 12.sp); Spacer(Modifier.height(4.dp)); Text("Completed sessions will appear here after your first study block.", color = TextSecondary, fontSize = 11.sp) } }
            } else {
                items(recentSessions, key = { it.id }) { session ->
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(vertical = JeeSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(session.subject, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                Text(session.chapter + " · " + session.activityType.name.lowercase().replace('_', ' '),
                                    color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(session.minutes.toString() + " min", color = PrimaryLight, style = MaterialTheme.typography.labelLarge)
                                Text(session.date, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        HorizontalDivider(color = BgDivider.copy(alpha = .55f), thickness = 0.7.dp)
                    }
                }
            }
        }
    }
    if (showCustom) {
        AlertDialog(onDismissRequest = { showCustom = false }, title = { Text("Custom timer") }, text = { OutlinedTextField(custom, { custom = it.filter(Char::isDigit).take(4) }, label = { Text("Minutes") }, singleLine = true) }, confirmButton = {
            val m = custom.toIntOrNull()
            TextButton(enabled = m != null && m in 1..1440, onClick = { m?.let { selected = null; total = it * 60; remaining = total; running = false; end = 0; repo.resetTimer(total); showCustom = false } }) { Text("Use") }
        }, dismissButton = { TextButton(onClick = { showCustom = false }) { Text("Cancel") } })
    }
}

@Composable
private fun Preset(label: String, sub: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val containerColor by animateColorAsState(if (selected) PrimarySoft else BgCard,
        animationSpec = tween(JeeMotion.standardMillis), label = "timer-preset-container")
    val borderColor by animateColorAsState(if (selected) Primary else BgCardBorder.copy(alpha = .55f),
        animationSpec = tween(JeeMotion.standardMillis), label = "timer-preset-border")
    val labelColor by animateColorAsState(if (selected) PrimaryLight else TextPrimary,
        animationSpec = tween(JeeMotion.standardMillis), label = "timer-preset-label")
    Column(modifier.animateContentSize(animationSpec = tween(JeeMotion.standardMillis))
        .clip(RoundedCornerShape(15.dp)).background(containerColor)
        .border(1.dp, borderColor, RoundedCornerShape(15.dp)).premiumClick(onClick)
        .padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(sub, color = TextSecondary, fontSize = 10.sp)
    }
}
