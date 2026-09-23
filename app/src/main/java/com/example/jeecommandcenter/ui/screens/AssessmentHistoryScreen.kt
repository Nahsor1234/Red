package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.LearningRepository
import com.example.jeecommandcenter.data.TestAttemptRecord
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssessmentHistoryScreen(learning: LearningRepository, onBack: () -> Unit, onOpenAttempt: (TestAttemptRecord) -> Unit = {}) {
    val attempts = remember { learning.getTestAttempts().sortedByDescending { it.completedAt } }
    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "Test history", onBack = onBack) }) { padding ->
        if (attempts.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.Center) {
                JeeCard { Icon(Icons.Filled.Assessment, null, tint = PrimaryLight, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(10.dp)); Text("No tests recorded yet", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(5.dp)); Text("Completed practice tests, mocks and AI quizzes will appear here.", color = TextSecondary, fontSize = 12.sp) }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(attempts, key = { it.id }) { attempt ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).border(1.dp, BgCardBorder.copy(alpha = .8f), RoundedCornerShape(16.dp)).premiumClick { onOpenAttempt(attempt) }.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(if (attempt.name.isBlank()) attempt.mode.name.lowercase().replace('_', ' ') else attempt.name, style = MaterialTheme.typography.titleMedium); Text(SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(attempt.completedAt)), color = TextMuted, fontSize = 9.sp) }
                            Text("${attempt.score}/${attempt.totalMarks}", color = PrimaryLight, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(7.dp)); Text("${attempt.correctCount} correct · ${attempt.incorrectCount} incorrect · ${attempt.skippedCount} skipped", color = TextSecondary, fontSize = 11.sp); Text("${formatTestDuration(attempt.durationSec)} · ${attempt.questionCount} questions", color = TextMuted, fontSize = 10.sp); Spacer(Modifier.height(5.dp)); Text("View full result →", color = PrimaryLight, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun formatTestDuration(seconds: Int): String { val minutes = seconds / 60; val remaining = seconds % 60; return if (minutes > 0) "${minutes}m ${remaining}s" else "${remaining}s" }
