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

    Scaffold(
        containerColor = BgApp,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Analytics", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { refresh++ }) { Icon(Icons.Filled.Refresh, "Refresh") }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                AnalyticsHero(snapshot)
            }
            item {
                Text("Subjects", style = MaterialTheme.typography.titleMedium)
            }
            items(snapshot.subjectAnalytics) { subject ->
                SubjectBar(subject)
            }
            item {
                Text("Mistake pattern", style = MaterialTheme.typography.titleMedium)
            }
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
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCardAlt).padding(16.dp)
    ) {
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
        Text(
            "Average test score: " + snapshot.averageTestScore.toInt() + "%",
            color = TextSecondary,
            fontSize = 12.sp
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
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BgCard).padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(subject.subject, color = TextSecondary, fontSize = 12.sp)
            Text(
                subject.attempted.toString() + " attempted · " + (subject.accuracy * 100).toInt() + "%",
                color = TextMuted,
                fontSize = 10.sp
            )
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
