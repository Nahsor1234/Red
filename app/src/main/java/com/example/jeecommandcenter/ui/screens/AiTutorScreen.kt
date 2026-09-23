package com.example.jeecommandcenter.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AiTutorScreen(
    context: android.content.Context,
    jee: JeeRepository,
    learning: LearningRepository,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    conversationId: Long? = null,
    onConversationOpened: (Long) -> Unit = {},
    onConversationCleared: () -> Unit = {}
) {
    val settings = remember { AiSettingsRepository(context) }
    val orchestrator = remember { AiOrchestrator(context) }
    val history = remember { AiChatHistoryRepository(context) }
    val scope = rememberCoroutineScope()
    val analytics = remember { learning.analytics(jee) }
    val intelligence = remember { JeeIntelligence(jee, learning) }
    val priorities = remember { intelligence.dailyPriorities(3) }
    val revisions = remember { intelligence.revisionRecommendations(3) }
    val weak = remember { intelligence.weakChapters(3) }
    val topMistakes = remember { learning.getMistakes().filterNot { it.resolved }.take(5) }
    val hasData = analytics.studyMinutes7d > 0 || analytics.testsCompleted > 0 || analytics.unresolvedMistakes > 0 || priorities.isNotEmpty()
    val coachingHeadline = when { priorities.isNotEmpty() -> priorities.first().title; revisions.isNotEmpty() -> "Clear " + revisions.first().chapter.name + " revision"; weak.isNotEmpty() -> "Strengthen " + weak.first().chapter.name; hasData -> "Keep building your study signal"; else -> "Start collecting real study data" }
    var activeId by remember(conversationId) { mutableStateOf(conversationId) }
    var messages by remember(conversationId) { mutableStateOf(conversationId?.let { history.getConversation(it)?.messages }.orEmpty()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var streamingText by remember { mutableStateOf("") }
    var activeRequestJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(conversationId) { activeId = conversationId; messages = conversationId?.let { history.getConversation(it)?.messages }.orEmpty() }

    fun ensureConversation(title: String): Long {
        activeId?.let { return it }
        val created = history.createConversation(title)
        activeId = created.id
        messages = emptyList()
        onConversationOpened(created.id)
        return created.id
    }

    fun exitConversation() {
        activeRequestJob?.cancel()
        activeRequestJob = null
        activeId = null
        messages = emptyList()
        streamingText = ""
        busy = false
        history.setActiveConversationId(null)
        onConversationCleared()
    }

    fun handleBack() {
        if (activeId != null || messages.isNotEmpty()) exitConversation() else onBack()
    }

    fun send(prompt: String, title: String = "Study coach", block: (suspend () -> AiResult)? = null) {
        if (prompt.isBlank() || busy) return
        if (!settings.hasApiKey()) {
            onOpenSettings()
            return
        }

        val id = ensureConversation(title)
        history.appendMessage(id, "USER", prompt.trim())
        messages = history.getConversation(id)?.messages.orEmpty()
        input = ""
        streamingText = ""
        busy = true

        activeRequestJob = scope.launch {
            val result = try {
                block?.invoke() ?: run {
                    val turns = history.getConversation(id)?.messages.orEmpty()
                        .takeLast(24)
                        .map { message ->
                            AiPromptMessage(
                                role = if (message.role == "COACH") "assistant" else "user",
                                content = message.text
                            )
                        }

                    AiEngine(settings).streamConversation(
                        messages = turns,
                        systemInstruction = JeeAiPrompt.taskInstruction(
                            "multi-turn JEE study coaching"
                        ) + "\n\nCurrent student context:\n" +
                            contextSummary(analytics, topMistakes),
                        onChunk = { chunk ->
                            withContext(Dispatchers.Main.immediate) {
                                streamingText += chunk
                            }
                        }
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                AiResult(false, error = error.message ?: "AI request failed.")
            }

            activeRequestJob = null
            val coachText = if (result.success && result.text.isNotBlank()) {
                result.text
            } else {
                result.error ?: "The coach returned no response. Try again."
            }

            withContext(Dispatchers.Main.immediate) {
                if (result.success && result.text.isNotBlank()) {
                    history.appendMessage(id, "COACH", coachText)
                } else {
                    history.appendMessage(id, "COACH", coachText)
                }
                messages = history.getConversation(id)?.messages.orEmpty()
                streamingText = ""
                busy = false
            }
        }
    }

    val inChat = activeId != null || messages.isNotEmpty()

    BackHandler(enabled = true) {
        handleBack()
    }
    Scaffold(containerColor = BgApp, topBar = {
        JeeTopBar(title = "AI Study Coach", onBack = ::handleBack, trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpenHistory) { Icon(Icons.Filled.History, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("History", fontSize = 12.sp) }
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (settings.hasApiKey()) Primary else TextMuted)); Spacer(Modifier.width(5.dp))
            }
        })
    }, bottomBar = {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 10.dp, top = 5.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (!inChat) {
                CoachQuickAction("Analyze my preparation", Icons.Filled.Analytics, Modifier.fillMaxWidth()) { send("Analyze my preparation", "Preparation analysis") { orchestrator.analyzePerformance() } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CoachQuickAction("What should I study?", Icons.Filled.Today, Modifier.weight(1f)) { send("What should I study now? Use my current study data, revision backlog, mistakes, and weak chapters to recommend the next concrete JEE study action.", "Daily study recommendation") }
                    CoachQuickAction("Analyze my mistakes from now", Icons.Filled.ErrorOutline, Modifier.weight(1f)) { send("Analyze my mistakes from now", "Mistake analysis") { orchestrator.explainMistakes() } }
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(BgCardAlt).border(1.dp, BgCardBorder.copy(alpha = .85f), RoundedCornerShape(28.dp)).padding(horizontal = 5.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, null, tint = TextSecondary, modifier = Modifier.padding(8.dp).size(21.dp))
                TextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Ask your study coach", color = TextMuted) }, singleLine = false, maxLines = 3, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                IconButton(onClick = { send(input) }, enabled = input.isNotBlank() && !busy) { Icon(Icons.Filled.Send, "Ask coach", tint = if (input.isNotBlank() && !busy) PrimaryLight else TextMuted) }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(top = 18.dp, bottom = 18.dp)) {
            if (!inChat) item {
                Spacer(Modifier.height(90.dp)); Icon(Icons.Filled.AutoAwesome, null, tint = PrimaryLight, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(14.dp))
                Text("How can I help with your JEE prep?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(6.dp))
                Text("Your study data is available to the coach. Ask anything, or use a quick action below.", color = TextSecondary, fontSize = 12.sp); Spacer(Modifier.height(12.dp)); Text("Current signal · $coachingHeadline", color = TextMuted, fontSize = 10.sp)
            } else {
                items(messages, key = { it.id }) { message ->
                    val isUser = message.role == "USER"
                    Surface(Modifier.fillMaxWidth(), color = if (isUser) BgCardAlt else BgCard, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder.copy(alpha = .75f))) {
                        Column(Modifier.padding(15.dp)) { Text(if (isUser) "YOU" else "COACH", color = PrimaryLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .9.sp); Spacer(Modifier.height(7.dp)); if (isUser) Text(message.text, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium) else MarkdownText(message.text) }
                    }
                }
                if (busy || streamingText.isNotBlank()) item(key = "streaming-coach-message") {
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = BgCard,
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder.copy(alpha = .75f))
                    ) {
                        Column(Modifier.padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "COACH",
                                    color = PrimaryLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = .9.sp
                                )
                                if (streamingText.isBlank()) {
                                    Spacer(Modifier.width(9.dp))
                                    CircularProgressIndicator(
                                        Modifier.size(13.dp),
                                        strokeWidth = 2.dp,
                                        color = Primary
                                    )
                                }
                            }
                            Spacer(Modifier.height(7.dp))
                            if (streamingText.isBlank()) {
                                Text(
                                    "Coach is thinking…",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            } else {
                                MarkdownText(streamingText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun CoachQuickAction(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) { Row(modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(18.dp)).background(BgCard).border(1.dp, BgCardBorder.copy(alpha = .8f), RoundedCornerShape(18.dp)).premiumClick(onClick).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = PrimaryLight, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(title, color = TextOnCard, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 2) } }

@Composable private fun MarkdownText(markdown: String) {
    var inCode = false
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        markdown.replace("\r\n", "\n").split("\n").forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.trim() == "```" -> inCode = !inCode
                inCode -> Text(line, color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                line.startsWith("### ") -> Text(line.removePrefix("### "), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                line.startsWith("## ") -> Text(line.removePrefix("## "), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                line.startsWith("# ") -> Text(line.removePrefix("# "), style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                line.startsWith("- ") || line.startsWith("* ") -> Text("• " + line.substring(2).inlineMarkdown(), color = TextOnCard, fontSize = 13.sp)
                line.matches(Regex("^\\d+\\. .*")) -> Text(line.inlineMarkdown(), color = TextOnCard, fontSize = 13.sp)
                line.isBlank() -> Spacer(Modifier.height(2.dp))
                else -> Text(line.inlineMarkdown(), color = TextOnCard, fontSize = 13.sp)
            }
        }
    }
}

private fun String.inlineMarkdown() = buildAnnotatedString {
    val regex = Regex("\\*\\*(.+?)\\*\\*")
    var cursor = 0
    regex.findAll(this@inlineMarkdown).forEach { match -> append(this@inlineMarkdown.substring(cursor, match.range.first)); withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }; cursor = match.range.last + 1 }
    append(this@inlineMarkdown.substring(cursor))
}

private fun contextSummary(analytics: AnalyticsSnapshot, mistakes: List<MistakeRecord>): String {
    val subjects = analytics.subjectAnalytics.joinToString("; ") { it.subject + ": " + it.attempted + " attempted, " + (it.accuracy * 100).toInt() + "% accuracy" }
    val errors = mistakes.joinToString("; ") { it.subject + "/" + it.chapterId + " repeated " + it.count + "x, type=" + it.mistakeType.name }.ifBlank { "No unresolved mistakes recorded." }
    return "7-day study: " + analytics.studyMinutes7d + " minutes. Overall accuracy: " + (analytics.accuracy * 100).toInt() + "%. Tests: " + analytics.testsCompleted + ". Unresolved mistakes: " + analytics.unresolvedMistakes + ". Repeated mistakes: " + analytics.repeatedMistakes + ". Subjects: " + subjects + ". Recurring errors: " + errors + "."
}
