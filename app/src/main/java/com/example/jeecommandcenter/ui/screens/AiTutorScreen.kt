package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import com.example.jeecommandcenter.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun AiTutorScreen(
    context: android.content.Context,
    jee: JeeRepository,
    learning: LearningRepository,
    onBack: () -> Unit
) {
    val settings = remember { AiSettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val analytics = remember { learning.analytics(jee) }
    val topMistakes = remember { learning.getMistakes().filterNot { it.resolved }.take(5) }

    fun send(prompt: String) {
        if (prompt.isBlank() || busy) return
        busy = true
        input = ""
        scope.launch {
            val result = AiEngine(settings).ask(
                prompt = prompt + "\n\nStudent context:\n" + contextSummary(analytics, topMistakes),
                systemInstruction = "You are the JeE JEE preparation tutor. Explain clearly, use step-by-step reasoning, identify misconceptions, and never invent the student performance data."
            )
            response = if (result.success) result.text else (result.error ?: "AI request failed.")
            busy = false
        }
    }

    Scaffold(
        containerColor = BgApp,
        topBar = {
            JeeTopBar(title = "AI Tutor", onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { send("Analyze my recent performance and identify my biggest study bottleneck.") },
                        label = { Text("Analyze me") }
                    )
                    AssistChip(
                        onClick = { send("Based on my mistakes, what should I revise today? Give me a concrete 60-minute sequence.") },
                        label = { Text("Plan 60 min") }
                    )
                }
            }
            item {
                AssistChip(
                    onClick = { send("Explain my recurring mistakes and give me two practical ways to prevent them.") },
                    label = { Text("Analyze mistakes") }
                )
            }
            if (response.isNotBlank()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).padding(16.dp)
                    ) {
                        Text("AI", color = AccentPurple, fontSize = 11.sp)
                        Spacer(Modifier.height(7.dp))
                        Text(response, color = TextOnCard, fontSize = 13.sp)
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    label = { Text("Ask your JEE tutor") },
                    placeholder = { Text("e.g. Teach me electrostatics from first principles") }
                )
            }
            item {
                Button(
                    onClick = { send(input) },
                    enabled = input.isNotBlank() && !busy && settings.hasApiKey(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Send, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (busy) "Thinking..." else "Ask AI")
                }
            }
            item {
                Text(
                    if (!settings.hasApiKey()) "Configure an AI provider in Settings → AI Hub first."
                    else "AI uses your current analytics, mistakes and revision state as context.",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun contextSummary(
    analytics: AnalyticsSnapshot,
    mistakes: List<MistakeRecord>
): String {
    val subjects = analytics.subjectAnalytics.joinToString("; ") {
        it.subject + ": " + it.attempted + " attempted, " + (it.accuracy * 100).toInt() + "% accuracy"
    }
    val errors = mistakes.joinToString("; ") {
        it.subject + "/" + it.chapterId + " repeated " + it.count + "x, type=" + it.mistakeType.name
    }.ifBlank { "No unresolved mistakes recorded." }
    return "7-day study: " + analytics.studyMinutes7d + " minutes. Overall accuracy: " + (analytics.accuracy * 100).toInt() + "%. Tests: " + analytics.testsCompleted + ". Unresolved mistakes: " + analytics.unresolvedMistakes + ". Repeated mistakes: " + analytics.repeatedMistakes + ". Subjects: " + subjects + ". Recurring errors: " + errors + "."
}
