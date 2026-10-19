package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import com.example.jeecommandcenter.ui.components.*

@Composable
fun MistakeBankScreen(learning: LearningRepository, onBack: () -> Unit, onOpenTutor: () -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    var syncingCloudMistakes by remember { mutableStateOf(false) }
    val mistakes = remember(refresh) { learning.getMistakes().filterNot { it.resolved } }
    val cloud = remember { CloudJeeRepository() }

    // Import mistakes recorded in the cloud (including attempts made on another device).
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
                )) imported++
            }
            if (imported > 0) refresh++
        }
        syncingCloudMistakes = false
    }
    Scaffold(containerColor = BgApp, topBar = {
        JeeTopBar(title = "Mistake Bank", subtitle = if (syncingCloudMistakes) "Syncing saved attempts…" else "${mistakes.size} unresolved questions", onBack = onBack, trailing = { Text(mistakes.size.toString(), color = AccentAmber) })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (mistakes.isEmpty()) {
                item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).border(JeeSurfaceTokens.borderWidth,BgCardBorder.copy(alpha=JeeSurfaceTokens.cardBorderAlpha),JeeShapes.medium).padding(18.dp)) { Text("No unresolved mistakes", style = MaterialTheme.typography.titleMedium); Text("Incorrect answers from Chapter Practice, Tests & Mocks, and supported quizzes appear here automatically.", color = TextMuted, fontSize = 12.sp) } }
            } else items(mistakes, key = { it.id }) { mistake -> MistakeCard(mistake, learning) { refresh++ } }
            item {
                Button(onClick = onOpenTutor, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(7.dp)); Text("Ask AI about my mistakes")
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MistakeCard(mistake: MistakeRecord, learning: LearningRepository, onRefresh: () -> Unit) {
    var expanded by remember(mistake.id) { mutableStateOf(false) }
    val chapterName = JeeCatalog.find(mistake.chapterId)?.name ?: mistake.chapterId.replace("_", " ").replaceFirstChar { it.uppercase() }
    val sourceLabel = when (mistake.source.lowercase()) { "practice" -> "Chapter Practice"; "assessment" -> "Tests & Mocks"; else -> mistake.source.replaceFirstChar { it.uppercase() } }
    var typeMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).border(JeeSurfaceTokens.borderWidth,BgCardBorder.copy(alpha=JeeSurfaceTokens.cardBorderAlpha),JeeShapes.medium).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) { Text(mistake.subject + " · " + chapterName + " · " + sourceLabel, color = AccentBlueLight, fontSize = 10.sp); Spacer(Modifier.height(4.dp)); MathMarkdownText(mistake.questionPrompt, modifier = Modifier.fillMaxWidth(), color = TextPrimary, fontSize = MaterialTheme.typography.titleMedium.fontSize, lineHeight = MaterialTheme.typography.titleMedium.lineHeight) }
            Text("×" + mistake.count, color = if (mistake.count >= 2) AccentAmber else TextMuted)
        }
        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedButton(onClick = { typeMenu = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), modifier = Modifier.height(36.dp)) { Text("Type: " + mistake.mistakeType.name.replace('_', ' '), fontSize = 10.sp) }
            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                MistakeType.entries.forEach { type -> DropdownMenuItem(text = { Text(type.name.replace('_', ' '), fontSize = 11.sp) }, onClick = { learning.updateMistakeType(mistake.id, type); typeMenu = false; onRefresh() }) }
            }
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("Your answer:", color = TextSecondary, fontSize = 12.sp); MathMarkdownText(mistake.lastSelectedAnswer, modifier = Modifier.fillMaxWidth(), color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("Correct answer:", color = AccentGreen, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium); MathMarkdownText(mistake.correctAnswer, modifier = Modifier.fillMaxWidth(), color = AccentGreen, fontSize = 12.sp, lineHeight = 17.sp) }
            if (mistake.correction.isNotBlank()) {
                Text("Solution", color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 12.sp)
                MathMarkdownText(mistake.correction, modifier = Modifier.fillMaxWidth(), color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            } else {
                Text("No explanation was saved for this question.", color = TextMuted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.weight(1f)) { Text(if (expanded) "Hide answer" else "Answer") }
            Button(onClick = { learning.resolveMistake(mistake.id); onRefresh() }, modifier = Modifier.weight(1f)) { Text("Resolve") }
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
