package com.example.jeecommandcenter.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.util.Locale
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
    var subject by remember { mutableStateOf("General") }
    var chapter by remember { mutableStateOf<String?>(null) }
    var activity by remember { mutableStateOf(ActivityType.LEARNING) }
    var showContext by remember { mutableStateOf(false) }
    val view = LocalView.current
    val subjects = listOf("General", "Physics", "Chemistry", "Mathematics")
    val chapters = remember(subject) { if (subject == "General") emptyList() else JeeCatalog.forSubject(subject) }
    val recentSessions = remember(refresh) { repo.getSessions().take(6) }

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
            delay(250)
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

    Scaffold(containerColor = BgApp, bottomBar = { BottomNavBar(selectedTab, onTabSelected, onAiClick) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Study session", style = MaterialTheme.typography.headlineSmall)
                    Text(repo.getTodayMinutes().toString() + " min today", color = TextMuted, fontSize = 11.sp)
                }
            }
            item {
                Box(Modifier.fillMaxWidth().height(250.dp), Alignment.Center) {
                    val progress = if (total > 0) remaining.toFloat() / total else 0f
                    Canvas(Modifier.size(224.dp)) {
                        val st = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                        drawArc(BgDivider, -90f, 360f, false, style = st)
                        drawArc(AccentBlue, -90f, 360f * progress, false, style = st)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60), fontSize = 46.sp, fontWeight = FontWeight.Normal)
                        Text(if (running) "Focus mode" else "Ready", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCardAlt)
                        .border(JeeSurfaceTokens.borderWidth,BgCardBorder.copy(alpha=JeeSurfaceTokens.cardBorderAlpha),JeeShapes.medium).padding(15.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("SESSION CONTEXT", color = TextSecondary, fontSize = 11.sp)
                        TextButton(enabled = !running, onClick = { showContext = true }) { Text("Change", fontSize = 11.sp) }
                    }
                    Text(subject, style = MaterialTheme.typography.titleMedium)
                    Text(chapter?.let { JeeCatalog.find(it)?.name } ?: "No chapter linked", color = TextMuted, fontSize = 11.sp)
                    Text("Activity: " + activity.name.lowercase().replace('_', ' '), color = TextMuted, fontSize = 10.sp)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimerPreset.values().forEach { p -> Preset(p.label, p.sublabel, selected == p, Modifier.weight(1f)) { preset(p) } }
                    Preset("Custom", (total / 60).toString() + " min", selected == null, Modifier.weight(1f)) { custom = (total / 60).coerceAtLeast(1).toString(); showCustom = true }
                }
            }
            item {
                Button(
                    onClick = {
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
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color(0xFF17120A))
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) "Pause" else "Start session", color = Color(0xFF17120A))
                }
            }
            item { SectionHeader("Recent sessions") }
            if (recentSessions.isEmpty()) {
                item {
                    JeeCard {
                        Text("No completed sessions yet.", color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Completed sessions will appear here after your first study block.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            } else {
                items(recentSessions, key = { it.id }) { session ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgCard)
                            .border(1.dp,BgCardBorder.copy(alpha=.72f),RoundedCornerShape(14.dp))
                            .padding(horizontal=14.dp, vertical=11.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(session.subject, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(session.chapter + " · " + session.activityType.name.lowercase().replace('_', ' '), color = TextMuted, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(session.minutes.toString() + " min", color = PrimaryLight, fontSize = 12.sp)
                            Text(session.date, color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }

    if (showContext) {
        AlertDialog(
            onDismissRequest = { showContext = false },
            title = { Text("Study context") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Subject", color = TextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjects.forEach { value -> JeeFilterChip(value, value == subject) { subject = value; chapter = null } }
                    }
                    if (subject != "General") {
                        Text("Chapter", color = TextSecondary, fontSize = 12.sp)
                        LazyColumn(Modifier.fillMaxWidth().height(220.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(chapters, key = { it.id }) { value ->
                                TextButton(onClick = { chapter = value.id }) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text("\${value.number}. \${value.name}", color = if (chapter == value.id) PrimaryLight else TextOnCard)
                                        if (chapter == value.id) Text("Selected", color = PrimaryLight, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                    Text("Activity", color = TextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ActivityType.LEARNING, ActivityType.PRACTICE, ActivityType.REVISION, ActivityType.TEST).forEach { value ->
                            JeeFilterChip(value.name.lowercase().replace('_', ' '), value == activity) { activity = value }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showContext = false }) { Text("Done") } }
        )
    }

    if (showCustom) {
        AlertDialog(
            onDismissRequest = { showCustom = false },
            title = { Text("Custom timer") },
            text = { OutlinedTextField(custom, { custom = it.filter(Char::isDigit).take(4) }, label = { Text("Minutes") }, singleLine = true) },
            confirmButton = {
                val m = custom.toIntOrNull()
                TextButton(enabled = m != null && m in 1..1440, onClick = {
                    m?.let {
                        selected = null
                        total = it * 60
                        remaining = total
                        running = false
                        end = 0
                        repo.resetTimer(total)
                        showCustom = false
                    }
                }) { Text("Use") }
            },
            dismissButton = { TextButton(onClick = { showCustom = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Preset(label: String, sub: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp)).background(BgCard)
            .border(1.dp, if (selected) AccentBlue else Color.Transparent, RoundedCornerShape(15.dp))
            .premiumClick(onClick).padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = if (selected) AccentBlue else TextOnCard, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(sub, color = TextMuted, fontSize = 10.sp)
    }
}
