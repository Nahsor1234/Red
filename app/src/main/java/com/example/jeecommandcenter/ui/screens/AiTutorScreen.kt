package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiTutorScreen(
    context: android.content.Context,
    jee: JeeRepository,
    learning: LearningRepository,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {}
) {
    val settings = remember { AiSettingsRepository(context) }
    val orchestrator = remember { AiOrchestrator(context) }
    val history = remember { AiChatHistoryRepository(context) }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var lastPrompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val analytics = remember { learning.analytics(jee) }
    val intelligence = remember { JeeIntelligence(jee, learning) }
    val priorities = remember { intelligence.dailyPriorities(3) }
    val revisions = remember { intelligence.revisionRecommendations(3) }
    val weak = remember { intelligence.weakChapters(3) }
    val topMistakes = remember { learning.getMistakes().filterNot { it.resolved }.take(5) }
    val hasData = analytics.studyMinutes7d > 0 || analytics.testsCompleted > 0 || analytics.unresolvedMistakes > 0 || priorities.isNotEmpty()
    val coachingHeadline = when {
        priorities.isNotEmpty() -> priorities.first().title
        revisions.isNotEmpty() -> "Clear " + revisions.first().chapter.name + " revision"
        weak.isNotEmpty() -> "Strengthen " + weak.first().chapter.name
        hasData -> "Keep building your study signal"
        else -> "Start collecting real study data"
    }

    fun askCoach(prompt: String, title: String) {
        if (prompt.isBlank() || busy) return
        if (!settings.hasApiKey()) { onOpenSettings(); return }
        busy = true
        lastPrompt = prompt
        input = ""
        response = ""
        scope.launch {
            val result = runCatching {
                AiEngine(settings).ask(
                    prompt + "\n\nStudent context:\n" + contextSummary(analytics, topMistakes),
                    "You are the JEE study coach. Use only supplied student data. Never invent performance data. Give concise, actionable guidance."
                )
            }.getOrElse { AiResult(false, error = it.message ?: "AI request failed.") }
            response = if (result.success && result.text.isNotBlank()) result.text else (result.error ?: "The coach returned no response. Try again.")
            if (result.success && result.text.isNotBlank()) history.save(title, prompt, result.text)
            busy = false
        }
    }

    fun action(title: String, prompt: String, block: suspend () -> AiResult) {
        if (busy) return
        if (!settings.hasApiKey()) { onOpenSettings(); return }
        busy = true
        lastPrompt = prompt
        response = ""
        scope.launch {
            val result = runCatching { block() }.getOrElse { AiResult(false, error = it.message ?: "AI request failed.") }
            response = if (result.success && result.text.isNotBlank()) result.text else (result.error ?: "The coach returned no response. Try again.")
            if (result.success && result.text.isNotBlank()) history.save(title, prompt, result.text)
            busy = false
        }
    }

    Scaffold(
        containerColor = BgApp,
        topBar = {
            JeeTopBar(
                title = "AI Study Coach",
                onBack = onBack,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onOpenHistory) { Icon(Icons.Filled.History, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("History", fontSize = 12.sp) }
                        Box(Modifier.size(7.dp).clip(CircleShape).background(if (settings.hasApiKey()) Primary else TextMuted))
                        Spacer(Modifier.width(5.dp))
                    }
                }
            )
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp, top = 5.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (!busy && response.isBlank()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CoachQuickAction("Analyze my preparation", Icons.Filled.Analytics, Modifier.weight(1f)) {
                            action("Preparation analysis", "Analyze my preparation", { orchestrator.analyzePerformance() })
                        }
                        CoachQuickAction("What should I study?", Icons.Filled.Today, Modifier.weight(1f)) {
                            askCoach("What should I study now? Use my current study data, revision backlog, mistakes, and weak chapters to recommend the next concrete JEE study action.", "Daily study recommendation")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CoachQuickAction("Analyze my mistakes from now", Icons.Filled.ErrorOutline, Modifier.weight(1f)) {
                            action("Mistake analysis", "Analyze my mistakes from now", { orchestrator.explainMistakes() })
                        }
                        CoachQuickAction("Chat with coach", Icons.Filled.AutoAwesome, Modifier.weight(1f)) { }
                    }
                }
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(BgCardAlt)
                        .border(1.dp, BgCardBorder.copy(alpha = .85f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, null, tint = TextSecondary, modifier = Modifier.padding(8.dp).size(21.dp))
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask your study coach", color = TextMuted) },
                        singleLine = false,
                        maxLines = 3,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                    IconButton(onClick = { askCoach(input, "Study coach") }, enabled = input.isNotBlank() && !busy) {
                        Icon(Icons.Filled.Send, "Ask coach", tint = if (input.isNotBlank() && !busy) PrimaryLight else TextMuted)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 18.dp)
        ) {
            if (response.isBlank()) {
                item {
                    Spacer(Modifier.height(90.dp))
                    Icon(Icons.Filled.AutoAwesome, null, tint = PrimaryLight, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("How can I help with your JEE prep?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Your study data is available to the coach. Ask anything, or use a quick action below.", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Current signal · $coachingHeadline", color = TextMuted, fontSize = 10.sp)
                }
            } else {
                item {
                    Surface(Modifier.fillMaxWidth(), color = BgCardAlt, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder.copy(alpha = .75f))) {
                        Text(lastPrompt, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(15.dp))
                    }
                }
                item {
                    JeeCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("COACH", color = PrimaryLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .9.sp)
                            if (busy) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = Primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        MarkdownText(response)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachQuickAction(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder.copy(alpha = .8f), RoundedCornerShape(18.dp))
            .premiumClick(onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, color = TextOnCard, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 2)
    }
}

@Composable
private fun MarkdownText(markdown: String) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        markdown.replace("\r\n", "\n").split("\n").forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.startsWith("### ") -> Text(line.removePrefix("### "), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                line.startsWith("## ") -> Text(line.removePrefix("## "), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                line.startsWith("# ") -> Text(line.removePrefix("# "), style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                line.startsWith("- ") || line.startsWith("* ") -> Text("• " + line.substring(2).inlineMarkdown(), color = TextOnCard, fontSize = 13.sp)
                line.matches(Regex("^\\d+\\. .*")) -> Text(line, color = TextOnCard, fontSize = 13.sp)
                line.startsWith("```") -> Text(line.removePrefix("```").ifBlank { " " }, color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                line.isBlank() -> Spacer(Modifier.height(2.dp))
                else -> Text(line.inlineMarkdown(), color = TextOnCard, fontSize = 13.sp)
            }
        }
    }
}

private fun String.inlineMarkdown() = buildAnnotatedString {
    val regex = Regex("\\*\\*(.+?)\\*\\*")
    var cursor = 0
    regex.findAll(this@inlineMarkdown).forEach { match ->
        append(this@inlineMarkdown.substring(cursor, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
        cursor = match.range.last + 1
    }
    append(this@inlineMarkdown.substring(cursor))
}

private fun contextSummary(analytics: AnalyticsSnapshot, mistakes: List<MistakeRecord>): String {
    val subjects = analytics.subjectAnalytics.joinToString("; ") { it.subject + ": " + it.attempted + " attempted, " + (it.accuracy * 100).toInt() + "% accuracy" }
    val errors = mistakes.joinToString("; ") { it.subject + "/" + it.chapterId + " repeated " + it.count + "x, type=" + it.mistakeType.name }.ifBlank { "No unresolved mistakes recorded." }
    return "7-day study: " + analytics.studyMinutes7d + " minutes. Overall accuracy: " + (analytics.accuracy * 100).toInt() + "%. Tests: " + analytics.testsCompleted + ". Unresolved mistakes: " + analytics.unresolvedMistakes + ". Repeated mistakes: " + analytics.repeatedMistakes + ". Subjects: " + subjects + ". Recurring errors: " + errors + "."
}
