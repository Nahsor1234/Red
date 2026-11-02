package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
fun AssessmentHistoryScreen(learning: LearningRepository, onBack: () -> Unit, onOpenAttempt: (TestAttemptRecord) -> Unit = {}) {
    val attempts = remember { learning.getTestAttempts().sortedByDescending { it.completedAt } }
    Scaffold(containerColor = BgAppBase, topBar = { JeeTopBar(title = "Test history", onBack = onBack) }) { padding ->
        if (attempts.isEmpty()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
            ) {
                item { JeePageHeader(title = "Test history", subtitle = "Stored results from completed assessments.", eyebrow = "ASSESSMENT") }
                item {
                    JeeCard(featured = true) {
                        Icon(Icons.Filled.Assessment, null, tint = PrimaryLight, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No saved attempts yet", style = MaterialTheme.typography.titleLarge)
                        Text("Completed practice tests, mocks and AI quizzes will appear here.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
            ) {
                item {
                    JeePageHeader(
                        title = "Test history",
                        subtitle = "Open an attempt to review its stored result.",
                        eyebrow = "ASSESSMENT"
                    )
                }
                item {
                    Column {
                        SectionHeader("Completed attempts")
                        Text("${attempts.size} saved", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(attempts, key = { it.id }) { attempt ->
                    JeeCard(modifier = Modifier.clickable(onClick = { onOpenAttempt(attempt) })) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    if (attempt.name.isBlank()) attempt.mode.name.lowercase().replace('_', ' ') else attempt.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(SimpleDateFormat("EEE, d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(attempt.completedAt)), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Text("${attempt.score}", color = PrimaryLight, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                                Text("of ${attempt.totalMarks} marks", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = BgDivider)
                        Spacer(Modifier.height(9.dp))
                        Text("${attempt.correctCount} correct · ${attempt.incorrectCount} incorrect · ${attempt.skippedCount} skipped", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Text("${attempt.questionCount} questions · ${formatTestDuration(attempt.durationSec)}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text("View saved result  →", color = PrimaryLight, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

private fun formatTestDuration(seconds: Int): String { val minutes = seconds / 60; val remaining = seconds % 60; return if (minutes > 0) "${minutes}m ${remaining}s" else "${remaining}s" }
