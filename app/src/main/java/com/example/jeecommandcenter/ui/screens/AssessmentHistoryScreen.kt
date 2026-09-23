package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
fun AssessmentHistoryScreen(learning: LearningRepository, onBack: () -> Unit) {
    val attempts = remember { learning.getTestAttempts().sortedByDescending { it.completedAt } }
    var selected by remember { mutableStateOf<TestAttemptRecord?>(null) }

    Scaffold(
        containerColor = BgApp,
        topBar = { JeeTopBar(title = "Test history", onBack = onBack) }
    ) { padding ->
        if (attempts.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.Center) {
                JeeCard {
                    Icon(Icons.Filled.Assessment, null, tint = PrimaryLight, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("No tests recorded yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(5.dp))
                    Text("Completed practice tests, mocks and AI quizzes will appear here.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(attempts, key = { it.id }) { attempt ->
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgCard)
                            .border(1.dp, BgCardBorder.copy(alpha = .8f), RoundedCornerShape(16.dp))
                            .premiumClick { selected = attempt }
                            .padding(14.dp)
                    ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (attempt.name.isBlank()) attempt.mode.name.lowercase().replace('_', ' ') else attempt.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(attempt.completedAt)),
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                                Text(attempt.score.toString() + "/" + attempt.totalMarks, color = PrimaryLight, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(
                                attempt.correctCount.toString() + " correct · " + attempt.incorrectCount + " incorrect · " + attempt.skippedCount + " skipped",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(formatTestDuration(attempt.durationSec) + " · " + attempt.questionCount + " questions", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    selected?.let { attempt ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Test result") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(attempt.mode.name.lowercase().replace('_', ' '), color = PrimaryLight, fontSize = 11.sp)
                    Text(attempt.score.toString() + " / " + attempt.totalMarks, style = MaterialTheme.typography.headlineSmall)
                    Text(attempt.correctCount.toString() + " correct · " + attempt.incorrectCount + " incorrect · " + attempt.skippedCount + " skipped", color = TextSecondary, fontSize = 12.sp)
                    Text("Attempted: " + attempt.attemptedCount + " / " + attempt.questionCount, color = TextSecondary, fontSize = 12.sp)
                    Text("Duration: " + formatTestDuration(attempt.durationSec), color = TextSecondary, fontSize = 12.sp)
                    if (attempt.subjectBreakdown.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Subject breakdown", color = TextPrimary, fontWeight = FontWeight.Medium)
                        attempt.subjectBreakdown.forEach { (subject, pair) ->
                            Text(subject + " · " + pair.first + "/" + pair.second + " correct", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }
}

private fun formatTestDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return if (minutes > 0) minutes.toString() + "m " + remaining.toString() + "s" else remaining.toString() + "s"
}
