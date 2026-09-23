package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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

    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "AI Tutor", onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { action { orchestrator.analyzePerformance() } }, label = { Text("Analyze me") })
                AssistChip(onClick = { action { orchestrator.buildDailyStudyPlan() } }, label = { Text("Plan my day") })
            } }
            item { AssistChip(onClick = { action { orchestrator.explainMistakes() } }, label = { Text("Analyze mistakes") }) }
            if (response.isNotBlank()) item {
                Column(Modifier.fillMaxWidth().clip(JeeShapes.large).background(BgCard).border(JeeSurfaceTokens.borderWidth,BgCardBorder.copy(alpha=JeeSurfaceTokens.cardBorderAlpha),JeeShapes.medium).padding(16.dp)) {
                    Text("AI", color = AccentBlueLight, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    MarkdownText(response)
                    if (busy) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator(Modifier.fillMaxWidth(), color = AccentBlue) }
                }
            }
            item { OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("Ask your JEE tutor") }, placeholder = { Text("e.g. Teach me electrostatics from first principles") }) }
            item { Button(onClick = { stream(input) }, enabled = input.isNotBlank() && !busy && settings.hasApiKey(), modifier = Modifier.fillMaxWidth()) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.Send, null, Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp)); Text(if (busy) "Thinking..." else "Ask AI")
            } }
            item { Text(if (!settings.hasApiKey()) "Configure an AI provider in Settings → AI Hub." else "Responses stream as they arrive. Markdown headings, lists and emphasis are rendered.", color = TextMuted, fontSize = 10.sp) }
            if (!settings.hasApiKey()) item { OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Open AI configuration") } }
            item { Spacer(Modifier.height(24.dp)) }
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
