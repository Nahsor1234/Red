package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.TestAttemptRecord
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun AssessmentResultScreen(attempt: TestAttemptRecord, onBack: () -> Unit) {
    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "Test result", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JeeCard(featured = true) {
                Icon(Icons.Filled.Assessment, null, tint = PrimaryLight, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(8.dp))
                Text(if (attempt.name.isBlank()) attempt.mode.name.lowercase().replace('_', ' ') else attempt.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp)); Text("${attempt.score} / ${attempt.totalMarks}", style = MaterialTheme.typography.displaySmall, color = PrimaryLight); Spacer(Modifier.height(5.dp))
                Text("${attempt.correctCount} correct · ${attempt.incorrectCount} incorrect · ${attempt.skippedCount} skipped", color = TextSecondary, fontSize = 12.sp)
                Text("Attempted: ${attempt.attemptedCount} / ${attempt.questionCount}", color = TextSecondary, fontSize = 12.sp); Text("Duration: ${formatTestDuration(attempt.durationSec)}", color = TextMuted, fontSize = 11.sp)
            }
            JeeCard {
                Text("Subject breakdown", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
                if (attempt.subjectBreakdown.isEmpty()) Text("No subject breakdown recorded for this attempt.", color = TextMuted, fontSize = 12.sp)
                else attempt.subjectBreakdown.forEach { (subject, pair) -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(subject, color = TextPrimary); Text("${pair.first} / ${pair.second} correct", color = TextSecondary, fontSize = 12.sp) } }
            }
        }
    }
}

private fun formatTestDuration(seconds: Int): String { val minutes = seconds / 60; val remaining = seconds % 60; return if (minutes > 0) "${minutes}m ${remaining}s" else "${remaining}s" }
