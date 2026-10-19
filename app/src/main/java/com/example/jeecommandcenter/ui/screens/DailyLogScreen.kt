package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.example.jeecommandcenter.data.CloudJeeRepository
import com.example.jeecommandcenter.data.CloudPreferenceSync
import com.example.jeecommandcenter.data.DailyLogEntry
import com.example.jeecommandcenter.data.DailyLogRepository
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.components.LinearStatBar
import com.example.jeecommandcenter.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun DailyLogScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val repository = remember { DailyLogRepository(context) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var selectedDate by remember { mutableStateOf(today) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            val current = LocalDate.now()
            if (current != today) {
                val previousToday = today
                today = current
                if (selectedDate == previousToday) selectedDate = current
                if (visibleMonth == YearMonth.from(previousToday)) visibleMonth = YearMonth.from(current)
            }
        }
    }

    val entry = remember(selectedDate, refresh) { repository.getEntry(selectedDate) }
    val completedCount = remember(visibleMonth, refresh) { repository.completedCount(visibleMonth) }
    val monthDays = visibleMonth.lengthOfMonth()
    val daysInScope = when {
        visibleMonth.isBefore(YearMonth.from(today)) -> monthDays
        visibleMonth.isAfter(YearMonth.from(today)) -> 1
        else -> today.dayOfMonth.coerceAtLeast(1)
    }
    val consistencyPercent = (completedCount.coerceAtMost(daysInScope) * 100f / daysInScope).toInt().coerceIn(0, 100)
    val dayStreak = remember(today, refresh) {
        var cursor = if (repository.getEntry(today).completed) today else today.minusDays(1)
        var streak = 0
        while (repository.getEntry(cursor).completed) {
            streak++
            cursor = cursor.minusDays(1)
        }
        streak
    }
    val calendarCells = remember(visibleMonth) {
        buildList<LocalDate?> {
            repeat(visibleMonth.atDay(1).dayOfWeek.value - 1) { add(null) }
            (1..visibleMonth.lengthOfMonth()).forEach { add(visibleMonth.atDay(it)) }
            while (size % 7 != 0) add(null)
        }
    }
    val selectedIsToday = selectedDate == today
    var cloudBackupStatus by remember { mutableStateOf<String?>(null) }

    // Debounce typing, then sync the complete SharedPreferences state. This includes
    // every dated daily-log key, not just the currently visible day.
    LaunchedEffect(selectedDate, entry.notes, entry.completed, selectedIsToday) {
        if (!selectedIsToday) {
            cloudBackupStatus = null
            return@LaunchedEffect
        }

        cloudBackupStatus = "Saved on this device · checking cloud backup"
        delay(1_200)
        val user = runCatching { CloudJeeRepository().currentUser() }.getOrNull()
        if (user == null) {
            cloudBackupStatus = "Saved on this device · sign in for cloud backup"
            return@LaunchedEffect
        }

        cloudBackupStatus = "Backing up to your Sigma JE account…"
        runCatching { CloudPreferenceSync(context).sync(user.id) }
            .onSuccess {
                refresh++
                cloudBackupStatus = "Backed up to your Sigma JE account"
            }
            .onFailure {
                cloudBackupStatus = "Saved on device · cloud backup pending; retry from Cloud Account"
            }
    }
    Scaffold(
        containerColor = BgApp,
        topBar = { JeeTopBar(title = "Daily Progress", onBack = onBack) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JeeCard {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "YOUR MONTH AT A GLANCE",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                    Text(
                        "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 96.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier.width(104.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                drawArc(
                                    color = BgDivider,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = stroke
                                )
                                if (consistencyPercent > 0) {
                                    drawArc(
                                        color = PrimaryLight,
                                        startAngle = -90f,
                                        sweepAngle = 360f * consistencyPercent / 100f,
                                        useCenter = false,
                                        style = stroke
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                Text(
                                    "$consistencyPercent%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    "CONSISTENCY",
                                    color = TextMuted,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = .25.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(7.dp))
                    Row(
                        Modifier.weight(1f).height(72.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("$completedCount", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(3.dp))
                            Text("Days studied", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Box(Modifier.width(1.dp).height(42.dp).background(BgDivider))
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("$dayStreak", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(3.dp))
                            Text("Day streak", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            JeeCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1); selectedDate = visibleMonth.atDay(1) }) {
                        Icon(Icons.Filled.ChevronLeft, "Previous month", tint = TextPrimary)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(visibleMonth.year.toString(), color = TextMuted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1); selectedDate = visibleMonth.atDay(1) }) {
                        Icon(Icons.Filled.ChevronRight, "Next month", tint = TextPrimary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, day ->
                        Text(day, Modifier.weight(1f), color = TextMuted, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(4.dp))
                calendarCells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        week.forEach { date ->
                            if (date == null) {
                                Spacer(Modifier.weight(1f).aspectRatio(0.92f))
                            } else {
                                val dateEntry = remember(date, refresh) { repository.getEntry(date) }
                                val isSelected = date == selectedDate
                                val isToday = date == today
                                val cellColor = when {
                                    isSelected -> PrimarySoft
                                    dateEntry.completed -> AccentGreen.copy(alpha = 0.14f)
                                    else -> BgCard.copy(alpha = 0.68f)
                                }
                                val cellBorder = when {
                                    isSelected -> PrimaryLight
                                    isToday -> Primary.copy(alpha = 0.75f)
                                    else -> BgCardBorder.copy(alpha = 0.55f)
                                }
                                Column(
                                    Modifier.weight(1f)
                                        .aspectRatio(0.92f)
                                        .padding(1.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(cellColor)
                                        .border(1.dp, cellBorder, RoundedCornerShape(11.dp))
                                        .clickable { selectedDate = date }
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(date.dayOfMonth.toString(), color = if (isSelected) PrimaryLight else TextPrimary, fontSize = 13.sp, fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium)
                                    Spacer(Modifier.height(2.dp))
                                    if (dateEntry.completed) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Completed",
                                            tint = PrimaryLight,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    } else {
                                        Spacer(Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text("Check = completed · outlined date = today", color = TextMuted, fontSize = 10.sp)
            }

            JeeCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (!selectedIsToday) {
                        Surface(shape = RoundedCornerShape(8.dp), color = BgCardAlt) {
                            Text(if (selectedDate.isAfter(today)) "Upcoming" else if (entry.completed) "Complete" else if (entry.notes.isNotBlank()) "Notes saved" else "No record", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                        }
                    }
                }
                if (selectedIsToday) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = entry.completed,
                            onCheckedChange = { repository.updateTodayCompletion(it); refresh++ },
                            colors = CheckboxDefaults.colors(checkedColor = Primary, checkmarkColor = BgApp, uncheckedColor = TextMuted)
                        )
                        Column(Modifier.weight(1f)) {
                            Text("I've completed my study today", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Tick only when you're done for today.", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                } else if (selectedDate.isAfter(today)) {
                    Spacer(Modifier.height(7.dp))
                    Text("This date hasn't happened yet. You can view it, but can't prefill or edit it.", color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Spacer(Modifier.height(7.dp))
                Text("What did you study?", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(5.dp))
                OutlinedTextField(
                    value = entry.notes,
                    onValueChange = { if (selectedIsToday) { repository.updateTodayNotes(it); refresh++ } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp, max = 220.dp),
                    minLines = 4,
                    maxLines = 8,
                    placeholder = { Text(if (selectedIsToday) "Subjects, chapters, questions, revision..." else "No study notes saved for this date.", color = TextMuted, fontSize = 12.sp) },
                    readOnly = !selectedIsToday,
                    enabled = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = BgCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = BgCard.copy(alpha = .7f),
                        unfocusedContainerColor = BgCard.copy(alpha = .7f),
                        cursorColor = PrimaryLight
                    )
                )
                if (selectedIsToday) {
                    Spacer(Modifier.height(5.dp))
                    Text("${entry.notes.length}/${DailyLogRepository.MAX_NOTE_LENGTH} characters · ${cloudBackupStatus ?: "Saved automatically on this device"}", color = if (cloudBackupStatus?.startsWith("Backed up") == true) AccentGreen else TextMuted, fontSize = 9.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
