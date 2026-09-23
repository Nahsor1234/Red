package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import com.example.jeecommandcenter.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun AiTutorScreen(context: android.content.Context, jee: JeeRepository, learning: LearningRepository, onBack: () -> Unit, onOpenSettings: () -> Unit = {}) {
    val settings = remember { AiSettingsRepository(context) }
    val orchestrator = remember { AiOrchestrator(context) }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val analytics = remember { learning.analytics(jee) }
    val intelligence = remember { JeeIntelligence(jee, learning) }
    val mistakes = remember { learning.getMistakes().filterNot { it.resolved } }
    val weak = remember { intelligence.weakChapters(3) }
    val priorities = remember { intelligence.dailyPriorities(3) }
    val revisions = remember { intelligence.revisionRecommendations(3) }
    val hasData = analytics.studyMinutes7d > 0 || analytics.testsCompleted > 0 || analytics.unresolvedMistakes > 0 || priorities.isNotEmpty()
    val coachingHeadline = when {
        priorities.isNotEmpty() -> priorities.first().title
        revisions.isNotEmpty() -> "Clear " + revisions.first().chapter.name + " revision"
        weak.isNotEmpty() -> "Strengthen " + weak.first().chapter.name
        hasData -> "Keep building your study signal"
        else -> "Start collecting real study data"
    }
    val coachingReason = when {
        priorities.isNotEmpty() -> priorities.first().reason
        revisions.isNotEmpty() -> revisions.first().reason
        weak.isNotEmpty() -> weak.first().signals.take(2).joinToString(" + ")
        hasData -> "Your recent data is present, but it does not yet produce a strong priority."
        else -> "Study sessions, questions, revisions and tests will make the coach more useful."
    }
    val topMistakes = remember { learning.getMistakes().filterNot { it.resolved }.take(5) }

    fun stream(prompt: String) {
        if (prompt.isBlank() || busy || !settings.hasApiKey()) return
        busy = true; input = ""; response = ""
        scope.launch {
            val result = AiEngine(settings).stream(prompt + "\n\nStudent context:\n" + contextSummary(analytics, topMistakes), "You are the JeE JEE preparation tutor. Explain clearly, use step-by-step reasoning, identify misconceptions, and never invent student performance data.") { chunk -> response += chunk }
            if (!result.success) response = result.error ?: "AI request failed."
            busy = false
        }
    }

    fun action(block: suspend () -> AiResult) {
        if (busy || !settings.hasApiKey()) return
        busy = true; response = ""
        scope.launch {
            val result = block()
            response = if (result.success) result.text else result.error ?: "AI request failed."
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
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (settings.hasApiKey()) Primary else TextMuted))
                        Spacer(Modifier.width(6.dp))
                        Text(if (settings.hasApiKey()) "Ready" else "Setup", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                JeeCard(featured = true) {
                    Text("WHAT'S IMPORTANT TODAY?", color = PrimaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.05.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(coachingHeadline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(coachingReason, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CoachMetric(analytics.studyMinutes7d.toString() + "m", "7d study", Modifier.weight(1f))
                        CoachMetric((analytics.accuracy * 100).toInt().toString() + "%", "accuracy", Modifier.weight(1f))
                        CoachMetric(jee.getRevisionQueue().size.toString(), "revision due", Modifier.weight(1f))
                        CoachMetric(analytics.unresolvedMistakes.toString(), "mistakes", Modifier.weight(1f))
                    }
                }
            }
            item {
                SectionHeader("Next actions")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoachAction("Analyze preparation", Icons.Filled.Insights, Modifier.weight(1f)) { action { orchestrator.analyzePerformance() } }
                        CoachAction("Plan my day", Icons.Filled.Today, Modifier.weight(1f)) { action { orchestrator.buildDailyStudyPlan() } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoachAction("Analyze mistakes", Icons.Filled.ErrorOutline, Modifier.weight(1f)) { action { orchestrator.explainMistakes() } }
                        CoachAction("Learn a topic", Icons.Filled.School, Modifier.weight(1f)) {
                            stream("Help me choose and learn the most useful JEE topic for my current preparation. Start with one topic and teach it from first principles.")
                        }
                    }
                }
            }
            if (response.isNotBlank()) {
                item {
                    JeeCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("LATEST COACHING INSIGHT", color = PrimaryLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .9.sp)
                            if (busy) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = Primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        MarkdownText(response)
                    }
                }
            }
            item {
                SectionHeader("Ask your coach")
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("Ask anything about your JEE preparation") },
                    placeholder = { Text("e.g. How should I recover my Physics accuracy this week?") }
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { stream(input) },
                    enabled = input.isNotBlank() && !busy && settings.hasApiKey(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Send, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (busy) "Thinking..." else "Ask coach")
                }
            }
            if (!settings.hasApiKey()) {
                item {
                    JeeCard {
                        Text("AI is optional", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(5.dp))
                        Text("The local focus, revision and analytics systems continue working without AI.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
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
                line.startsWith("```") -> Text(line.removePrefix("```").ifBlank { " " }, color = TextSecondary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
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
    return "7-day study: ${analytics.studyMinutes7d} minutes. Overall accuracy: ${(analytics.accuracy * 100).toInt()}%. Tests: ${analytics.testsCompleted}. Unresolved mistakes: ${analytics.unresolvedMistakes}. Repeated mistakes: ${analytics.repeatedMistakes}. Subjects: $subjects. Recurring errors: $errors."
}
