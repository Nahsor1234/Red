package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private enum class AssessmentState { SETUP, RUNNING, RESULT }

@Composable
fun AssessmentScreen(learning: LearningRepository, jee: JeeRepository, onBack: () -> Unit, onOpenMistakes: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
    var showAiDialog by remember { mutableStateOf(false) }
    var aiBusy by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    fun startWith(list: List<Question>) {
        if (list.isEmpty()) return
        testId = UUID.randomUUID().toString(); questions = list; index = 0; selected = -1; answered = false; correct = 0; startedAt = System.currentTimeMillis(); questionStartedAt = startedAt; state = AssessmentState.RUNNING
    }
    fun startLocal() { startWith(learning.selectQuestions(mode, if (mode == TestMode.PRACTICE) subject else null, if (mode == TestMode.MOCK) 10 else 5)) }
    fun finish() {
        val duration = ((System.currentTimeMillis() - startedAt) / 1000L).toInt()
        val attempts = learning.getQuestionAttempts().filter { it.testId == testId }
        val marks = attempts.fold(0) { total, attempt -> total + if (attempt.correct) 4 else -1 }
        learning.saveTestAttempt(TestAttemptRecord(testId, mode, startedAt, System.currentTimeMillis(), attempts.size, attempts.count { it.correct }, marks, attempts.size * 4, duration, attempts.groupBy { it.subject }.mapValues { (_, list) -> list.count { it.correct } to list.size }))
        state = AssessmentState.RESULT
    }

    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = when (state) { AssessmentState.SETUP -> "Assessment"; AssessmentState.RUNNING -> if (mode == TestMode.MOCK) "Starter mock" else "Practice test"; AssessmentState.RESULT -> "Result" }, onBack = onBack) }) { padding ->
        when (state) {
            AssessmentState.SETUP -> SetupContent(mode, subject, { mode = it }, { subject = it }, ::startLocal, onOpenMistakes, { showAiDialog = true }, Modifier.padding(padding))
            AssessmentState.RUNNING -> {
                val question = questions.getOrNull(index)
                if (question != null) {
                    var remaining by remember(index, testId) { mutableIntStateOf(if (mode == TestMode.MOCK) 90 else 60) }
                    LaunchedEffect(index, testId) { while (!answered && remaining > 0) { delay(1000); remaining-- } }
                    LaunchedEffect(remaining, answered) { if (!answered && remaining == 0) { selected = -1; learning.saveQuestionAttempt(testId, question, -1, 60); jee.flagChapterWeak(question.chapterId); answered = true } }
                    QuestionContent(Modifier.padding(padding), question, index, questions.size, selected, answered, remaining, { if (!answered) selected = it }, {
                        if (!answered && selected >= 0) { val record = learning.saveQuestionAttempt(testId, question, selected, ((System.currentTimeMillis() - questionStartedAt) / 1000).toInt()); if (record.correct) correct++ else jee.flagChapterWeak(question.chapterId); answered = true }
                    }, {
                        if (index + 1 >= questions.size) finish() else { index++; selected = -1; answered = false; questionStartedAt = System.currentTimeMillis() }
                    })
                }
            }
            AssessmentState.RESULT -> ResultContent(Modifier.padding(padding), correct, questions.size, { state = AssessmentState.SETUP }, onOpenMistakes)
        }
    }

    if (showAiDialog) {
        var difficulty by remember { mutableStateOf("Mixed") }
        var count by remember { mutableIntStateOf(10) }
        AlertDialog(onDismissRequest = { if (!aiBusy) showAiDialog = false }, title = { Text("Create AI quiz") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Generate a fresh quiz using your configured AI provider.", color = TextSecondary, fontSize = 12.sp)
                Text("Difficulty", color = TextPrimary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Easy", "Medium", "Hard", "Mixed").forEach { FilterChip(selected = difficulty == it, onClick = { difficulty = it }, label = { Text(it, fontSize = 11.sp) }) } }
                Text("Questions", color = TextPrimary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(5, 10, 15, 20).forEach { FilterChip(selected = count == it, onClick = { count = it }, label = { Text(it.toString()) }) } }
                aiError?.let { Text(it, color = AccentPink, fontSize = 11.sp) }
            }
        }, confirmButton = {
            Button(enabled = !aiBusy, onClick = {
                aiBusy = true; aiError = null
                scope.launch {
                    val result = AiQuizEngine(AiEngine(AiSettingsRepository(context))).generate(subject, difficulty, count)
                    aiBusy = false
                    if (result.success) { showAiDialog = false; startWith(result.questions) } else aiError = result.error ?: "Could not create quiz."
                }
            }) { if (aiBusy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Create quiz") }
        }, dismissButton = { TextButton(enabled = !aiBusy, onClick = { showAiDialog = false }) { Text("Cancel") } })
    }
}

@Composable
private fun SetupContent(mode: TestMode, subject: String, onMode: (TestMode) -> Unit, onSubject: (String) -> Unit, onStart: () -> Unit, onOpenMistakes: () -> Unit, onCreateAiQuiz: () -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCardAlt).padding(18.dp)) { Text("Practice & quiz", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(6.dp)); Text("Use the local question bank or create a fresh AI-generated quiz.", color = TextMuted, fontSize = 11.sp) } }
        item { Text("Mode", color = TextSecondary, fontSize = 12.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = mode == TestMode.PRACTICE, onClick = { onMode(TestMode.PRACTICE) }, label = { Text("Practice") }); FilterChip(selected = mode == TestMode.MOCK, onClick = { onMode(TestMode.MOCK) }, label = { Text("Starter mock") }) } }
        if (mode == TestMode.PRACTICE) item { Text("Subject", color = TextSecondary, fontSize = 12.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Physics", "Chemistry", "Mathematics").forEach { FilterChip(selected = subject == it, onClick = { onSubject(it) }, label = { Text(it) }) } } }
        item { Button(onClick = onCreateAiQuiz, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(7.dp)); Text("Create quiz with AI") } }
        item { OutlinedButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(7.dp)); Text(if (mode == TestMode.MOCK) "Start 10-question mock" else "Start local practice") } }
        item { OutlinedButton(onClick = onOpenMistakes, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.ErrorOutline, null); Spacer(Modifier.width(7.dp)); Text("Open mistake bank") } }
    }
}

@Composable
private fun QuestionContent(modifier: Modifier, question: Question, questionIndex: Int, total: Int, selected: Int, answered: Boolean, remaining: Int, onSelect: (Int) -> Unit, onSubmit: () -> Unit, onNext: () -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Question ${questionIndex + 1} / $total", color = TextSecondary, fontSize = 12.sp); Text("${remaining}s", color = if (remaining <= 10) AccentAmber else TextSecondary) } }
        item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).padding(16.dp)) { Text(question.chapter, color = AccentBlueLight, fontSize = 10.sp); Spacer(Modifier.height(7.dp)); Text(question.prompt, style = MaterialTheme.typography.titleMedium) } }
        items(question.options.indices.toList()) { optionIndex ->
            val chosen = selected == optionIndex
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (chosen) AccentBlueSoft else BgCard).selectable(selected = chosen, enabled = !answered, role = Role.RadioButton, onClick = { onSelect(optionIndex) }).padding(14.dp)) { RadioButton(selected = chosen, onClick = null, enabled = !answered); Spacer(Modifier.width(7.dp)); Text(question.options[optionIndex], modifier = Modifier.padding(top = 12.dp), color = TextOnCard) }
        }
        item { if (answered) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgCardAlt).padding(14.dp)) { Text(if (selected == question.correctIndex) "Correct" else "Review", color = if (selected == question.correctIndex) AccentGreen else AccentAmber, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(5.dp)); Text(question.explanation, color = TextSecondary, fontSize = 12.sp) }; Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(if (questionIndex + 1 == total) "Finish quiz" else "Next") } } else Button(onClick = onSubmit, enabled = selected >= 0, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Text("Submit answer") } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ResultContent(modifier: Modifier, correct: Int, total: Int, onRestart: () -> Unit, onMistakes: () -> Unit) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCardAlt).padding(18.dp)) { Text("Quiz complete", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp)); Text("$correct / $total correct", style = MaterialTheme.typography.headlineMedium); Text(if (total == 0) "No questions recorded" else "${(correct * 100) / total}% accuracy", color = TextSecondary, fontSize = 12.sp) }
        Button(onClick = onMistakes, modifier = Modifier.fillMaxWidth()) { Text("Review mistakes") }
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Create another quiz") }
    }
}
