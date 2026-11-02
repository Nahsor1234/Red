package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import com.example.jeecommandcenter.ui.components.*
import java.time.format.DateTimeFormatter

@Composable
fun AnalyticsScreen(repo: JeeRepository, learning: LearningRepository, onBack: () -> Unit, onOpenTutor: () -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    val snapshot = remember(refresh) { learning.analytics(repo) }
    val mistakes = remember(refresh) { learning.mistakeStats() }
    val intelligence = remember { JeeIntelligence(repo, learning) }
    val weekly = remember(refresh) { intelligence.weeklyReview() }
    val hasWeeklyQuestionBaseline = remember(refresh) {
        val cutoff = System.currentTimeMillis() - 7L * 86_400_000L
        learning.getQuestionAttempts().any { it.createdAt >= cutoff }
    }
    val performance = remember(refresh) { PerformanceAnalytics(repo, learning) }
    val trend = remember(refresh) { performance.trend(14) }
    val evidence = remember(refresh) { performance.chapterEvidence(10) }
    val maxStudy = trend.maxOfOrNull { it.studyMinutes }?.coerceAtLeast(1) ?: 1

    Scaffold(containerColor = BgAppBase, topBar = {
        JeeTopBar(title = "Analytics", onBack = onBack, trailing = { IconButton(onClick = { refresh++ }) { Icon(Icons.Filled.Refresh, "Refresh") } })
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            item { AnalyticsHero(snapshot) }
            item { WeeklyReviewCard(weekly, hasWeeklyQuestionBaseline) }
            item {
                JeeCard {
                    SectionHeader("14-day activity")
                    Text("Study and question activity from stored records.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    trend.forEach { point ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(point.date.format(DateTimeFormatter.ofPattern("d MMM")), color = TextSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(48.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearStatBar(point.studyMinutes.toFloat() / maxStudy, height = 6.dp)
                                Text(
                                    point.studyMinutes.toString() + "m study · " + point.questions + " questions" + (point.accuracy?.let { " · " + (it * 100).toInt() + "%" } ?: ""),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (point.tests > 0) Text("${point.testScore?.toInt() ?: 0}% test", color = Success, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            item { SectionHeader("Subjects") }
            items(snapshot.subjectAnalytics) { subject -> SubjectBar(subject) }
            item { SectionHeader("Chapter evidence") }
            if (evidence.isEmpty()) item {
                JeeCard { Text("No chapter evidence yet", style = MaterialTheme.typography.titleMedium); Text("Chapter evidence appears after study, question, revision, or test activity is recorded.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium) }
            }
            else items(evidence) { ChapterEvidenceRow(it) }
            item { SectionHeader("Mistake patterns") }
            if (mistakes.isEmpty()) item {
                JeeCard { Text("No mistake patterns recorded", style = MaterialTheme.typography.titleMedium); Text("Saved mistake types will be summarized here.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium) }
            }
            else items(mistakes) { item ->
                JeeCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.type.name.replace('_', ' '), color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Text(item.count.toString(), color = PrimaryLight, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            item {
                Button(onClick = onOpenTutor, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), colors = ButtonDefaults.buttonColors(containerColor = FocusAction, contentColor = OnFocusAction)) {
                    Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(7.dp)); Text("Ask AI to analyze my performance")
                }
            }
        }
    }
}

@Composable
private fun AnalyticsHero(snapshot: AnalyticsSnapshot) {
    JeeCard(featured = true) {
        JeePageHeader(title = "Learning performance", subtitle = "A snapshot of recorded progress.")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric("7-day study", "${snapshot.studyMinutes7d}m", Modifier.weight(1f))
            Metric("Question accuracy", if (snapshot.totalQuestionsAttempted > 0) "${(snapshot.accuracy * 100).toInt()}%" else "—", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric("Tests completed", snapshot.testsCompleted.toString(), Modifier.weight(1f))
            Metric("Unresolved mistakes", snapshot.unresolvedMistakes.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric("Repeated mistakes", snapshot.repeatedMistakes.toString(), Modifier.weight(1f))
            Metric("Mastered chapters", snapshot.masteredChapters.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (snapshot.totalQuestionsAttempted == 0) "No question baseline · 0 questions attempted"
            else "${snapshot.totalQuestionsAttempted} questions attempted · ${snapshot.correctAnswers} correct",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            if (snapshot.testsCompleted == 0) "Average test score · no tests recorded"
            else "Average test score · ${snapshot.averageTestScore.toInt()}%",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun WeeklyReviewCard(review: WeeklyReview, hasQuestionBaseline: Boolean) {
    JeeCard {
        SectionHeader("Weekly review")
        Text("${review.studyMinutes}m study · ${review.sessions} sessions · ${review.revisions} revisions · ${review.tests} tests", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text(if (hasQuestionBaseline) "${(review.averageAccuracy * 100).toInt()}% recent accuracy" else "No recent question baseline", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        if (review.improvementFocus.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Suggested focus", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            review.improvementFocus.forEach { focus -> Text("•  $focus", color = TextSecondary, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun ChapterEvidenceRow(evidence: ChapterEvidence) {
    JeeCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(evidence.chapterName, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                Text(evidence.subject, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Text(evidence.accuracy?.let { (it * 100).toInt().toString() + "%" } ?: "No baseline", color = if ((evidence.accuracy ?: 1f) < 0.65f) TextAmber else TextSecondary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        Text("${evidence.studyMinutes}m study · ${evidence.questions} questions · ${evidence.revisions} revisions · ${evidence.unresolvedMistakes} unresolved mistakes", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    JeeCard(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SubjectBar(subject: SubjectAnalytics) {
    JeeCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(subject.subject, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(if (subject.attempted > 0) "${subject.attempted} attempted · ${(subject.accuracy * 100).toInt()}%" else "No questions attempted", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        if (subject.attempted > 0) {
            Spacer(Modifier.height(8.dp))
            LinearStatBar(subject.accuracy, height = 6.dp)
        }
    }
}
