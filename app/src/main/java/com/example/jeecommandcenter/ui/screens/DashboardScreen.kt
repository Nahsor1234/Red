package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private data class DashboardSnapshot(
    val tasks: List<AppTask>,
    val todayMinutes: Int,
    val goalMinutes: Int,
    val priorities: List<DailyPriority>,
    val weak: WeakChapter?
)

@Composable
fun DashboardScreen(
    repo: JeeRepository,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onAiClick: () -> Unit = {},
    onOpenRevision: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAssessment: () -> Unit = {},
    onOpenMistakes: () -> Unit = {},
    onOpenStudyTimer: () -> Unit = {},
    onOpenDailyLog: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenChapter: (JeeChapter) -> Unit = {}
) {
    var refresh by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFocusPicker by remember { mutableStateOf(false) }
    var focusSubject by remember { mutableStateOf("Physics") }
    var selectedFocusId by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val learning = remember { LearningRepository(context) }
    val intelligence = remember { JeeIntelligence(repo, learning) }
    var snapshot by remember { mutableStateOf<DashboardSnapshot?>(null) }

    LaunchedEffect(refresh) {
        snapshot = withContext(Dispatchers.Default) {
            DashboardSnapshot(
                tasks = repo.getTasks(),
                todayMinutes = repo.getTodayMinutes(),
                goalMinutes = repo.getDailyGoalMinutes(),
                priorities = intelligence.dailyPriorities(3),
                weak = intelligence.weakChapters(1).firstOrNull()
            )
        }
    }

    val tasks = snapshot?.tasks.orEmpty()
    val todayMinutes = snapshot?.todayMinutes ?: 0
    val goalMinutes = snapshot?.goalMinutes ?: repo.getDailyGoalMinutes()
    val priorities = snapshot?.priorities.orEmpty()
    val weak = snapshot?.weak
    val todayTasks = tasks.filter { it.dueDay == "Today" }
    val dailyLog = remember { DailyLogRepository(context) }
    val currentMonth = YearMonth.now()
    val dailyLogCompleted = remember(refresh, currentMonth) { dailyLog.completedCount(currentMonth) }
    val nextTask = todayTasks.firstOrNull { !it.done } ?: tasks.firstOrNull { !it.done }
    val autoFocus = priorities.firstOrNull()
    val selectedFocusChapter = selectedFocusId?.let { JeeCatalog.find(it) }
    val focus = selectedFocusChapter?.let { chapter ->
        val progress = repo.chapters(focusSubject).firstOrNull { it.number == chapter.number }
        FocusDisplay(
            chapter.name,
            if (progress == null) "Selected chapter · start building a study signal."
            else "${(progress.progress * 100).toInt()}% complete · confidence ${progress.confidence}/5",
            30,
            chapter.subject
        )
    } ?: autoFocus?.let { FocusDisplay(it.title, it.reason, it.durationMin, it.subject) }

    val focusEyebrow = when {
        selectedFocusChapter != null -> "YOUR SELECTED FOCUS"
        autoFocus != null -> "RECOMMENDED NEXT ACTION"
        nextTask != null -> "TASK TO PICK UP"
        else -> "SET YOUR NEXT FOCUS"
    }
    val daysToJee = remember {
        ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.of(2027, 1, 22)).coerceAtLeast(0)
    }
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BgAppBase,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AnimatedBottomNavBar(
                    selectedTab,
                    onTabSelected,
                    visible = selectedTab != AppTab.AI
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = JeeSpacing.lg)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(JeeSpacing.sm))

                // Header: utility actions remain available without competing with the study action.
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 112.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            greeting,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "JEE MAIN 2027 · $daysToJee days to Session 1",
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenNotifications) {
                            Icon(Icons.Filled.Notifications, "Notifications", tint = TextSecondary)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, "Settings", tint = TextPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(JeeSpacing.md))

                // One dominant dashboard surface: the next useful study action.
                JeeCard(featured = true, modifier = Modifier.animateContentSize(animationSpec = tween(JeeMotion.layoutMillis))) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            focusEyebrow,
                            color = Primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        TextButton(
                            onClick = { showFocusPicker = true },
                            contentPadding = PaddingValues(horizontal = JeeSpacing.sm, vertical = 0.dp)
                        ) {
                            Text("Change", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                            Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(Modifier.height(JeeSpacing.xs))

                    when {
                        snapshot == null -> {
                            Text(
                                "Preparing your study overview…",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(JeeSpacing.sm))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = TextSecondary,
                                trackColor = BgDivider
                            )
                        }
                        focus != null -> {
                            AnimatedContent(targetState = focus.title, label = "home-focus-title") { title ->
                                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(JeeSpacing.xs))
                            AnimatedContent(targetState = focus.reason, label = "home-focus-reason") { reason ->
                                Text(reason, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
                            }
                            Spacer(Modifier.height(JeeSpacing.md))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = JeeShapes.pill,
                                    color = BgCardAlt,
                                    border = BorderStroke(1.dp, BgCardBorder.copy(alpha = .8f))
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = JeeSpacing.sm, vertical = JeeSpacing.xs),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Schedule, null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(JeeSpacing.xs))
                                        Text(
                                            "${focus.durationMin} min",
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
                                        )
                                    }
                                }
                                Spacer(Modifier.width(JeeSpacing.sm))
                                Text(
                                    focus.subject,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        nextTask != null -> {
                            Text(
                                nextTask.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(JeeSpacing.xs))
                            Text(
                                "An unfinished task is ready to pick up. Start a focused block and work through it.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 21.sp
                            )
                            Spacer(Modifier.height(JeeSpacing.sm))
                            Text(
                                "${nextTask.subject} · ${nextTask.durationMin} min",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        else -> {
                            Text(
                                "Choose a chapter to focus on",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(JeeSpacing.xs))
                            Text(
                                "Select a chapter or use the automatic recommendation when enough study evidence is available.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 21.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(JeeSpacing.lg))
                    val buttonInteraction = remember { MutableInteractionSource() }
                    val buttonPressed by buttonInteraction.collectIsPressedAsState()
                    val buttonScale by animateFloatAsState(
                        targetValue = if (buttonPressed) 0.98f else 1f,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                        label = "home-focus-button-scale"
                    )
                    Button(
                        onClick = onOpenStudyTimer,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                            .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale),
                        shape = JeeShapes.medium,
                        enabled = snapshot != null,
                        interactionSource = buttonInteraction,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(JeeSpacing.sm))
                            Text(if (focus == null && nextTask == null) "Open study timer" else "Start focused session")
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(JeeSpacing.md))

                Spacer(Modifier.height(JeeSpacing.md))
                HomeSectionHeader("Today")

                if (snapshot == null) {
                    Text(
                        "Loading today's study data…",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(JeeSpacing.lg)) {
                        HomeMetric(
                            "${todayMinutes / 60}h ${todayMinutes % 60}m",
                            "Study time",
                            Modifier.weight(1f),
                            emphasized = true
                        )
                        HomeMetric(
                            "$goalMinutes min",
                            "Daily goal",
                            Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(JeeSpacing.sm))
                    LinearStatBar(
                        if (goalMinutes == 0) 0f else (todayMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f),
                        height = 6.dp,
                        fillColor = TextSecondary
                    )
                    Spacer(Modifier.height(JeeSpacing.xs))
                    Text(
                        if (goalMinutes == 0) "Daily goal not set"
                        else "${todayMinutes.coerceAtMost(goalMinutes)} / $goalMinutes min daily goal",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
                    )
                }

                Spacer(Modifier.height(JeeSpacing.md))

                // Daily log remains directly reachable, but no longer occupies a full card.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(JeeShapes.medium)
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = "Open daily progress, ${currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, $dailyLogCompleted of ${currentMonth.lengthOfMonth()} days completed"
                        }
                        .premiumClick(onClick = onOpenDailyLog)
                        .padding(vertical = JeeSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(JeeSpacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text("Daily consistency", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} · $dailyLogCompleted/${currentMonth.lengthOfMonth()} days completed",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
                        )
                        Spacer(Modifier.height(6.dp))
                        LinearStatBar(
                            dailyLogCompleted.toFloat() / currentMonth.lengthOfMonth().coerceAtLeast(1),
                            height = 4.dp,
                            fillColor = TextSecondary
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
                }

                Spacer(Modifier.height(JeeSpacing.md))
                HomeSectionHeader("Today's tasks", "See all") { onTabSelected(AppTab.TASKS) }
                if (snapshot == null) {
                    Text("Loading tasks…", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                } else if (todayTasks.isEmpty()) {
                    Text(
                        "No tasks scheduled for today.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = JeeSpacing.sm)
                    )
                } else {
                    todayTasks.take(4).forEachIndexed { index, task ->
                        TaskRow(
                            TaskItem(
                                task.id.toString(),
                                task.title,
                                task.subject + " · " + task.durationMin + "m",
                                "",
                                task.done
                            ),
                            onToggle = { id ->
                                val taskId = id.toLong()
                                val wasDone = todayTasks.firstOrNull { it.id == taskId }?.done == true
                                repo.toggleTask(taskId)
                                refresh++
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = if (wasDone) "Task reopened" else "Task completed",
                                        actionLabel = "Undo",
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        repo.toggleTask(taskId)
                                        refresh++
                                    }
                                }
                            },
                            onClick = { onTabSelected(AppTab.TASKS) },
                            expressiveMotion = true
                        )
                        if (index < minOf(todayTasks.size, 4) - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                color = BgDivider.copy(alpha = .45f),
                                thickness = .5.dp
                            )
                        }
                    }
                }

                weak?.let { item ->
                    Spacer(Modifier.height(JeeSpacing.md))
                    SectionHeader("Needs attention")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(JeeShapes.medium)
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "Review ${item.chapter.name}. ${item.signals.take(2).joinToString(", ")}"
                            }
                            .premiumClick { onOpenChapter(item.chapter) }
                            .padding(vertical = JeeSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.chapter.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                item.signals.take(2).joinToString(" · "),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(Modifier.width(JeeSpacing.sm))
                        Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(96.dp))
            }
        }
    }

    if (showFocusPicker) {
        val subjects = listOf("Physics", "Chemistry", "Mathematics")
        val chapters = JeeCatalog.forSubject(focusSubject)
        AlertDialog(
            onDismissRequest = { showFocusPicker = false },
            title = { Text("Choose your focus") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(JeeSpacing.sm)) {
                    Text("Subject", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(JeeSpacing.xs)) {
                        subjects.forEach { value ->
                            JeeFilterChip(value, value == focusSubject) {
                                focusSubject = value
                                selectedFocusId = null
                            }
                        }
                    }
                    Text("Chapter", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    LazyColumn(
                        Modifier.fillMaxWidth().height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item {
                            TextButton(onClick = { selectedFocusId = null }) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Automatic recommendation",
                                        color = if (selectedFocusId == null) PrimaryLight else TextOnCard
                                    )
                                    if (selectedFocusId == null) {
                                        Text("Selected", color = PrimaryLight, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        items(chapters, key = { it.id }) { chapter ->
                            TextButton(onClick = { selectedFocusId = chapter.id }) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${chapter.number}. ${chapter.name}",
                                        color = if (selectedFocusId == chapter.id) PrimaryLight else TextOnCard
                                    )
                                    if (selectedFocusId == chapter.id) {
                                        Text("Selected", color = PrimaryLight, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFocusPicker = false }) { Text("Done") }
            }
        )
    }
}

data class FocusDisplay(val title: String, val reason: String, val durationMin: Int, val subject: String)

@Composable
@Composable
private fun HomeSectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = JeeSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (actionLabel != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary),
                modifier = Modifier.semantics { role = Role.Button }.premiumClick { onActionClick() }
            )
        }
    }
}

private fun HomeMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    Column(modifier.padding(vertical = JeeSpacing.xs)) {
        AnimatedContent(targetState = value, label = "home-metric-value") { animatedValue ->
            Text(
                animatedValue,
                color = if (emphasized) TextPrimary else TextSecondary,
                style = (if (emphasized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium)
                    .copy(fontFeatureSettings = "tnum"),
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
        Text(
            label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}