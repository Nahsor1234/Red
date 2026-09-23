package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun DashboardScreen(
    repo: JeeRepository,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onAiClick: () -> Unit = {},
    onOpenPlanner: () -> Unit = {},
    onOpenRevision: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAssessment: () -> Unit = {},
    onOpenMistakes: () -> Unit = {},
    onOpenStudyTimer: () -> Unit = {}
) {
    var refresh by remember { mutableIntStateOf(0) }
    var showFocusPicker by remember { mutableStateOf(false) }
    var focusSubject by remember { mutableStateOf("Physics") }
    var selectedFocusId by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val learning = remember { LearningRepository(context) }
    val intelligence = remember { JeeIntelligence(repo, learning) }
    val tasks = remember(refresh) { repo.getTasks() }
    val todayMinutes = remember(refresh) { repo.getTodayMinutes() }
    val goalMinutes = remember(refresh) { repo.getDailyGoalMinutes() }
    val totalChapters = JeeRepository.syllabus.values.sumOf { it.size }
    val completedChapters = remember(refresh) { JeeRepository.syllabus.keys.sumOf { subject -> repo.chapters(subject).count { it.progress >= 1f } } }
    val coverage = if (totalChapters == 0) 0f else completedChapters.toFloat() / totalChapters
    val revisionDue = remember(refresh) { repo.getRevisionQueue().size }
    val priorities = remember(refresh) { intelligence.dailyPriorities(3) }
    val weak = remember(refresh) { intelligence.weakChapters(1).firstOrNull() }
    val todayTasks = tasks.filter { it.dueDay == "Today" }
    val nextTask = todayTasks.firstOrNull { !it.done } ?: tasks.firstOrNull { !it.done }
    val autoFocus = priorities.firstOrNull()
    val selectedFocusChapter = selectedFocusId?.let { JeeCatalog.find(it) }
    val focus = selectedFocusChapter?.let { chapter ->
        val progress = repo.chapters(focusSubject).firstOrNull { it.number == chapter.number }
        FocusDisplay(chapter.name, if (progress == null) "Selected chapter · start building a study signal." else "${(progress.progress * 100).toInt()}% complete · confidence ${progress.confidence}/5", 30, chapter.subject)
    } ?: autoFocus?.let { FocusDisplay(it.title, it.reason, it.durationMin, it.subject) }
    val unresolvedMistakes = remember(refresh) { learning.getMistakes().count { !it.resolved } }
    val questionsAttempted = remember(refresh) { learning.getQuestionAttempts().size }
    val daysToJee = remember { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.of(2027, 1, 22)).coerceAtLeast(0) }
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    Box(Modifier.fillMaxSize()) {
        JeeBackground()
        Scaffold(containerColor = BgApp.copy(alpha = 0f), bottomBar = { BottomNavBar(selectedTab, onTabSelected, onAiClick) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text(greeting, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text("JEE 2027 · focused preparation", color = TextSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Settings", tint = TextPrimary, modifier = Modifier.size(24.dp)) }
                }

                JeeCard {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text("JEE MAIN 2027 · SESSION 1", color = PrimaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(daysToJee.toString(), fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                            Text("days to first scheduled exam day", color = TextSecondary, fontSize = 11.sp)
                        }
                        Surface(shape = RoundedCornerShape(14.dp), color = PrimarySoft, border = BorderStroke(1.dp, BgCardBorder.copy(alpha = .75f))) { Icon(Icons.Filled.Event, "JEE Main 2027 countdown", tint = PrimaryLight, modifier = Modifier.padding(12.dp).size(22.dp)) }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text("Tentative NTA schedule · Jan 22–24 and Jan 28–30, 2027", color = TextMuted, fontSize = 9.sp)
                }

                Spacer(Modifier.height(16.dp))
                SectionHeader("Quick actions")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("Practice", questionsAttempted.toString() + " questions", Icons.Filled.Quiz, Modifier.weight(1f), true, onOpenAssessment)
                    QuickAction("Revision", revisionDue.toString() + " due", Icons.Filled.Replay, Modifier.weight(1f), false, onOpenRevision)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("Mistakes", unresolvedMistakes.toString() + " unresolved", Icons.Filled.ErrorOutline, Modifier.weight(1f), false, onOpenMistakes)
                    QuickAction("Study", (todayMinutes / 60).toString() + "h " + (todayMinutes % 60).toString() + "m today", Icons.Filled.Timer, Modifier.weight(1f), false, onOpenStudyTimer)
                }

                Spacer(Modifier.height(18.dp))
                SectionHeader("Today at a glance")
                JeeCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HomeMetric((todayMinutes / 60).toString() + "h " + (todayMinutes % 60).toString() + "m", "Study", Modifier.weight(1f))
                        HomeMetric(todayTasks.count { it.done }.toString() + "/" + todayTasks.size, "Tasks", Modifier.weight(1f))
                        HomeMetric(revisionDue.toString(), "Due", Modifier.weight(1f))
                        HomeMetric((coverage * 100).toInt().toString() + "%", "Coverage", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearStatBar(if (goalMinutes == 0) 0f else (todayMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f), height = 6.dp, fillColor = Primary)
                    Spacer(Modifier.height(6.dp))
                    Text(if (goalMinutes == 0) "Daily goal not set" else todayMinutes.coerceAtMost(goalMinutes).toString() + " / " + goalMinutes + " min daily goal", color = TextMuted, fontSize = 10.sp)
                }

                Spacer(Modifier.height(18.dp))
                SectionHeader("Your focus")
                JeeCard(featured = true) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("YOUR FOCUS", color = PrimaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        TextButton(onClick = { showFocusPicker = true }) { Text("Change", color = PrimaryLight, fontSize = 11.sp) }
                    }
                    Spacer(Modifier.height(3.dp))
                    when {
                        focus != null -> {
                            Text(focus.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp))
                            Text(focus.reason, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = JeeShapes.pill, color = PrimarySoft, border = BorderStroke(1.dp, BgCardBorder.copy(alpha = .8f))) {
                                    Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Schedule, null, tint = PrimaryLight, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(focus.durationMin.toString() + " min", color = PrimaryLight, fontSize = 11.sp)
                                    }
                                }
                                Spacer(Modifier.width(9.dp)); Text(focus.subject, color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        nextTask != null -> {
                            Text(nextTask.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp)); Text("A task is waiting today. Start a focused study block and keep the next action simple.", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                            Spacer(Modifier.height(12.dp)); Text(nextTask.subject + " · " + nextTask.durationMin + " min", color = TextMuted, fontSize = 11.sp)
                        }
                        else -> {
                            Text("Choose a chapter to focus on", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp)); Text("Pick a chapter or keep the automatic data-driven recommendation.", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    Button(onClick = onOpenStudyTimer, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Start focused session")
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionHeader("Today's tasks", "See all") { onTabSelected(AppTab.TASKS) }
                JeeCard {
                    if (todayTasks.isEmpty()) Text("No tasks scheduled for today.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
                    else todayTasks.take(4).forEachIndexed { index, task ->
                        TaskRow(TaskItem(task.id.toString(), task.title, task.subject + " · " + task.durationMin + "m", "", task.done), onToggle = { id -> repo.toggleTask(id.toLong()); refresh++ }, onClick = { onTabSelected(AppTab.TASKS) })
                        if (index < minOf(todayTasks.size, 4) - 1) HorizontalDivider(color = BgDivider.copy(alpha = .45f), thickness = .5.dp)
                    }
                }

                weak?.let {
                    Spacer(Modifier.height(18.dp)); SectionHeader("Needs attention")
                    JeeCard { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(it.chapter.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium); Text(it.signals.take(2).joinToString(" · "), color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp) }
                        Icon(Icons.Filled.ChevronRight, "Review chapter", tint = PrimaryLight)
                    } }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showFocusPicker) {
        val subjects = listOf("Physics", "Chemistry", "Mathematics")
        val chapters = JeeCatalog.forSubject(focusSubject)
        AlertDialog(onDismissRequest = { showFocusPicker = false }, title = { Text("Choose your focus") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Subject", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { subjects.forEach { value -> JeeFilterChip(value, value == focusSubject) { focusSubject = value; selectedFocusId = null } } }
                Text("Chapter", color = TextSecondary, fontSize = 12.sp)
                LazyColumn(Modifier.fillMaxWidth().height(260.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    item { TextButton(onClick = { selectedFocusId = null }) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Automatic recommendation", color = if (selectedFocusId == null) PrimaryLight else TextOnCard); if (selectedFocusId == null) Text("Selected", color = PrimaryLight, fontSize = 10.sp) } } }
                    items(chapters, key = { it.id }) { chapter -> TextButton(onClick = { selectedFocusId = chapter.id }) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("${chapter.number}. ${chapter.name}", color = if (selectedFocusId == chapter.id) PrimaryLight else TextOnCard); if (selectedFocusId == chapter.id) Text("Selected", color = PrimaryLight, fontSize = 10.sp) } } }
                }
            }
        }, confirmButton = { TextButton(onClick = { showFocusPicker = false }) { Text("Done") } })
    }
}

data class FocusDisplay(val title: String, val reason: String, val durationMin: Int, val subject: String)

@Composable
private fun QuickAction(title: String, metric: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit) {
    Column(modifier.height(78.dp).clip(RoundedCornerShape(15.dp)).background(if (primary) BgCardAlt else BgCard).border(1.dp, if (primary) Primary.copy(alpha = .55f) else BgCardBorder.copy(alpha = .8f), RoundedCornerShape(15.dp)).premiumClick(onClick).padding(horizontal = 14.dp, vertical = 11.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) { Icon(icon, null, tint = if (primary) PrimaryLight else TextSecondary, modifier = Modifier.size(21.dp)); Text(metric, color = if (primary) PrimaryLight else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
        Spacer(Modifier.height(5.dp)); Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HomeMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(BgCard.copy(alpha = .6f)).padding(horizontal = 8.dp, vertical = 10.dp)) {
        Text(value, color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(label, color = TextMuted, fontSize = 10.sp, maxLines = 1)
    }
}
