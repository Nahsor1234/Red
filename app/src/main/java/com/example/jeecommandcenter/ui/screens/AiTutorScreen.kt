package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
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
        if (prompt.isBlank() || busy) return
        busy = true; input = ""; response = ""
        scope.launch {
            val result = AiEngine(settings).askStreaming(prompt + "\n\nStudent context:\n" + contextSummary(analytics, topMistakes), "You are the JEE preparation tutor. Give accurate, actionable explanations. Use Markdown headings, bullets, numbered lists and bold emphasis. Never invent student data.") { chunk -> response += chunk }
            if (!result.success && response.isBlank()) response = result.error ?: "AI request failed."
            busy = false
        }
    }

    fun runAnalysis(action: suspend () -> AiResult) {
        if (busy) return
        busy = true; response = ""
        scope.launch { val result = action(); response = if (result.success) result.text else (result.error ?: "AI request failed."); busy = false }
    }

    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "AI Tutor", onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                AssistChip(onClick = { if (settings.hasApiKey()) runAnalysis { orchestrator.analyzePerformance() } }, label = { Text("Analyze me") })
                AssistChip(onClick = { if (settings.hasApiKey()) runAnalysis { orchestrator.buildDailyStudyPlan() } }, label = { Text("Plan my day") })
                AssistChip(onClick = { if (settings.hasApiKey()) runAnalysis { orchestrator.explainMistakes() } }, label = { Text("Mistakes") })
            } }
            if (response.isNotBlank()) item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCardAlt).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("AI", color = AccentBlueLight, fontSize = 11.sp); if (busy) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = AccentBlue) }
                Spacer(Modifier.height(8.dp)); AiMarkdown(response)
            } }
            item { OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("Ask your JEE tutor") }, placeholder = { Text("e.g. Explain Gauss's law with a simple example") }) }
            item { Button(onClick = { stream(input) }, enabled = input.isNotBlank() && !busy && settings.hasApiKey(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF17120A)) else Icon(Icons.Filled.Send, null, Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp)); Text(if (busy) "Streaming..." else "Ask AI")
            } }
            item { Text(if (!settings.hasApiKey()) "Configure an AI provider in Settings → AI Hub." else "Responses stream live and render Markdown headings, lists and emphasis.", color = TextMuted, fontSize = 10.sp); if (!settings.hasApiKey()) OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Open AI configuration") } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AiMarkdown(markdown: String) {
    var inCode by remember(markdown) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        markdown.replace("\r", "").split("\n").forEach { raw ->
            val line = raw.trimEnd()
            if (line.trim() == "```") { inCode = !inCode; return@forEach }
            when {
                inCode -> Text(line, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().background(BgApp, RoundedCornerShape(8.dp)).padding(9.dp))
                line.startsWith("### ") -> Text(line.removePrefix("### "), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                line.startsWith("## ") -> Text(line.removePrefix("## "), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
                line.startsWith("# ") -> Text(line.removePrefix("# "), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                line.startsWith("- ") || line.startsWith("* ") -> Text("• " + line.drop(2), color = TextPrimary, fontSize = 13.sp)
                line.matches(Regex("^\\d+\\. .*")) -> Text(line, color = TextPrimary, fontSize = 13.sp)
                line.isBlank() -> Spacer(Modifier.height(2.dp))
                else -> Text(inlineMarkdown(line), color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val regex = Regex("\\*\\*(.+?)\\*\\*")
    var cursor = 0
    regex.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first)); withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }; cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

private fun contextSummary(analytics: AnalyticsSnapshot, mistakes: List<MistakeRecord>): String {
    val subjects = analytics.subjectAnalytics.joinToString("; ") { it.subject + ": " + it.attempted + " attempted, " + (it.accuracy * 100).toInt() + "% accuracy" }
    val errors = mistakes.joinToString("; ") { it.subject + "/" + it.chapterId + " repeated " + it.count + "x, type=" + it.mistakeType.name }.ifBlank { "No unresolved mistakes recorded." }
    return "7-day study: ${analytics.studyMinutes7d} minutes. Overall accuracy: ${(analytics.accuracy * 100).toInt()}%. Tests: ${analytics.testsCompleted}. Unresolved mistakes: ${analytics.unresolvedMistakes}. Repeated mistakes: ${analytics.repeatedMistakes}. Subjects: $subjects. Recurring errors: $errors."
}
