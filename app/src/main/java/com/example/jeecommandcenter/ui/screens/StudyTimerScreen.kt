package com.example.jeecommandcenter.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import kotlin.math.ceil

private enum class TimerPreset(val minutes: Int) {
    QUICK(15), POMODORO(25), EXTENDED(45), DEEP(60)
}

@Composable
fun StudyTimerScreen(
    repo: JeeRepository,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onAiClick: () -> Unit = {}
) {
    val initial = remember { repo.restoreTimerState() }
    var selected by remember {
        mutableStateOf(TimerPreset.values().firstOrNull { it.minutes * 60 == initial.totalSeconds })
    }
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
            val next = ceil((end - System.currentTimeMillis()).coerceAtLeast(0L) / 1000.0)
                .toInt().coerceIn(0, total)
            remaining = next
            if (next == 0) {
                running = false
                end = 0L
                repo.completeTimer(total, subject, chapter, activity)
                refresh++
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                break
            }
            delay(1000)
        }
    }

    fun choosePreset(preset: TimerPreset) {
        selected = preset
        total = preset.minutes * 60
        remaining = total
        running = false
        end = 0L
        repo.resetTimer(total)
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Scaffold(
        containerColor = BgAppBase,
        bottomBar = { AnimatedBottomNavBar(selectedTab, onTabSelected, visible = selectedTab != AppTab.AI) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = JeeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(JeeSpacing.md),
            contentPadding = PaddingValues(top = JeeSpacing.md, bottom = JeeSpacing.xl)
        ) {
            item {
                Column {
                    Text("Focus", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (sessionsLoaded) "$todayMinutes min studied today" else "Loading study total…",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SESSION CONTEXT", color = TextSecondary, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            buildString {
                                append(subject)
                                chapter?.let { id -> JeeCatalog.find(id)?.let { append(" · ").append(it.name) } }
                            },
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2
                        )
                        Text(activity.name.lowercase().replace('_', ' '), color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(
                        enabled = !running,
                        onClick = {
                            contextExpanded = !contextExpanded
                            if (!contextExpanded) chapterPickerExpanded = false
                        }
                    ) { Text(if (contextExpanded) "Done" else "Edit") }
                }
                AnimatedVisibility(
                    visible = contextExpanded,
                    enter = androidx.compose.animation.fadeIn(tween(JeeMotion.fastMillis)) +
                        androidx.compose.animation.expandVertically(tween(JeeMotion.layoutMillis)),
                    exit = androidx.compose.animation.fadeOut(tween(JeeMotion.fastMillis)) +
                        androidx.compose.animation.shrinkVertically(tween(JeeMotion.layoutMillis))
                ) {
                    Column(
                        Modifier.fillMaxWidth().clip(JeeShapes.medium).background(BgCard)
                            .border(1.dp, BgCardBorder, JeeShapes.medium).padding(JeeSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(JeeSpacing.sm)
                    ) {
                        Text("Subject", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(JeeSpacing.xs)) {
                            subjects.forEach { value ->
                                JeeFilterChip(value, value == subject) {
                                    subject = value
                                    chapter = null
                                    chapterPickerExpanded = false
                                }
                            }
                        }
                        Text("Chapter", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        Surface(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BgCardBorder, JeeShapes.small)
                                .premiumClick { if (subject != "General") chapterPickerExpanded = !chapterPickerExpanded },
                            color = BgAppBase,
                            shape = JeeShapes.small
                        ) {
                            Row(
                                Modifier.padding(horizontal = JeeSpacing.md, vertical = JeeSpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    chapter?.let { JeeCatalog.find(it)?.name }
                                        ?: if (subject == "General") "General study" else "Select chapter",
                                    color = if (chapter != null) TextPrimary else TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                                Icon(
                                    if (chapterPickerExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "Choose chapter",
                                    tint = TextSecondary
                                )
                            }
                        }
                        AnimatedVisibility(chapterPickerExpanded && subject != "General") {
                            Column(Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                                chapters.forEach { chapterItem ->
                                    TextButton(
                                        onClick = { chapter = chapterItem.id; chapterPickerExpanded = false },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                            Text(
                                                "${chapterItem.number}. ${chapterItem.name}",
                                                color = if (chapter == chapterItem.id) PrimaryLight else TextPrimary,
                                                maxLines = 1
                                            )
                                            if (chapter == chapterItem.id) Icon(Icons.Filled.Check, null, tint = PrimaryLight)
                                        }
                                    }
                                }
                            }
                        }
                        Text("Activity", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(JeeSpacing.xs)) {
                            listOf(ActivityType.LEARNING, ActivityType.PRACTICE, ActivityType.REVISION, ActivityType.TEST)
                                .forEach { value ->
                                    JeeFilterChip(value.name.lowercase().replace('_', ' '), value == activity) {
                                        activity = value
                                    }
                                }
                        }
                    }
                }
            }
            item {
                Box(Modifier.fillMaxWidth().height(272.dp), contentAlignment = Alignment.Center) {
                    val fraction = if (total > 0) remaining.toFloat() / total else 0f
                    val animatedFraction by animateFloatAsState(
                        fraction.coerceIn(0f, 1f),
                        animationSpec = tween(JeeMotion.timerProgressMillis),
                        label = "focus-ring-progress"
                    )
                    Canvas(Modifier.size(240.dp)) {
                        val stroke = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
                        drawArc(BgDivider, -90f, 360f, false, style = stroke)
                        drawArc(Primary, -90f, 360f * animatedFraction, false, style = stroke)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontFeatureSettings = "tnum"
                            ),
                            fontWeight = FontWeight.Normal
                        )
                        AnimatedContent(
                            targetState = when {
                                running -> "Focusing"
                                remaining == 0 -> "Session complete"
                                remaining < total -> "Paused"
                                else -> "Ready"
                            },
                            label = "focus-state"
                        ) { state ->
                            Text(state, color = if (running) PrimaryLight else TextSecondary,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(JeeSpacing.xs)) {
                    TimerPreset.values().forEach { preset ->
                        Preset(preset.minutes.toString(), selected == preset, Modifier.weight(1f)) {
                            choosePreset(preset)
                        }
                    }
                    Preset("Custom", selected == null, Modifier.weight(1.15f)) {
                        custom = (total / 60).coerceAtLeast(1).toString()
                        showCustom = true
                    }
                }
                Text("Minutes", color = TextSecondary, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = JeeSpacing.xs))
            }
            item {
                Button(
                    onClick = {
                        val wasRunning = running
                        if (wasRunning) {
                            remaining = ceil((end - System.currentTimeMillis()).coerceAtLeast(0L) / 1000.0)
                                .toInt().coerceIn(0, total)
                            running = false
                            end = 0L
                            repo.pauseTimer(total, remaining)
                        } else {
                            if (remaining <= 0) remaining = total
                            repo.startTimer(total, remaining)
                            end = System.currentTimeMillis() + remaining * 1000L
                            running = true
                        }
                        view.performHapticFeedback(
                            if (wasRunning) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.CONTEXT_CLICK
                        )
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = JeeShapes.medium
                ) {
                    AnimatedContent(targetState = running, label = "focus-primary-action") { active ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (active) Icons.Filled.Pause else Icons.Filled.PlayArrow, null)
                            Spacer(Modifier.width(JeeSpacing.sm))
                            Text(
                                when {
                                    active -> "Pause session"
                                    remaining < total && remaining > 0 -> "Resume session"
                                    remaining == 0 -> "Start again"
                                    else -> "Start focus session"
                                }
                            )
                        }
                    }
                }
            }
            item {
                SectionHeader("Recent sessions")
                if (!sessionsLoaded) {
                    Text("Loading sessions…", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                } else if (recentSessions.isEmpty()) {
                    Text(
                        "Completed focus sessions will appear here.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = JeeSpacing.sm)
                    )
                }
            }
            items(recentSessions, key = { it.id }) { session ->
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = JeeSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(session.subject, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${session.chapter} · ${session.activityType.name.lowercase().replace('_', ' ')}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${session.minutes} min", color = PrimaryLight, style = MaterialTheme.typography.labelLarge)
                            Text(session.date, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    HorizontalDivider(color = BgDivider.copy(alpha = 0.6f), thickness = 0.7.dp)
                }
            }
        }
    }

    if (showCustom) {
        AlertDialog(
            onDismissRequest = { showCustom = false },
            title = { Text("Custom focus") },
            text = {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.filter(Char::isDigit).take(4) },
                    label = { Text("Minutes") },
                    singleLine = true
                )
            },
            confirmButton = {
                val minutes = custom.toIntOrNull()
                TextButton(
                    enabled = minutes != null && minutes in 1..1440,
                    onClick = {
                        minutes?.let {
                            selected = null
                            total = it * 60
                            remaining = total
                            running = false
                            end = 0L
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
private fun Preset(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val containerColor by animateColorAsState(
        if (selected) PrimarySoft else BgCard,
        animationSpec = tween(JeeMotion.standardMillis),
        label = "focus-preset-surface"
    )
    val borderColor by animateColorAsState(
        if (selected) Primary else BgCardBorder.copy(alpha = 0.7f),
        animationSpec = tween(JeeMotion.standardMillis),
        label = "focus-preset-border"
    )
    val labelColor by animateColorAsState(
        if (selected) PrimaryLight else TextPrimary,
        animationSpec = tween(JeeMotion.standardMillis),
        label = "focus-preset-label"
    )
    Column(
        modifier.heightIn(min = 48.dp).clip(JeeShapes.small).background(containerColor)
            .border(1.dp, borderColor, JeeShapes.small).premiumClick(onClick)
            .padding(vertical = JeeSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = labelColor, style = MaterialTheme.typography.labelLarge)
    }
}
