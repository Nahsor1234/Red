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
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Composable
fun DashboardScreen(
    repo: JeeRepository,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onFabClick: () -> Unit = {}
) {
    var refresh by remember { mutableIntStateOf(0) }

    val tasks = remember(refresh) { repo.getTasks() }
    val today = remember(refresh) { repo.getTodayMinutes() }
    val goal = remember(refresh) { repo.getDailyGoalMinutes() }
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    val completed = tasks.count { it.done }
    val total = JeeRepository.syllabus.values.sumOf { it.size }
    val doneChapters = remember(refresh) {
        JeeRepository.syllabus.keys.sumOf { subject ->
            repo.chapters(subject).count { it.progress >= 1f }
        }
    }
    val prep = if (total == 0) 0f else doneChapters.toFloat() / total
    val daysLeft = ChronoUnit.DAYS
        .between(LocalDate.now(), LocalDate.of(2027, 1, 1))
        .coerceAtLeast(0)
    val todayTasks = tasks.filter { it.dueDay == "Today" }
    val nextTask = todayTasks.firstOrNull { !it.done }
        ?: tasks.firstOrNull { !it.done }

    Scaffold(
        containerColor = BgApp,
        bottomBar = { BottomNavBar(selectedTab, onTabSelected, onFabClick) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AccentBlueSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            null,
                            tint = AccentBlueLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(greeting, style = MaterialTheme.typography.titleMedium)
                        Text("Let's make today count", color = TextMuted, fontSize = 12.sp)
                    }
                }
                Icon(
                    Icons.Filled.Refresh,
                    "Refresh",
                    tint = TextSecondary,
                    modifier = Modifier.premiumClick { refresh++ }
                )
            }

            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCardAlt)
                    .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Bolt,
                                null,
                                tint = AccentBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "JEE 2027",
                                color = AccentBlueLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(daysLeft.toString(), style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.width(4.dp))
                            Text("days to 2027", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                    Text(
                        "Live from your study data",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.widthIn(max = 110.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearStatBar(prep)
            }

            Spacer(Modifier.height(16.dp))
            Text("Overall preparation", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text((prep * 100).toInt().toString() + "%", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            LinearStatBar(prep)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(doneChapters.toString() + " chapters complete", color = TextMuted, fontSize = 11.sp)
                Text(completed.toString() + " tasks complete", color = TextMuted, fontSize = 11.sp)
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .padding(14.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Column {
                    Text("Today's goal", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            (today / 60).toString() + "h " + (today % 60).toString() + "m",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            " / " + (goal / 60).toString() + "h",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(BgDivider),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = {
                            if (goal > 0) (today.toFloat() / goal).coerceIn(0f, 1f) else 0f
                        },
                        modifier = Modifier.size(46.dp),
                        color = AccentGreen,
                        trackColor = BgDivider,
                        strokeWidth = 4.dp
                    )
                    Text(
                        (if (goal > 0) (today * 100 / goal).coerceIn(0, 100) else 0).toString() + "%",
                        fontSize = 10.sp,
                        color = AccentGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            if (nextTask != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgPriority)
                        .premiumClick { onTabSelected(AppTab.TASKS) }
                        .padding(14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Bolt,
                                null,
                                tint = TextAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Do this now",
                                color = TextAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BgPriorityBadge)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Next task", color = TextPriorityBadge, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Column {
                            Text(nextTask.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                nextTask.subject + " · " + nextTask.durationMin + "m",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(AccentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.ArrowForward,
                                "Open tasks",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionHeader("Today's tasks")
            todayTasks.forEachIndexed { index, task ->
                TaskRow(
                    TaskItem(
                        task.id.toString(),
                        task.title,
                        task.subject,
                        task.durationMin.toString() + "m",
                        task.done
                    )
                ) { id ->
                    repo.toggleTask(id.toLong())
                    refresh++
                }
                if (index < todayTasks.size - 1) {
                    HorizontalDivider(color = BgDivider, thickness = 0.5.dp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
