package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class DashboardSnapshot(
    val tasks: List<AppTask>,
    val todayMinutes: Int,
    val goalMinutes: Int,
    val completedChapters: Int,
    val priorities: List<DailyPriority>,
    val weak: WeakChapter?,
    val unresolvedMistakes: Int,
    val questionsAttempted: Int
)

@Composable
fun DashboardScreen(
    repo: JeeRepository,
    onTabSelected: (AppTab) -> Unit,
    onOpenRevision: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAssessment: () -> Unit = {},
    onOpenMistakes: () -> Unit = {},
    onOpenStudyTimer: () -> Unit = {},
    onOpenDailyLog: () -> Unit = {},
    onOpenNotifications: () -> Unit = {}
) {
    var refresh by remember { mutableIntStateOf(0) }
    var showFocusPicker by remember { mutableStateOf(false) }
    var focusSubject by remember { mutableStateOf("Physics") }
    var selectedFocusId by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val learning = remember { LearningRepository(context) }
    val intelligence = remember { JeeIntelligence(repo, learning) }
    val totalChapters = JeeRepository.syllabus.values.sumOf { it.size }
    var snapshot by remember { mutableStateOf<DashboardSnapshot?>(null) }
    LaunchedEffect(refresh) {
        snapshot = withContext(Dispatchers.Default) {
            DashboardSnapshot(
                tasks = repo.getTasks(),
                todayMinutes = repo.getTodayMinutes(),
                goalMinutes = repo.getDailyGoalMinutes(),
                completedChapters = JeeRepository.syllabus.keys.sumOf { subject ->
                    repo.chapters(subject).count { it.progress >= 1f }
                },
                priorities = intelligence.dailyPriorities(3),
                weak = intelligence.weakChapters(1).firstOrNull(),
                unresolvedMistakes = learning.getMistakes().count { !it.resolved },
                questionsAttempted = learning.getQuestionAttempts().size
            )
        }
    }
    val tasks = snapshot?.tasks.orEmpty()
    val todayMinutes = snapshot?.todayMinutes ?: 0
    val goalMinutes = snapshot?.goalMinutes ?: repo.getDailyGoalMinutes()
    val completedChapters = snapshot?.completedChapters ?: 0
    val coverage = if (totalChapters == 0) 0f else completedChapters.toFloat() / totalChapters
    val priorities = snapshot?.priorities.orEmpty()
    val weak = snapshot?.weak
    val unresolvedMistakes = snapshot?.unresolvedMistakes ?: 0
    val questionsAttempted = snapshot?.questionsAttempted ?: 0
    val todayTasks = tasks.filter { it.dueDay == "Today" }
    val dailyLog = remember { DailyLogRepository(context) }
    val currentMonth = YearMonth.now()
    val dailyLogCompleted = remember(refresh, currentMonth) { dailyLog.completedCount(currentMonth) }
    val nextTask = todayTasks.firstOrNull { !it.done } ?: tasks.firstOrNull { !it.done }
    val autoFocus = priorities.firstOrNull()
    val selectedFocusChapter = selectedFocusId?.let { JeeCatalog.find(it) }
    // Chapter progress is read from local storage; do it once per selection, not on every recomposition.
    val focusProgress = remember(selectedFocusId, refresh) {
        selectedFocusChapter?.let { chapter -> repo.chapters(chapter.subject).firstOrNull { it.number == chapter.number } }
    }
    val focus = selectedFocusChapter?.let { chapter ->
        FocusDisplay(
            chapter.name,
            if (focusProgress == null) "Selected chapter · start building a study signal."
            else "${(focusProgress.progress * 100).toInt()}% complete · confidence ${focusProgress.confidence}/5",
            30,
            chapter.subject
        )
    } ?: autoFocus?.let { FocusDisplay(it.title, it.reason, it.durationMin, it.subject) }
    val daysToJee = remember { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.of(2027, 1, 22)).coerceAtLeast(0) }
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val goalProgress = if (goalMinutes == 0) 0f else (todayMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)

    Scaffold(containerColor = BgApp) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Column {
                    Text(greeting, style = MaterialTheme.typography.headlineSmall)
                    Text("JEE Main 2027", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Rounded.Notifications, "Notifications", tint = PrimaryLight)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, "Settings", tint = TextPrimary)
                    }
                }
            }

            // Hero: the one thing to do next.
            JeeCard(modifier = Modifier.animateContentSize(), featured = true) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Up next", style = MaterialTheme.typography.labelLarge, color = PrimaryLight)
                    TextButton(onClick = { showFocusPicker = true }) {
                        Text("Change", style = MaterialTheme.typography.labelLarge, color = PrimaryLight)
                    }
                }
                when {
                    focus != null -> {
                        Text(focus.title, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(focus.reason, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = JeeShapes.pill, color = PrimarySoft) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Schedule, null, tint = PrimaryLight, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("${focus.durationMin} min", style = MaterialTheme.typography.labelMedium, color = PrimaryLight)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(focus.subject, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    nextTask != null -> {
                        Text(nextTask.title, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text("A task is waiting today. Start a focused study block and keep the next action simple.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("${nextTask.subject} · ${nextTask.durationMin} min", style = MaterialTheme.typography.labelMedium)
                    }
                    else -> {
                        Text("Choose a chapter to focus on", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text("Pick a chapter or keep the automatic data-driven recommendation.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onOpenStudyTimer, modifier = Modifier.fillMaxWidth().height(52.dp), shape = JeeShapes.medium) {
                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start focused session", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Today")
            JeeCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(progress = goalProgress, diameter = 108.dp, strokeWidth = 10.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedValue(formatMinutes(todayMinutes)) { Text(it, style = MaterialTheme.typography.titleMedium) }
                            Text(
                                if (goalMinutes == 0) "No goal set" else "of ${formatMinutes(goalMinutes)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatLine("Tasks done", "${todayTasks.count { it.done }}/${todayTasks.size}")
                        StatLine("Open mistakes", unresolvedMistakes.toString())
                        StatLine("Syllabus done", "${(coverage * 100).toInt()}%")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Today's tasks", "See all") { onTabSelected(AppTab.TASKS) }
            JeeCard {
                if (todayTasks.isEmpty()) {
                    Text("No tasks scheduled for today.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
                } else {
                    val shown = todayTasks.take(4)
                    shown.forEachIndexed { index, task ->
                        TaskRow(
                            TaskItem(task.id.toString(), task.title, task.subject + " · " + task.durationMin + "m", "", task.done),
                            onToggle = { id -> repo.toggleTask(id.toLong()); refresh++ },
                            onClick = { onTabSelected(AppTab.TASKS) }
                        )
                        if (index < shown.lastIndex) HorizontalDivider(color = BgDivider.copy(alpha = .5f), thickness = .5.dp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Jump to")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Practice", "$questionsAttempted done", Icons.Rounded.Quiz, Modifier.weight(1f), true, onOpenAssessment)
                QuickAction("Notes", "Library", Icons.Rounded.Description, Modifier.weight(1f), false, onOpenRevision)
                QuickAction("Mistakes", "$unresolvedMistakes open", Icons.Rounded.ErrorOutline, Modifier.weight(1f), false, onOpenMistakes)
            }

            Spacer(Modifier.height(20.dp))
            JeeCard(modifier = Modifier.clip(JeeShapes.medium).premiumClick(onClick = onOpenDailyLog)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = JeeShapes.small, color = PrimarySoft) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = PrimaryLight, modifier = Modifier.padding(10.dp).size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Daily Progress", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} · $dailyLogCompleted/${currentMonth.lengthOfMonth()} days completed",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, "Open Daily Progress", tint = PrimaryLight)
                }
                Spacer(Modifier.height(12.dp))
                LinearStatBar(dailyLogCompleted.toFloat() / currentMonth.lengthOfMonth().coerceAtLeast(1))
            }

            Spacer(Modifier.height(12.dp))
            JeeCard {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("$daysToJee days to JEE Main 2027", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(2.dp))
                        Text("Tentative NTA schedule · Jan 22–24 and Jan 28–30, 2027", style = MaterialTheme.typography.labelSmall)
                    }
                    Icon(Icons.Rounded.Event, "JEE Main 2027 countdown", tint = PrimaryLight)
                }
            }

            weak?.let {
                Spacer(Modifier.height(20.dp))
                SectionHeader("Needs attention")
                JeeCard {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(it.chapter.name, style = MaterialTheme.typography.titleMedium)
                            Text(it.signals.take(2).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Rounded.ChevronRight, "Review chapter", tint = PrimaryLight)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showFocusPicker) {
        val subjects = listOf("Physics", "Chemistry", "Mathematics")
        val chapters = remember(focusSubject) { JeeCatalog.forSubject(focusSubject) }
        AlertDialog(
            onDismissRequest = { showFocusPicker = false },
            title = { Text("Choose your focus") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Subject", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjects.forEach { value -> JeeFilterChip(value, value == focusSubject) { focusSubject = value; selectedFocusId = null } }
                    }
                    Text("Chapter", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(Modifier.fillMaxWidth().height(260.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        item {
                            FocusOption("Automatic recommendation", selectedFocusId == null) { selectedFocusId = null }
                        }
                        items(chapters, key = { it.id }) { chapter ->
                            FocusOption("${chapter.number}. ${chapter.name}", selectedFocusId == chapter.id) { selectedFocusId = chapter.id }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFocusPicker = false }) { Text("Done") } }
        )
    }
}

data class FocusDisplay(val title: String, val reason: String, val durationMin: Int, val subject: String)

private fun formatMinutes(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"

@Composable
private fun FocusOption(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(label, color = if (selected) PrimaryLight else TextPrimary, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Rounded.Check, "Selected", tint = PrimaryLight, modifier = Modifier.size(18.dp))
        }
    }
}

/** Slides a changed value up/out so stat updates read as a change rather than a jump. */
@Composable
private fun AnimatedValue(value: String, content: @Composable (String) -> Unit) {
    AnimatedContent(
        targetState = value,
        transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
        label = "stat"
    ) { content(it) }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        AnimatedValue(value) { Text(it, style = MaterialTheme.typography.titleSmall) }
    }
}

@Composable
private fun QuickAction(
    title: String,
    metric: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier
            .heightIn(min = 84.dp)
            .clip(JeeShapes.medium)
            .background(if (primary) BgCardAlt else BgCard)
            .premiumClick(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(icon, null, tint = if (primary) PrimaryLight else TextSecondary, modifier = Modifier.size(22.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(metric, style = MaterialTheme.typography.labelSmall, color = if (primary) PrimaryLight else TextMuted, maxLines = 1)
        }
    }
}
