package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.delay

private enum class TimerPreset(val label: String, val sublabel: String, val minutes: Int) {
    POMODORO("Pomodoro", "25 min", 25),
    SHORT("Short", "15 min", 15),
    LONG("Long", "50 min", 50),
    CUSTOM("Custom", "…", 25)
}

@Composable
fun StudyTimerScreen(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, onFabClick: () -> Unit = {}) {
    var selectedPreset by remember { mutableStateOf(TimerPreset.POMODORO) }
    var totalSeconds by remember { mutableStateOf(selectedPreset.minutes * 60) }
    var remainingSeconds by remember { mutableStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var showSessionsTab by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        } else if (remainingSeconds == 0) {
            isRunning = false
        }
    }

    fun applyPreset(preset: TimerPreset) {
        selectedPreset = preset
        totalSeconds = preset.minutes * 60
        remainingSeconds = totalSeconds
        isRunning = false
    }

    Scaffold(
        containerColor = BgApp,
        bottomBar = { BottomNavBar(selectedTab, onTabSelected, onFabClick) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Study session", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp))
                Row {
                    Icon(Icons.Filled.BarChart, "Stats", tint = TextSecondary)
                    Spacer(Modifier.width(14.dp))
                    Icon(Icons.Filled.MoreVert, "More", tint = TextSecondary)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BgCard).padding(4.dp)) {
                SegmentButton("Timer", !showSessionsTab, Modifier.weight(1f)) { showSessionsTab = false }
                SegmentButton("Sessions", showSessionsTab, Modifier.weight(1f)) { showSessionsTab = true }
            }
            Spacer(Modifier.height(28.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
                Canvas(Modifier.size(220.dp)) {
                    val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(BgDivider, -90f, 360f, false, style = stroke, size = Size(size.width, size.height))
                    drawArc(AccentBlue, -90f, 360f * progress, false, style = stroke, size = Size(size.width, size.height))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    Text(String.format("%02d:%02d", mins, secs), fontSize = 44.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("Focus on progress", color = TextMuted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgCard).padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(AccentBlueSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.TrackChanges, null, tint = AccentBlueLight, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Studying", color = TextMuted, fontSize = 11.sp)
                        Text("Kinematics", style = MaterialTheme.typography.titleMedium)
                        Text("Physics", color = TextMuted, fontSize = 11.sp)
                    }
                }
                Text("Change", color = AccentBlue, fontSize = 13.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimerPreset.values().forEach { preset ->
                    PresetCard(preset, selectedPreset == preset, Modifier.weight(1f)) { applyPreset(preset) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { isRunning = !isRunning },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Icon(if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(if (isRunning) "Pause" else "Start", color = Color.White, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(14.dp))
            Text("“Small steps every day lead to big results.”", color = TextMuted, fontSize = 12.sp, fontStyle = FontStyle.Italic, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(Modifier.then(modifier).clip(RoundedCornerShape(8.dp)).background(if (selected) AccentBlue else Color.Transparent).clickable(onClick = onClick).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) Color.White else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PresetCard(preset: TimerPreset, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val borderColor = if (selected) AccentBlue else Color.Transparent
    Column(
        Modifier.then(modifier).clip(RoundedCornerShape(12.dp)).background(BgCard).border(1.dp, borderColor, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(preset.label, color = if (selected) AccentBlue else TextOnCard, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(preset.sublabel, color = TextMuted, fontSize = 11.sp)
    }
}