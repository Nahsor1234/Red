package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
fun DashboardScreen(repo: JeeRepository, selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit = {}, onOpenPlanner: () -> Unit = {}, onOpenRevision: () -> Unit = {}, onOpenSettings: () -> Unit = {}, onOpenAssessment: () -> Unit = {}, onOpenMistakes: () -> Unit = {}) {
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
            Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(42.dp)) { Box(Modifier.size(42.dp).clip(CircleShape).background(AccentBlueSoft), Alignment.Center) { Icon(Icons.Filled.Person, null, tint = AccentBlueLight, modifier = Modifier.size(19.dp)) } }
                    Spacer(Modifier.width(10.dp)); Column { Text(greeting, style = MaterialTheme.typography.titleMedium); Text("Let's make today count", color = TextSecondary, fontSize = 12.sp) }
                }
                Icon(Icons.Filled.Refresh, "Refresh", tint = TextSecondary, modifier = Modifier.premiumClick { refresh++ })
            }
            JeeCard(featured = true) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) { Column { Text("⚡  JEE 2027", color = AccentBlueLight, fontSize = 14.sp, fontWeight = FontWeight.Medium); Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.Bottom) { Text(days.toString(), style = MaterialTheme.typography.displaySmall); Spacer(Modifier.width(6.dp)); Text("days to 2027", color = TextSecondary, fontSize = 13.sp) } }; Text("Live from your study\ndata", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp) }
                Spacer(Modifier.height(16.dp)); LinearStatBar(prep, height = 6.dp)
            }
            Spacer(Modifier.height(24.dp)); SectionHeader("Preparation", "Overall")
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) { Text("${(prep * 100).toInt()}%", style = MaterialTheme.typography.displaySmall); Text("$done chapters complete · ${todayTasks.count { it.done }} tasks today", color = TextSecondary, fontSize = 12.sp) }
            Spacer(Modifier.height(9.dp)); LinearStatBar(prep, height = 7.dp)
            Spacer(Modifier.height(24.dp)); SectionHeader("Today", LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() })
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                JeeCard { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column { Text("Study goal", color = TextSecondary, fontSize = 13.sp); Spacer(Modifier.height(4.dp)); Text("${today / 60}h ${today % 60}m", style = MaterialTheme.typography.titleLarge); Text("/ ${goal / 60}h", color = TextMuted, fontSize = 12.sp) }; Box(Modifier.size(54.dp).clip(CircleShape).background(BgCardAlt), Alignment.Center) { Text("${if (goal == 0) 0 else (today * 100 / goal).coerceIn(0, 100)}%", color = AccentGreen, fontSize = 13.sp) } } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { HomeActionCard("Plan", "Open planner", "Auto-built from study state", Modifier.weight(1f), onOpenPlanner); HomeActionCard("Revision", "$due due", "Spaced review queue", Modifier.weight(1f), onOpenRevision) }
            }
            Spacer(Modifier.height(10.dp)); JeeCard(featured = true) { Text("Today's priorities", style = MaterialTheme.typography.titleLarge); Text("What matters most right now, based on your data.", color = TextMuted, fontSize = 12.sp); Spacer(Modifier.height(8.dp)); if (priorities.isEmpty()) Text("No priority generated yet.", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 10.dp)) else priorities.forEachIndexed { index, priority -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("${index + 1}", color = AccentBlue, fontSize = 13.sp, modifier = Modifier.width(18.dp)); Column(Modifier.weight(1f)) { Text(priority.title, fontSize = 14.sp); Text(priority.reason, color = TextMuted, fontSize = 11.sp) }; Text("${priority.durationMin}m", color = TextSecondary, fontSize = 12.sp) } } }
            if (next != null) { Spacer(Modifier.height(10.dp)); JeeCard(featured = true) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("⚡  DO THIS NOW", color = AccentBlueLight, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text("Next task", color = AccentPink, fontSize = 12.sp) }; Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(next.title, style = MaterialTheme.typography.titleMedium); Text("${next.subject} · ${next.durationMin} min", color = TextSecondary, fontSize = 12.sp) }; Box(Modifier.size(42.dp).clip(CircleShape).background(AccentBlue).premiumClick { onTabSelected(AppTab.TASKS) }, Alignment.Center) { Icon(Icons.Filled.ArrowForward, null, tint = Color(0xFF17120A), modifier = Modifier.size(19.dp)) } } } }
            Spacer(Modifier.height(24.dp)); SectionHeader("Practice & mistakes"); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { HomeActionCard("Questions practice", "Practice by subject", "Build question volume", Modifier.weight(1f), onOpenAssessment); HomeActionCard("Mistake bank", "Review weak patterns", "Recurring errors", Modifier.weight(1f), onOpenMistakes) }
            Spacer(Modifier.height(24.dp)); SectionHeader("Today's tasks", "See all") { onTabSelected(AppTab.TASKS) }
            if (todayTasks.isEmpty()) Text("No tasks scheduled for today.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp)) else todayTasks.take(4).forEachIndexed { index, task -> TaskRow(TaskItem(task.id.toString(), task.title, task.subject, "${task.durationMin}m", task.done), onToggle = { id -> repo.toggleTask(id.toLong()); refresh++ }, onClick = { onTabSelected(AppTab.TASKS) }); if (index < minOf(todayTasks.size, 4) - 1) HorizontalDivider(color = BgDivider, thickness = .5.dp) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeActionCard(title: String, value: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(JeeShapes.medium).background(BgCard).border(1.dp, BgCardBorder.copy(alpha = .72f), JeeShapes.medium).premiumClick(onClick).padding(16.dp)) { Text(title, color = TextSecondary, fontSize = 13.sp); Spacer(Modifier.height(4.dp)); Text(value, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = TextMuted, fontSize = 10.sp, maxLines = 1) }
}
