package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import com.example.jeecommandcenter.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

private enum class AssessmentState { SETUP, RUNNING, RESULT }

@Composable
fun AssessmentScreen(learning: LearningRepository, jee: JeeRepository, onBack: () -> Unit, onOpenMistakes: () -> Unit, onOpenHistory: () -> Unit = {}) {
    val context = LocalContext.current
    val settings = remember { AiSettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(AssessmentState.SETUP) }
    var mode by remember { mutableStateOf(TestMode.PRACTICE) }
    var subject by remember { mutableStateOf("Physics") }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var answered by remember { mutableStateOf(false) }
    var correct by remember { mutableIntStateOf(0) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var questionStartedAt by remember { mutableLongStateOf(0L) }
    var testId by remember { mutableStateOf("") }
    var loadingQuestions by remember { mutableStateOf(false) }
    var loadMessage by remember { mutableStateOf<String?>(null) }
    var showAiDialog by remember { mutableStateOf(false) }
    var aiBusy by remember { mutableStateOf(false) }
    var aiDifficulty by remember { mutableStateOf("Mixed") }
    var aiCount by remember { mutableIntStateOf(10) }

    fun startWith(list: List<Question>) {
        if (list.isEmpty()) return
        testId = UUID.randomUUID().toString(); questions = list; index = 0; selected = -1; answered = false; correct = 0; startedAt = System.currentTimeMillis(); questionStartedAt = startedAt; state = AssessmentState.RUNNING
    }
    fun start() {
        if (loadingQuestions) return
        scope.launch {
            loadingQuestions = true
            loadMessage = null
            val requestedCount = if (mode == TestMode.MOCK) 10 else 5
            val cloudQuestions = runCatching {
                val cloud = CloudJeeRepository()
                if (mode == TestMode.MOCK) cloud.allQuestions()
                else cloud.questionsForSubject(subject.lowercase())
            }.getOrDefault(emptyList())

            val localQuestions = if (mode == TestMode.PRACTICE) {
                learning.getQuestions().filter { it.subject.equals(subject, ignoreCase = true) }
            } else {
                learning.getQuestions()
            }
            val pool = if (cloudQuestions.isNotEmpty()) {
                cloudQuestions.map { it.toAssessmentQuestion() }
            } else {
                loadMessage = "Using the local question bank because cloud questions were unavailable."
                localQuestions
            }
            val filteredPool = if (mode == TestMode.PRACTICE) {
                pool.filter { it.subject.equals(subject, ignoreCase = true) }
            } else pool
            val selectedQuestions = filteredPool.shuffled().take(requestedCount)
            loadingQuestions = false
            if (selectedQuestions.isEmpty()) {
                loadMessage = "No questions are available for this assessment. Check the question bank and try again."
            } else {
                startWith(selectedQuestions)
            }
        }
    }

    fun finish() {
        if (state != AssessmentState.RUNNING || questions.isEmpty()) return
        val duration = ((System.currentTimeMillis() - startedAt) / 1000L).toInt()
        val attemptsByQuestion = learning.getQuestionAttempts()
            .filter { it.testId == testId }
            .associateBy { it.questionId }

        val correctCount = questions.count { question ->
            attemptsByQuestion[question.id]?.correct == true
        }
        val attemptedCount = questions.count { question ->
            (attemptsByQuestion[question.id]?.selectedIndex ?: -1) >= 0
        }
        val incorrectCount = questions.count { question ->
            val attempt = attemptsByQuestion[question.id]
            attempt != null && attempt.selectedIndex >= 0 && !attempt.correct
        }
        val skippedCount = (questions.size - attemptedCount).coerceAtLeast(0)
        val scoreValue = questions.sumOf { question ->
            when (val attempt = attemptsByQuestion[question.id]) {
                null -> 0.0
                else -> when {
                    attempt.selectedIndex < 0 -> 0.0
                    attempt.correct -> question.marks.toDouble()
                    else -> -question.negativeMarks.toDouble()
                }
            }
        }
        val totalMarks = questions.sumOf { it.marks }
        val breakdown = questions.groupBy { it.subject }.mapValues { (_, subjectQuestions) ->
            val subjectCorrect = subjectQuestions.count { attemptsByQuestion[it.id]?.correct == true }
            val subjectAttempted = subjectQuestions.count { (attemptsByQuestion[it.id]?.selectedIndex ?: -1) >= 0 }
            subjectCorrect to subjectAttempted
        }
        learning.saveTestAttempt(
            TestAttemptRecord(
                id = testId,
                mode = mode,
                startedAt = startedAt,
                completedAt = System.currentTimeMillis(),
                questionCount = questions.size,
                correctCount = correctCount,
                score = scoreValue.roundToInt(),
                totalMarks = totalMarks,
                durationSec = duration,
                subjectBreakdown = breakdown,
                attemptedCount = attemptedCount,
                incorrectCount = incorrectCount,
                skippedCount = skippedCount
            )
        )
        correct = correctCount
        state = AssessmentState.RESULT
    }

    Scaffold(containerColor = BgApp, topBar = {
        JeeTopBar(
            title = when (state) { AssessmentState.SETUP -> "Assessment"; AssessmentState.RUNNING -> if (mode == TestMode.MOCK) "Starter mock" else "Practice test"; AssessmentState.RESULT -> "Result" },
            onBack = onBack,
            trailing = { if (state == AssessmentState.SETUP) TextButton(onClick = onOpenHistory) { Icon(Icons.Filled.History, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("History", fontSize = 12.sp) } }
        )
    }) { padding ->
        when (state) {
            AssessmentState.SETUP -> SetupContent(
                mode, subject, { mode = it }, { subject = it }, ::start, onOpenMistakes,
                Modifier.padding(padding), settings.hasApiKey(), { showAiDialog = true },
                loadingQuestions, loadMessage
            )
            AssessmentState.RUNNING -> {
                val question = questions.getOrNull(index)
                if (question != null) {
                    var remaining by remember(index, testId) { mutableIntStateOf(if (mode == TestMode.MOCK) 90 else 60) }
                    LaunchedEffect(index, testId) { while (!answered && remaining > 0) { delay(1000); remaining-- } }
                    LaunchedEffect(remaining, answered) { if (!answered && remaining == 0) { selected = -1; learning.saveQuestionAttempt(testId, question, -1, 60); jee.flagChapterWeak(question.chapterId); answered = true } }
                    QuestionContent(Modifier.padding(padding), question, index, questions.size, selected, answered, remaining, { if (!answered) selected = it }, { if (!answered && selected >= 0) { val record = learning.saveQuestionAttempt(testId, question, selected, ((System.currentTimeMillis() - questionStartedAt) / 1000).toInt()); if (record.correct) correct++ else jee.flagChapterWeak(question.chapterId); answered = true } }, { if (index + 1 >= questions.size) finish() else { index++; selected = -1; answered = false; questionStartedAt = System.currentTimeMillis() } })
                }
            }
            AssessmentState.RESULT -> ResultContent(Modifier.padding(padding), correct, questions.size, { state = AssessmentState.SETUP }, onOpenMistakes)
        }
    }

    if (showAiDialog) {
        AlertDialog(onDismissRequest = { if (!aiBusy) showAiDialog = false }, title = { Text("Create quiz with AI") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Difficulty", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Easy", "Medium", "Hard", "Mixed").forEach { JeeFilterChip(it, aiDifficulty == it) { aiDifficulty = it } } }
                Text("Questions", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(5, 10, 15, 20).forEach { JeeFilterChip(it.toString(), aiCount == it) { aiCount = it } } }
                Text("Context: $subject${if (mode == TestMode.PRACTICE) "" else " · mixed subjects"}", color = TextMuted, fontSize = 11.sp)
                if (aiBusy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AccentBlue)
            }
        }, confirmButton = { Button(enabled = !aiBusy, onClick = { aiBusy = true; scope.launch { val generated = AiEngine(settings).generateQuiz(subject, null, aiDifficulty, aiCount); aiBusy = false; if (generated.isNotEmpty()) { showAiDialog = false; startWith(generated) } } }) { Text(if (aiBusy) "Creating..." else "Create quiz") } }, dismissButton = { TextButton(enabled = !aiBusy, onClick = { showAiDialog = false }) { Text("Cancel") } })
    }
}

@Composable
private fun SetupContent(
    mode: TestMode,
    subject: String,
    onMode: (TestMode) -> Unit,
    onSubject: (String) -> Unit,
    onStart: () -> Unit,
    onOpenMistakes: () -> Unit,
    modifier: Modifier,
    aiAvailable: Boolean,
    onCreateAi: () -> Unit,
    loadingQuestions: Boolean,
    loadMessage: String?
) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCardAlt).border(JeeSurfaceTokens.borderWidth,BgCardBorder.copy(alpha=JeeSurfaceTokens.cardBorderAlpha),JeeShapes.medium).padding(16.dp)) { Text("Assessment engine", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(6.dp)); Text("Practice tests and AI-generated quizzes create real question attempts, mistakes and analytics.", color = TextMuted, fontSize = 11.sp) } }
        item { Text("Mode", color = TextSecondary, fontSize = 12.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { JeeFilterChip("Practice", mode == TestMode.PRACTICE) { onMode(TestMode.PRACTICE) }; JeeFilterChip("Starter mock", mode == TestMode.MOCK) { onMode(TestMode.MOCK) } } }
        if (mode == TestMode.PRACTICE) item { Text("Subject", color = TextSecondary, fontSize = 12.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Physics", "Chemistry", "Mathematics").forEach { JeeFilterChip(it, subject == it) { onSubject(it) } } } }
        item {
            Button(
                onClick = onStart,
                enabled = !loadingQuestions,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                if (loadingQuestions) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Loading questions…")
                } else {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (mode == TestMode.MOCK) "Start 10-question mock" else "Start 5-question practice")
                }
            }
            if (!loadingQuestions && !loadMessage.isNullOrBlank()) {
                Text(loadMessage, color = TextSecondary, fontSize = 11.sp)
            }
        }
        item { OutlinedButton(onClick = onCreateAi, enabled = aiAvailable, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(7.dp)); Text(if (aiAvailable) "Create quiz with AI" else "Configure AI to create quiz") } }
        item { OutlinedButton(onClick = onOpenMistakes, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.ErrorOutline, null); Spacer(Modifier.width(7.dp)); Text("Open mistake bank") } }
        item { Text("AI quiz generation uses the configured provider and returns structured questions for the existing assessment flow.", color = TextMuted, fontSize = 10.sp) }
    }
}

@Composable
private fun QuestionContent(modifier: Modifier, question: Question, questionIndex: Int, total: Int, selected: Int, answered: Boolean, remaining: Int, onSelect: (Int) -> Unit, onSubmit: () -> Unit, onNext: () -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Question ${questionIndex + 1} / $total", color = TextSecondary, fontSize = 12.sp); Text("${remaining}s", color = if (remaining <= 10) AccentAmber else TextSecondary) } }
        item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).border(JeeSurfaceTokens.borderWidth,BgCardBorder.copy(alpha=JeeSurfaceTokens.cardBorderAlpha),JeeShapes.medium).padding(16.dp)) { Text(question.chapter, color = AccentBlueLight, fontSize = 10.sp); Spacer(Modifier.height(7.dp)); Text(question.prompt, style = MaterialTheme.typography.titleMedium) } }
        items(question.options.indices.toList()) { optionIndex -> val chosen = selected == optionIndex; Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (chosen) AccentBlueSoft else BgCard).selectable(selected = chosen, enabled = !answered, role = Role.RadioButton, onClick = { onSelect(optionIndex) }).padding(14.dp)) { RadioButton(chosen, onClick = null, enabled = !answered); Spacer(Modifier.width(7.dp)); Text(question.options[optionIndex], modifier = Modifier.padding(top = 12.dp), color = TextOnCard) } }
        item { if (answered) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgCardAlt).border(JeeSurfaceTokens.borderWidth,BgCardBorder.copy(alpha=JeeSurfaceTokens.cardBorderAlpha),JeeShapes.medium).padding(14.dp)) { Text(if (selected == question.correctIndex) "Correct" else "Review", color = if (selected == question.correctIndex) AccentGreen else AccentAmber, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(5.dp)); Text(question.explanation, color = TextSecondary, fontSize = 12.sp) }; Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(if (questionIndex + 1 == total) "Finish test" else "Next") } } else Button(onClick = onSubmit, enabled = selected >= 0, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Text("Submit answer") } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ResultContent(modifier: Modifier, correct: Int, total: Int, onRestart: () -> Unit, onMistakes: () -> Unit) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCardAlt).border(JeeSurfaceTokens.borderWidth,BgCardBorder.copy(alpha=JeeSurfaceTokens.cardBorderAlpha),JeeShapes.medium).padding(18.dp)) { Text("Assessment complete", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp)); Text("$correct / $total correct", style = MaterialTheme.typography.headlineMedium); Text(if (total == 0) "No questions recorded" else "${(correct * 100) / total}% accuracy", color = TextSecondary, fontSize = 12.sp) }; Button(onClick = onMistakes, modifier = Modifier.fillMaxWidth()) { Text("Review mistakes") }; OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Take another test") } }
}

private fun CloudQuestion.toAssessmentQuestion() = Question(
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
