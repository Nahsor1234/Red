package com.example.jeecommandcenter.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.ceil

private enum class TimerPreset(val label: String, val sublabel: String, val minutes: Int) {
    POMODORO("Pomodoro", "25 min", 25),
    SHORT("Short", "15 min", 15),
    LONG("Long", "50 min", 50)
}

@Composable
fun StudyTimerScreen(
    repo: JeeRepository,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onAiClick: () -> Unit = {}
) {
    val initialState = remember { repo.restoreTimerState() }
    var selectedPreset by remember {
        mutableStateOf(
            TimerPreset.values().firstOrNull { it.minutes * 60 == initialState.totalSeconds }
        )
    }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customMinutes by remember {
        mutableStateOf((initialState.totalSeconds / 60).coerceAtLeast(1).toString())
    }
    var totalSeconds by remember { mutableIntStateOf(initialState.totalSeconds) }
    var remaining by remember { mutableIntStateOf(initialState.remainingSeconds) }
    var running by remember { mutableStateOf(initialState.running) }
    var endAtMillis by remember { mutableLongStateOf(initialState.endAtMillis) }
    var sessionsTab by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val view = LocalView.current

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect

        while (running) {
            val remainingNow = ceil(
                (endAtMillis - System.currentTimeMillis()).coerceAtLeast(0L) / 1000.0
            ).toInt().coerceIn(0, totalSeconds)

            if (remainingNow != remaining) {
                remaining = remainingNow
            }

            if (remainingNow == 0) {
                running = false
                endAtMillis = 0L
                repo.completeTimer(totalSeconds)
                refresh++
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                break
            }

            delay(250)
        }
    }

    fun resetToPreset(preset: TimerPreset) {
        selectedPreset = preset
        totalSeconds = preset.minutes * 60
        remaining = totalSeconds
        running = false
        endAtMillis = 0L
        repo.resetTimer(totalSeconds)
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Scaffold(
        containerColor = BgApp,
        bottomBar = { BottomNavBar(selectedTab, onTabSelected, onAiClick) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    "Study session",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp)
                )
                Text(
                    remember(refresh) { repo.getTodayMinutes() }.toString() + " min today",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgCard)
                    .padding(4.dp)
            ) {
                SegmentButton("Timer", !sessionsTab, Modifier.weight(1f)) { sessionsTab = false }
                SegmentButton("Sessions", sessionsTab, Modifier.weight(1f)) { sessionsTab = true }
            }

            if (sessionsTab) {
                Spacer(Modifier.height(16.dp))
                val sessions = remember(refresh) { repo.getSessions() }
                LazyColumn(Modifier.weight(1f)) {
                    items(sessions, key = { it.id }) { session ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(session.subject, color = TextOnCard, fontSize = 13.sp)
                                Text(session.chapter, color = TextMuted, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    session.minutes.toString() + " min",
                                    color = AccentGreen,
                                    fontSize = 12.sp
                                )
                                Text(session.date, color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            } else {
                Spacer(Modifier.height(28.dp))
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = if (totalSeconds > 0) {
                        remaining.toFloat() / totalSeconds
                    } else {
                        0f
                    }

                    Canvas(Modifier.size(220.dp)) {
                        val stroke = Stroke(
                            width = 14.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawArc(BgDivider, -90f, 360f, false, style = stroke)
                        drawArc(
                            AccentBlue,
                            -90f,
                            360f * progress,
                            false,
                            style = stroke
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            if (running) "Focus mode" else "Ready",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .padding(14.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Column {
                        Text("Session subject", color = TextMuted, fontSize = 11.sp)
                        Text("General study", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Saved automatically when the timer completes",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimerPreset.values().forEach { preset ->
                        PresetCard(
                            preset,
                            selectedPreset == preset,
                            Modifier.weight(1f)
                        ) {
                            resetToPreset(preset)
                        }
                    }
                    CustomPresetCard(
                        selected = selectedPreset == null,
                        minutes = totalSeconds / 60,
                        modifier = Modifier.weight(1f)
                    ) {
                        customMinutes = (totalSeconds / 60).coerceAtLeast(1).toString()
                        showCustomDialog = true
                    }
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (running) {
                            remaining = ceil(
                                (endAtMillis - System.currentTimeMillis())
                                    .coerceAtLeast(0L) / 1000.0
                            ).toInt().coerceIn(0, totalSeconds)
                            running = false
                            endAtMillis = 0L
                            repo.pauseTimer(totalSeconds, remaining)
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } else {
                            if (remaining <= 0) remaining = totalSeconds
                            repo.startTimer(totalSeconds, remaining)
                            endAtMillis = System.currentTimeMillis() + remaining * 1000L
                            running = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(
                        if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) "Pause" else "Start", color = Color.White)
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "Completed sessions are added to your dashboard automatically.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom timer") },
            text = {
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { customMinutes = it.filter(Char::isDigit).take(4) },
                    label = { Text("Minutes") },
                    supportingText = { Text("1–1440 minutes") },
                    singleLine = true
                )
            },
            confirmButton = {
                val minutes = customMinutes.toIntOrNull()
                TextButton(
                    enabled = minutes != null && minutes in 1..1440,
                    onClick = {
                        minutes?.let {
                            selectedPreset = null
                            totalSeconds = it * 60
                            remaining = totalSeconds
                            running = false
                            endAtMillis = 0L
                            repo.resetTimer(totalSeconds)
                            showCustomDialog = false
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    }
                ) { Text("Use") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SegmentButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AccentBlue else Color.Transparent)
            .premiumClick(onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PresetCard(
    preset: TimerPreset,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(
                1.dp,
                if (selected) AccentBlue else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .premiumClick(onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            preset.label,
            color = if (selected) AccentBlue else TextOnCard,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(preset.sublabel, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun CustomPresetCard(
    selected: Boolean,
    minutes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(
                1.dp,
                if (selected) AccentBlue else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .premiumClick(onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Custom",
            color = if (selected) AccentBlue else TextOnCard,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text("$minutes min", color = TextMuted, fontSize = 11.sp)
    }
}
