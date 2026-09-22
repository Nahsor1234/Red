package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import com.example.jeecommandcenter.ui.components.*
import java.time.format.DateTimeFormatter

@Composable
fun AnalyticsScreen(
    repo: JeeRepository,
    learning: LearningRepository,
    onBack: () -> Unit,
    onOpenTutor: () -> Unit
) {
    var refresh by remember { mutableIntStateOf(0) }
    val snapshot = remember(refresh) { learning.analytics(repo) }
    val mistakes = remember(refresh) { learning.mistakeStats() }
    val intelligence = remember { JeeIntelligence(repo, learning) }
    val weekly = remember(refresh) { intelligence.weeklyReview() }
    val performance = remember(refresh) { PerformanceAnalytics(repo, learning) }
    val trend = remember(refresh) { performance.trend(14) }
    val evidence = remember(refresh) { performance.chapterEvidence(10) }
    val maxStudy = trend.maxOfOrNull { it.studyMinutes }?.coerceAtLeast(1) ?: 1

    Scaffold(
        containerColor = BgApp,
        topBar = {
            JeeTopBar(
                title = "Analytics",
                onBack = onBack,
                trailing = {
                    IconButton(onClick = { refresh++ }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { AnalyticsHero(snapshot) }
            item { WeeklyReviewCard(weekly) }
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).padding(14.dp)
                ) {
                    Text("14-day performance", style = MaterialTheme.typography.titleMedium)
                    Text("Study and question activity from actual stored records", color = TextMuted, fontSize = 10.sp)
                    Spacer(Modifier.height(10.dp))
                    trend.forEach { point ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                point.date.format(DateTimeFormatter.ofPattern("dd MMM")),
                                color = TextMuted,
                                fontSize = 9.sp,
                                modifier = Modifier.width(42.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                LinearProgressIndicator(
                                    progress = { point.studyMinutes.toFloat() / maxStudy },
                                    modifier = Modifier.fillMaxWidth().height(5.dp),
                                    color = AccentBlue,
                                    trackColor = BgDivider
                                )
                                Text(
                                    point.studyMinutes.toString() + "m study · " +
                                        point.questions + " questions" +
                                        (point.accuracy?.let { " · " + (it * 100).toInt() + "%" } ?: ""),
                                    color = TextMuted,
                                    fontSize = 8.sp
                                )
                            }
                            if (point.tests > 0) {
                                Text(
                                    "T " + (point.testScore?.toInt() ?: 0) + "%",
                                    color = AccentGreen,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            item { Text("Subjects", style = MaterialTheme.typography.titleMedium) }
            items(snapshot.subjectAnalytics) { subject -> SubjectBar(subject) }
            item { Text("Chapter evidence", style = MaterialTheme.typography.titleMedium) }
            if (evidence.isEmpty()) {
                item {
                    Text(
                        "Chapter evidence will appear after you study, answer questions, revise, or take tests.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            } else {
                items(evidence) { ChapterEvidenceRow(it) }
            }
            item { Text("Mistake pattern", style = MaterialTheme.typography.titleMedium) }
            items(mistakes) { item ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BgCard).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.type.name.replace('_', ' '), color = TextSecondary, fontSize = 12.sp)
                    Text(item.count.toString(), style = MaterialTheme.typography.titleMedium)
                }
            }
            item {
                Button(
                    onClick = onOpenTutor,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null)
                    Spacer(Modifier.width(7.dp))
                    Text("Ask AI to analyze my performance")
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AnalyticsHero(snapshot: AnalyticsSnapshot) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCardAlt).padding(16.dp)) {
        Text("Learning performance", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("7d study", snapshot.studyMinutes7d.toString() + "m", Modifier.weight(1f))
            Metric("Accuracy", (snapshot.accuracy * 100).toInt().toString() + "%", Modifier.weight(1f))
            Metric("Tests", snapshot.testsCompleted.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Mistakes", snapshot.unresolvedMistakes.toString(), Modifier.weight(1f))
            Metric("Repeated", snapshot.repeatedMistakes.toString(), Modifier.weight(1f))
            Metric("Mastered", snapshot.masteredChapters.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text("Average test score: " + snapshot.averageTestScore.toInt() + "%", color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun WeeklyReviewCard(review: WeeklyReview) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).padding(14.dp)) {
        Text("Weekly review", style = MaterialTheme.typography.titleMedium)
        Text(
            review.studyMinutes.toString() + "m study · " + review.sessions + " sessions · " +
                review.revisions + " revisions · " + review.tests + " tests",
            color = TextSecondary,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (review.averageAccuracy == 0f) "No recent question baseline"
            else (review.averageAccuracy * 100).toInt().toString() + "% recent accuracy",
            color = TextMuted,
            fontSize = 11.sp
        )
        if (review.improvementFocus.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("Focus: " + review.improvementFocus.joinToString(" · "), color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ChapterEvidenceRow(evidence: ChapterEvidence) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BgCard).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(evidence.chapterName, color = TextOnCard, fontSize = 12.sp)
                Text(evidence.subject, color = TextMuted, fontSize = 9.sp)
            }
            Text(
                evidence.accuracy?.let { (it * 100).toInt().toString() + "%" } ?: "—",
                color = if ((evidence.accuracy ?: 1f) < 0.65f) TextAmber else TextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            evidence.studyMinutes.toString() + "m study · " + evidence.questions + " questions · " +
                evidence.revisions + " revisions · " + evidence.unresolvedMistakes + " unresolved mistakes",
            color = TextMuted,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(BgCard).padding(10.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, color = TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun SubjectBar(subject: SubjectAnalytics) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BgCard).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(subject.subject, color = TextSecondary, fontSize = 12.sp)
            Text(subject.attempted.toString() + " attempted · " + (subject.accuracy * 100).toInt() + "%", color = TextMuted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(
            progress = { subject.accuracy.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = AccentBlue,
            trackColor = BgDivider
        )
    }
}
