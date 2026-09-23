package com.example.jeecommandcenter.ui.screens

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

    fun stream(prompt: String, title: String) {
        if (prompt.isBlank() || busy) return
        if (!settings.hasApiKey()) { onOpenSettings(); return }
        busy = true
        input = ""
        lastPrompt = prompt
        response = ""
        scope.launch {
            val result = AiEngine(settings).stream(
                prompt + "\n\nStudent context:\n" + contextSummary(analytics, topMistakes),
                "You are the JEE study coach. Use only supplied student data. Never invent performance data."
            ) { chunk -> response += chunk }
            if (result.success) history.save(title, prompt, result.text)
            else response = result.error ?: "AI request failed."
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
            val result = block()
            if (result.success) {
                response = result.text
                history.save(title, prompt, result.text)
            } else {
                response = result.error ?: "AI request failed."
            }
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
                        IconButton(onClick = onOpenHistory) {
                            Icon(Icons.Filled.History, "Chat history")
                        }
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (settings.hasApiKey()) Primary else TextMuted))
                        Spacer(Modifier.width(5.dp))
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 6.dp)
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
                IconButton(onClick = { stream(input, "Study coach") }, enabled = input.isNotBlank() && !busy) {
                    Icon(Icons.Filled.Send, "Ask coach", tint = if (input.isNotBlank() && !busy) PrimaryLight else TextMuted)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(top = 22.dp, bottom = 18.dp)
        ) {
            if (response.isBlank()) {
                item {
                    Spacer(Modifier.height(38.dp))
                    Icon(Icons.Filled.AutoAwesome, null, tint = PrimaryLight, modifier = Modifier.size(46.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("What can I help you with today?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text("Your personal JEE study coach", color = TextSecondary, fontSize = 12.sp)
                    if (coachingHeadline.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Current focus · " + coachingHeadline, color = TextMuted, fontSize = 10.sp)
                    }
                }
                item {
                    CoachPrompt("Analyze my preparation", "Find the biggest strengths and gaps in your current data.", Icons.Filled.Analytics) {
                        action("Preparation analysis", "Analyze my preparation", { orchestrator.analyzePerformance() })
                    }
                }
                item {
                    CoachPrompt("What should I study?", "Get the next concrete study action and the reason behind it.", Icons.Filled.Today) {
                        stream(
                            "What should I study today? Use my current study data, revision backlog, mistakes, and weak chapters to recommend the next concrete JEE study action.",
                            "Daily study recommendation"
                        )
                    }
                }
                item {
                    CoachPrompt("Analyze my mistakes", "Find recurring error patterns and what to do differently.", Icons.Filled.ErrorOutline) {
                        action("Mistake analysis", "Analyze my mistakes", { orchestrator.explainMistakes() })
                    }
                }
                if (!settings.hasApiKey()) {
                    item { TextButton(onClick = onOpenSettings) { Text("Configure AI to start coaching") } }
                }
            } else {
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = BgCardAlt,
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder.copy(alpha = .75f))
                    ) {
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
private fun CoachPrompt(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder.copy(alpha = .8f), RoundedCornerShape(18.dp))
            .premiumClick(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            Icon(icon, null, tint = PrimaryLight, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
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
    val regex = Regex("\*\*(.+?)\*\*")
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
    val errors = mistakes.joinToString("; ") { it.subject + "/" + it.chapterId + " repeated " + it.count + "x, type=" + it.mistakeType.name }
        .ifBlank { "No unresolved mistakes recorded." }
    return "7-day study: " + analytics.studyMinutes7d + " minutes. Overall accuracy: " +
        (analytics.accuracy * 100).toInt() + "%. Tests: " + analytics.testsCompleted +
        ". Unresolved mistakes: " + analytics.unresolvedMistakes + ". Repeated mistakes: " +
        analytics.repeatedMistakes + ". Subjects: " + subjects + ". Recurring errors: " + errors + "."
}
