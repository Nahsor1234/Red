package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun RevisionScreen(
    repo: JeeRepository,
    onBack: () -> Unit,
    onOpenPlanner: () -> Unit
) {
    var refresh by remember { mutableIntStateOf(0) }
    val context = LocalContext.current.applicationContext
    val learning = remember(context) { LearningRepository(context) }
    val cloud = remember { CloudJeeRepository() }
    var syncingCloudMistakes by remember { mutableStateOf(false) }
    val queue = remember(refresh) { repo.getRevisionQueue() }
    val mistakes = remember(refresh) { learning.getMistakes().filterNot { it.resolved } }
    val dueCount = queue.size

    LaunchedEffect(cloud, learning) {
        syncingCloudMistakes = true
        runCatching {
            val user = cloud.ensureSession()
            val remoteAttempts = cloud.questionAttemptsForUser(user.id)
            val pending = remoteAttempts.filter { attempt ->
                !attempt.correct && attempt.selectedIndex >= 0 &&
                    !learning.isCloudMistakeImported(user.id, attempt.id)
            }
            val questionsById = cloud.questionsByIds(pending.map { it.questionId }.toSet()).associateBy { it.id }
            var imported = 0
            pending.forEach { attempt ->
                val cloudQuestion = questionsById[attempt.questionId] ?: return@forEach
                if (learning.importCloudMistake(
                        userId = user.id,
                        attemptId = attempt.id,
                        question = cloudQuestion.toMistakeQuestion(),
                        selectedIndex = attempt.selectedIndex
                    )
                ) {
                    imported++
                }
            }
            if (imported > 0) refresh++
        }
        syncingCloudMistakes = false
    }

    Scaffold(
        containerColor = BgApp,
        topBar = {
            JeeTopBar(
                title = "Revision engine",
                subtitle = when {
                    mistakes.isNotEmpty() && dueCount > 0 -> "${mistakes.size} mistakes · $dueCount chapter reviews due"
                    mistakes.isNotEmpty() -> "${mistakes.size} question mistakes to review"
                    syncingCloudMistakes -> "Checking saved practice attempts…"
                    dueCount > 0 -> "$dueCount chapter reviews due"
                    else -> "Nothing due right now"
                },
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Question mistakes", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${mistakes.size} questions to revisit · Practice and assessment errors",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            if (mistakes.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgCard)
                            .border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
                            .padding(16.dp)
                    ) {
                        Text("No question mistakes waiting", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "When you submit an incorrect answer in practice or assessment, it will appear here with its saved solution.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(mistakes, key = { "mistake_${it.id}" }) { mistake ->
                    RevisionMistakeCard(mistake) {
                        learning.resolveMistake(mistake.id)
                        refresh++
                    }
                }
            }

            item {
                Text("Chapter spaced revision", style = MaterialTheme.typography.titleLarge)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RevisionMetric("Due", dueCount.toString(), Modifier.weight(1f))
                    RevisionMetric(
                        "Mastery",
                        repo.getMasteredChapterCount().toString(),
                        Modifier.weight(1f)
                    )
                    RevisionMetric(
                        "Active",
                        repo.getActiveRevisionCount().toString(),
                        Modifier.weight(1f)
                    )
                }
            }

            if (queue.isEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgCard).border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
                            .padding(18.dp)
                    ) {
                        Text("No chapter review due", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Chapter-level spaced reviews appear here after a chapter is flagged as weak or its review becomes due.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onOpenPlanner) {
                            Icon(Icons.Filled.Timer, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open planner")
                        }
                    }
                }
            } else {
                items(queue, key = { it.revision.id }) { item ->
                    RevisionCard(
                        item = item,
                        onRate = { rating ->
                            repo.reviewChapter(item.chapter.id, rating)
                            refresh++
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun RevisionMetric(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard).border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
            .padding(12.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun RevisionCard(
    item: RevisionQueueItem,
    onRate: (RevisionRating) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard).border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
            .padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(item.chapter.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    item.chapter.subject + " · " + (item.progress * 100).toInt() + "% mastered",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            Text(
                "C" + item.chapter.number,
                color = AccentBlueLight,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("How did this feel?", color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ReviewButton("Again", RevisionRating.AGAIN, onRate, Modifier.weight(1f))
            ReviewButton("Hard", RevisionRating.HARD, onRate, Modifier.weight(1f))
            ReviewButton("Good", RevisionRating.GOOD, onRate, Modifier.weight(1f))
            ReviewButton("Easy", RevisionRating.EASY, onRate, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Confidence: " + item.confidence + "/5",
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ReviewButton(
    label: String,
    rating: RevisionRating,
    onRate: (RevisionRating) -> Unit,
    modifier: Modifier
) {
    OutlinedButton(
        onClick = { onRate(rating) },
        modifier = modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, fontSize = 10.sp)
    }
}

@Composable
private fun RevisionMistakeCard(mistake: MistakeRecord, onResolve: () -> Unit) {
    var expanded by remember(mistake.id) { mutableStateOf(false) }
    val chapterName = JeeCatalog.find(mistake.chapterId)?.name ?: mistake.chapterId
    val category = when (mistake.mistakeType) {
        MistakeType.CONCEPT -> "Conceptual mistake"
        MistakeType.FORMULA -> "Formula mistake"
        MistakeType.CALCULATION -> "Calculation error"
        MistakeType.SILLY -> "Silly mistake"
        MistakeType.MISREAD -> "Misread question"
        MistakeType.TIME_PRESSURE -> "Time pressure"
        MistakeType.GUESS -> "Guessing"
        MistakeType.METHOD -> "Wrong method"
        MistakeType.UNCLASSIFIED -> "Not tagged yet"
    }

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("$chapterName · $category", color = AccentBlueLight, fontSize = 11.sp)
                Text(mistake.questionPrompt, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            }
            if (mistake.count > 1) {
                Text("×${mistake.count}", color = AccentAmber, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text(if (expanded) "Hide solution" else "Review solution", fontSize = 12.sp) }
            Button(
                onClick = onResolve,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text("Mark reviewed", fontSize = 12.sp) }
        }
        if (expanded) {
            HorizontalDivider(color = BgDivider)
            Text("Your answer: ${mistake.lastSelectedAnswer}", color = TextSecondary, fontSize = 12.sp)
            Text("Correct answer: ${mistake.correctAnswer}", color = PrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            if (mistake.correction.isNotBlank()) {
                Text(mistake.correction, color = TextPrimary.copy(alpha = .94f), fontSize = 13.sp, lineHeight = 19.sp)
            }
            Text("Mark reviewed only after you understand why the answer was wrong.", color = TextMuted, fontSize = 11.sp)
        }
    }
}

private fun CloudQuestion.toMistakeQuestion() = Question(
    id = id,
    subject = when (subjectId.lowercase()) {
        "physics" -> "Physics"
        "chemistry" -> "Chemistry"
        "mathematics" -> "Mathematics"
        else -> subjectId.replace('_', ' ').replaceFirstChar { it.uppercase() }
    },
    chapterId = JeeCatalog.normalizeChapterId(chapterId),
    chapter = chapterName,
    topic = topic,
    prompt = prompt,
    options = options,
    correctIndex = correctIndex,
    explanation = explanation,
    difficulty = difficulty,
    source = source,
    year = year,
    marks = marks,
    negativeMarks = negativeMarks
)
