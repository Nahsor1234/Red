package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun StudyPlannerScreen(
    repo: JeeRepository,
    onBack: () -> Unit,
    onOpenRevision: () -> Unit
) {
    var refresh by remember { mutableIntStateOf(0) }
    var generating by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val learning = remember { LearningRepository(context) }
    val plan = remember(refresh) { repo.getOrCreateDailyPlan(learning) }
    val completed = plan.items.count { it.completed }
    val doneMinutes = plan.items.filter { it.completed }.sumOf { it.durationMin }
    val remainingMinutes = (plan.goalMinutes - doneMinutes).coerceAtLeast(0)
    val progress = if (plan.goalMinutes == 0) 0f else
        (doneMinutes.toFloat() / plan.goalMinutes).coerceIn(0f, 1f)

    Scaffold(
        containerColor = BgApp,
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("Back")
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text("Study planner", style = MaterialTheme.typography.titleLarge)
                    Text("Generated from your actual study state", color = TextMuted, fontSize = 10.sp)
                }
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
                PlannerSummary(
                    goalMinutes = plan.goalMinutes,
                    doneMinutes = doneMinutes,
                    remainingMinutes = remainingMinutes,
                    completed = completed,
                    total = plan.items.size,
                    progress = progress
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            repo.regenerateDailyPlan(learning)
                            refresh++
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Regenerate")
                    }
                    OutlinedButton(
                        onClick = onOpenRevision,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Replay, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Revision")
                    }
                }
            }

            if (plan.items.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No plan could be generated",
                        body = "Add study time to your daily goal or mark a few chapters as started so the planner has work to schedule."
                    )
                }
            } else {
                items(plan.items, key = { it.id }) { item ->
                    PlannerItemCard(item) {
                        repo.completePlannerItem(item.id)
                        refresh++
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PlannerSummary(
    goalMinutes: Int,
    doneMinutes: Int,
    remainingMinutes: Int,
    completed: Int,
    total: Int,
    progress: Float
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BgCardAlt)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Column {
                Text("Today's plan", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    doneMinutes.toString() + " / " + goalMinutes + " min",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            Text(
                (progress * 100).toInt().toString() + "%",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearStatBar(progress)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(completed.toString() + " / " + total + " blocks complete", color = TextMuted, fontSize = 11.sp)
            Text(
                if (remainingMinutes > 0) remainingMinutes.toString() + " min left" else "Goal complete",
                color = if (remainingMinutes > 0) TextMuted else AccentGreen,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PlannerItemCard(item: PlannerItem, onComplete: () -> Unit) {
    val typeLabel = when (item.type) {
        PlannerItemType.REVISION -> "REVISION"
        PlannerItemType.STUDY -> "STUDY"
        PlannerItemType.PRACTICE -> "PRACTICE"
    }
    val typeColor = when (item.type) {
        PlannerItemType.REVISION -> AccentAmber
        PlannerItemType.STUDY -> AccentBlueLight
        PlannerItemType.PRACTICE -> AccentGreen
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(typeColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(typeLabel.take(1), color = typeColor, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = if (item.completed) TextMuted else TextOnCard,
                fontSize = 13.sp
            )
            Text(
                item.subject + " · " + item.durationMin + "m",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
        IconButton(
            onClick = onComplete,
            enabled = !item.completed
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                "Complete",
                tint = if (item.completed) AccentGreen else TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(18.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(body, color = TextMuted, fontSize = 12.sp)
    }
}
