package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun MistakeBankScreen(
    learning: LearningRepository,
    onBack: () -> Unit,
    onOpenTutor: () -> Unit
) {
    var refresh by remember { mutableIntStateOf(0) }
    val mistakes = remember(refresh) { learning.getMistakes().filterNot { it.resolved } }

    Scaffold(
        containerColor = BgApp,
        topBar = {
            JeeTopBar(
                title = "Mistake bank",
                onBack = onBack,
                trailing = { Text(mistakes.size.toString(), color = AccentAmber) }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (mistakes.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).padding(18.dp)
                    ) {
                        Text("No unresolved mistakes", style = MaterialTheme.typography.titleMedium)
                        Text("Incorrect test answers are added here automatically.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                items(mistakes, key = { it.id }) { mistake ->
                    MistakeCard(mistake, learning) { refresh++ }
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
                    Text("Ask AI about my mistakes")
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MistakeCard(
    mistake: MistakeRecord,
    learning: LearningRepository,
    onRefresh: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(mistake.subject + " · " + mistake.chapterId, color = AccentBlueLight, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(mistake.questionPrompt, style = MaterialTheme.typography.titleMedium)
            }
            Text("×" + mistake.count, color = if (mistake.count >= 2) AccentAmber else TextMuted)
        }
        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedButton(
                onClick = { typeMenu = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Type: " + mistake.mistakeType.name.replace('_', ' '), fontSize = 10.sp)
            }
            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                MistakeType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name.replace('_', ' '), fontSize = 11.sp) },
                        onClick = {
                            learning.updateMistakeType(mistake.id, type)
                            typeMenu = false
                            onRefresh()
                        }
                    )
                }
            }
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text("Your last answer: " + mistake.lastSelectedAnswer, color = TextSecondary, fontSize = 11.sp)
            Text("Correct: " + mistake.correctAnswer, color = AccentGreen, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.weight(1f)) {
                Text(if (expanded) "Hide" else "Details")
            }
            Button(
                onClick = { learning.resolveMistake(mistake.id); onRefresh() },
                modifier = Modifier.weight(1f)
            ) { Text("Resolve") }
        }
    }
}
