package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Composable
fun DashboardScreen(repo: JeeRepository, selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit = {}, onOpenPlanner: () -> Unit = {}, onOpenRevision: () -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    var refresh by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val learning = remember { LearningRepository(context) }
    val intelligence = remember { JeeIntelligence(repo, learning) }
    val tasks = remember(refresh) { repo.getTasks() }
    val today = remember(refresh) { repo.getTodayMinutes() }
    val goal = remember(refresh) { repo.getDailyGoalMinutes() }
    val total = JeeRepository.syllabus.values.sumOf { it.size }
    val done = remember(refresh) { JeeRepository.syllabus.keys.sumOf { subject -> repo.chapters(subject).count { it.progress >= 1f } } }
    val prep = if (total == 0) 0f else done.toFloat() / total
    val due = remember(refresh) { repo.getRevisionQueue().size }
    val priorities = remember(refresh) { intelligence.dailyPriorities(3) }
    val todayTasks = tasks.filter { it.dueDay == "Today" }
    val next = todayTasks.firstOrNull { !it.done } ?: tasks.firstOrNull { !it.done }
    val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.of(2027, 1, 1)).coerceAtLeast(0)
    val greeting = when (LocalTime.now().hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }

    Scaffold(containerColor = BgApp, bottomBar = { BottomNavBar(selectedTab, onTabSelected, onAiClick) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(38.dp)) {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(AccentBlueSoft), Alignment.Center) { Icon(Icons.Filled.Person, null, tint = AccentBlueLight, modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.width(9.dp))
                    Column { Text(greeting, style = MaterialTheme.typography.titleMedium); Text("Let's make today count", color = TextMuted, fontSize = 11.sp) }
                }
                Icon(Icons.Filled.Refresh, "Refresh", tint = TextSecondary, modifier = Modifier.premiumClick { refresh++ })
            }
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BgCardAlt).padding(18.dp)) {
                Text("JEE 2027", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.Bottom) { Text(days.toString(), style = MaterialTheme.typography.displaySmall); Spacer(Modifier.width(5.dp)); Text("days to 2027", color = TextMuted, fontSize = 12.sp) }
                Spacer(Modifier.height(12.dp)); Text("Preparation", color = TextSecondary, fontSize = 11.sp); Text("${(prep * 100).toInt()}%", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(6.dp)); LinearStatBar(prep)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Today", "${today / 60}h ${today % 60}m", "of ${goal / 60}h goal", Modifier.weight(1f)); MetricTile("Syllabus", "${(prep * 100).toInt()}%", "$done chapters", Modifier.weight(1f)); MetricTile("Revision", "$due", "due", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { ActionTile("Today's plan", "Open planner", Modifier.weight(1f), onOpenPlanner); ActionTile("Revision", "$due due", Modifier.weight(1f), onOpenRevision) }
            if (priorities.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCard).padding(15.dp)) {
                    Text("TODAY'S PRIORITIES", color = TextSecondary, fontSize = 11.sp); Spacer(Modifier.height(8.dp))
                    priorities.forEachIndexed { index, priority ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}", color = AccentBlue, fontSize = 12.sp); Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) { Text(priority.title, fontSize = 13.sp); Text(priority.reason, color = TextMuted, fontSize = 10.sp) }
                            Text("${priority.durationMin}m", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (next != null) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgPriority).padding(15.dp).premiumClick { onTabSelected(AppTab.TASKS) }) {
                    Text("DO THIS NOW", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Medium); Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(next.title, style = MaterialTheme.typography.titleMedium); Text("${next.subject} · ${next.durationMin}m", color = TextSecondary, fontSize = 11.sp) }
                        Box(Modifier.size(34.dp).clip(CircleShape).background(AccentBlue), Alignment.Center) { Icon(Icons.Filled.ArrowForward, null, tint = Color(0xFF17120A), modifier = Modifier.size(16.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp)); SectionHeader("Today's tasks")
            todayTasks.forEachIndexed { index, task ->
                TaskRow(TaskItem(task.id.toString(), task.title, task.subject, "${task.durationMin}m", task.done)) { id -> repo.toggleTask(id.toLong()); refresh++ }
                if (index < todayTasks.lastIndex) HorizontalDivider(color = BgDivider, thickness = .5.dp)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable private fun MetricTile(label: String, value: String, sub: String, modifier: Modifier) { Column(modifier.clip(RoundedCornerShape(16.dp)).background(BgCard).padding(13.dp)) { Text(label, color = TextSecondary, fontSize = 10.sp); Text(value, style = MaterialTheme.typography.titleMedium); Text(sub, color = TextMuted, fontSize = 9.sp) } }
@Composable private fun ActionTile(label: String, value: String, modifier: Modifier, onClick: () -> Unit) { Column(modifier.clip(RoundedCornerShape(16.dp)).background(BgCard).premiumClick(onClick).padding(14.dp)) { Text(label, color = TextSecondary, fontSize = 10.sp); Text(value, style = MaterialTheme.typography.titleMedium); Text("From your study data", color = TextMuted, fontSize = 9.sp) } }
