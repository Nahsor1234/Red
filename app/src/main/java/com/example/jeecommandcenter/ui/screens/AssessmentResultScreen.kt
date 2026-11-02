package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.data.TestAttemptRecord
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssessmentResultScreen(attempt: TestAttemptRecord, onBack: () -> Unit) {
    Scaffold(containerColor = BgAppBase, topBar = { JeeTopBar(title = "Test result", onBack = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            item {
                JeePageHeader(
                    title = if (attempt.name.isBlank()) attempt.mode.name.lowercase().replace('_', ' ') else attempt.name,
                    subtitle = "Completed ${SimpleDateFormat("EEE, d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(attempt.completedAt))}",
                    eyebrow = "SAVED ASSESSMENT"
                )
            }
            item {
                JeeCard(featured = true) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Filled.Assessment, null, tint = PrimaryLight, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("Recorded score", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                        Text("${attempt.score}", style = MaterialTheme.typography.displaySmall, color = PrimaryLight, fontWeight = FontWeight.SemiBold)
                        Text(" / ${attempt.totalMarks} marks", modifier = Modifier.padding(start = 6.dp, bottom = 8.dp), color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider(color = BgDivider)
                    Spacer(Modifier.height(10.dp))
                    Text("${attempt.correctCount} correct · ${attempt.incorrectCount} incorrect · ${attempt.skippedCount} skipped", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                    Text("${attempt.attemptedCount} attempted of ${attempt.questionCount} questions", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("Duration · ${formatTestDuration(attempt.durationSec)}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                JeeCard {
                    SectionHeader("Subject breakdown")
                    if (attempt.subjectBreakdown.isEmpty()) {
                        Text("No subject breakdown was recorded for this attempt.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        attempt.subjectBreakdown.entries.forEachIndexed { index, (subject, pair) ->
                            if (index > 0) HorizontalDivider(color = BgDivider)
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(subject, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Text("${pair.first} correct · ${pair.second} attempted", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTestDuration(seconds: Int): String { val minutes = seconds / 60; val remaining = seconds % 60; return if (minutes > 0) "${minutes}m ${remaining}s" else "${remaining}s" }
