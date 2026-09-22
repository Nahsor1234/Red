package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun RevisionScreen(
    repo: JeeRepository,
    onBack: () -> Unit,
    onOpenPlanner: () -> Unit
) {
    var refresh by remember { mutableIntStateOf(0) }
    val queue = remember(refresh) { repo.getRevisionQueue() }
    val dueCount = queue.size

    Scaffold(
        containerColor = BgApp,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Revision engine", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (dueCount == 0) "Nothing due right now" else dueCount.toString() + " reviews due",
                        color = if (dueCount == 0) AccentGreen else TextMuted,
                        fontSize = 10.sp
                    )
                }
                Icon(
                    Icons.Filled.Replay,
                    "Refresh",
                    tint = TextSecondary,
                    modifier = Modifier.premiumClick { refresh++ }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RevisionMetric("Due", dueCount.toString(), Modifier.weight(1f))
                    RevisionMetric(
                        "Mastery",
                        repo.getMasteredChapterCount().toString(),
                        Modifier.weight(1f)
                    )
                    RevisionMetric(
                        "Active",
                        repo.getActiveRevisionCount().toString(),
                        Modifier.weight(1f)
                    )
                }
            }

            if (queue.isEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgCard)
                            .padding(18.dp)
                    ) {
                        Text("Revision queue is clear", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Study a chapter in Syllabus and it will automatically enter spaced revision.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onOpenPlanner) {
                            Icon(Icons.Filled.Timer, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open planner")
                        }
                    }
                }
            } else {
                items(queue, key = { it.revision.id }) { item ->
                    RevisionCard(
                        item = item,
                        onRate = { rating ->
                            repo.reviewChapter(item.chapter.id, rating)
                            refresh++
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun RevisionMetric(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .padding(12.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun RevisionCard(
    item: RevisionQueueItem,
    onRate: (RevisionRating) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(item.chapter.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    item.chapter.subject + " · " + (item.progress * 100).toInt() + "% mastered",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            Text(
                "C" + item.chapter.number,
                color = AccentBlueLight,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("How did this feel?", color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ReviewButton("Again", RevisionRating.AGAIN, onRate, Modifier.weight(1f))
            ReviewButton("Hard", RevisionRating.HARD, onRate, Modifier.weight(1f))
            ReviewButton("Good", RevisionRating.GOOD, onRate, Modifier.weight(1f))
            ReviewButton("Easy", RevisionRating.EASY, onRate, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Confidence: " + item.confidence + "/5",
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ReviewButton(
    label: String,
    rating: RevisionRating,
    onRate: (RevisionRating) -> Unit,
    modifier: Modifier
) {
    OutlinedButton(
        onClick = { onRate(rating) },
        modifier = modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, fontSize = 10.sp)
    }
}
