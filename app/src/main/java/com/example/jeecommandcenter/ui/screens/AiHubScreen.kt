package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiHubScreen(
    context: android.content.Context,
    onBack: () -> Unit,
    onOpenTutor: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val settings = remember { AiSettingsRepository(context) }
    val orchestrator = remember { AiOrchestrator(context) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var resultTitle by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf("") }
    val config = remember { settings.getConfig() }
    val hasKey = settings.hasApiKey()

    val pulse = rememberInfiniteTransition(label = "ai-hub-pulse").animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(1100, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "ai-icon-pulse"
    )

    fun runAction(title: String, action: suspend () -> AiResult) {
        if (!hasKey || busy) return
        busy = true
        resultTitle = title
        result = ""
        scope.launch {
            val response = action()
            result = if (response.success) response.text else (response.error ?: "AI request failed.")
            busy = false
        }
    }

    Scaffold(
        containerColor = BgApp,
        topBar = { com.example.jeecommandcenter.ui.theme.JeeTopBar(title = "AI Hub", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = BgCardAlt), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(52.dp).graphicsLayer(scaleX = pulse.value, scaleY = pulse.value)
                                .background(AccentBlue, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Study intelligence", style = MaterialTheme.typography.titleLarge)
                            Text("Analysis and planning from your real JeE data.", color = TextMuted, fontSize = 11.sp)
                            Spacer(Modifier.height(7.dp))
                            AssistChip(
                                onClick = onOpenSettings,
                                label = { Text(if (hasKey) config.provider.label + " · " + config.model else "Configure AI") },
                                leadingIcon = {
                                    Icon(if (hasKey) Icons.Filled.Insights else Icons.Filled.Settings, null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }
            item { Text("Quick analysis", style = MaterialTheme.typography.titleMedium) }
            item { AiActionCard("Analyze me", "Understand your preparation, strengths and risks.", Icons.Filled.Insights, hasKey && !busy) { runAction("Performance analysis") { orchestrator.analyzePerformance() } } }
            item { AiActionCard("Plan my day", "Turn today's deterministic priorities into a practical plan.", Icons.Filled.Today, hasKey && !busy) { runAction("Today's study plan") { orchestrator.buildDailyStudyPlan() } } }
            item { AiActionCard("Analyze mistakes", "Find recurring error patterns and the next practice action.", Icons.Filled.ErrorOutline, hasKey && !busy) { runAction("Mistake analysis") { orchestrator.explainMistakes() } } }
            item { AiActionCard("Chat with AI Tutor", "Ask questions, learn concepts and work through problems.", Icons.Filled.Chat, hasKey) { onOpenTutor() } }

            if (!hasKey) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = BgCard), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("AI isn't configured", style = MaterialTheme.typography.titleMedium)
                            Text("Your planner, revision and analytics still work locally without AI.", color = TextMuted, fontSize = 11.sp)
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = onOpenSettings) {
                                Icon(Icons.Filled.Settings, null)
                                Spacer(Modifier.width(7.dp))
                                Text("Configure AI in Settings")
                            }
                        }
                    }
                }
            }

            if (resultTitle != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = BgCard), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, null, tint = AccentPurple)
                                Spacer(Modifier.width(8.dp))
                                Text(resultTitle!!, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(10.dp))
                            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AccentPurple)
                            else Text(result, color = TextOnCard, fontSize = 13.sp)
                        }
                    }
                }
            }

            item { Text("AI enhances JeE's deterministic study engine; it does not replace your stored data or priorities.", color = TextMuted, fontSize = 10.sp) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AiActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = { Icon(icon, null, tint = if (enabled) AccentPurple else TextMuted, modifier = Modifier.size(24.dp)) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
