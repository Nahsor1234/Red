package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.AiOrchestrator
import com.example.jeecommandcenter.data.AiResult
import com.example.jeecommandcenter.data.AiSettingsRepository
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeePageHeader
import com.example.jeecommandcenter.ui.components.SectionHeader
import com.example.jeecommandcenter.ui.theme.AccentBlue
import com.example.jeecommandcenter.ui.theme.BgApp
import com.example.jeecommandcenter.ui.theme.Primary
import com.example.jeecommandcenter.ui.theme.TextMuted
import com.example.jeecommandcenter.ui.theme.TextSecondary
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
    val view = LocalView.current
    var busy by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf("") }
    val config = settings.getConfig()
    val hasKey = settings.hasApiKey()

    fun run(actionTitle: String, action: suspend () -> AiResult) {
        if (!hasKey || busy) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
        busy = true
        title = actionTitle
        result = ""
        scope.launch {
            val response = action()
            result = if (response.success) response.text else (response.error ?: "AI request failed.")
            busy = false
        }
    }

    Scaffold(containerColor = BgApp) { insets ->
        LazyColumn(
            Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                JeePageHeader(
                    title = "AI study coach",
                    subtitle = "A provider-powered layer over your recorded preparation.",
                    eyebrow = "OPTIONAL · AI",
                    trailing = {
                        OutlinedButton(onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                            onOpenSettings()
                        }) {
                            Icon(Icons.Filled.Settings, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Settings")
                        }
                    }
                )
            }
            item {
                JeeCard(featured = true, modifier = Modifier.animateContentSize()) {
                    Text("READINESS", style = MaterialTheme.typography.labelMedium, color = TextSecondary, letterSpacing = 1.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        if (hasKey) "Ready when you are" else "AI is optional",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (hasKey) "${config.provider.label} · ${config.model}"
                        else "Add a provider and API key in Settings to use the coach. Your local study tools remain available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (!hasKey) {
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Configure AI")
                        }
                    }
                }
            }
            item { SectionHeader("Use your study data") }
            item {
                Text(
                    "Each action asks the configured provider to analyze information already recorded in JeE.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            item {
                AiAction("Analyze my preparation", "Look for real strengths, gaps and recent patterns.", Icons.Filled.Insights, hasKey && !busy) {
                    run("Preparation analysis") { orchestrator.analyzePerformance() }
                }
            }
            item {
                AiAction("What should I study now?", "Get a next-step suggestion based on your data.", Icons.Filled.Today, hasKey && !busy) {
                    run("Today's study plan") { orchestrator.buildDailyStudyPlan() }
                }
            }
            item {
                AiAction("Analyze my mistakes", "Review recorded mistakes and recurring chapters.", Icons.Filled.ErrorOutline, hasKey && !busy) {
                    run("Mistake analysis") { orchestrator.explainMistakes() }
                }
            }
            item {
                SectionHeader("Talk it through")
                JeeCard {
                    Text("Ask the study coach", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Open a conversation about your preparation. Existing study context is used by the coach; conversations require a configured provider.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                            onOpenTutor()
                        },
                        enabled = hasKey,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 2.dp))
                        Text(if (hasKey) "Open tutor" else "Configure AI to continue")
                    }
                }
            }
            item {
                AnimatedVisibility(visible = title != null, enter = fadeIn(), exit = fadeOut()) {
                    JeeCard(modifier = Modifier.animateContentSize()) {
                        Text(title.orEmpty(), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (busy) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentBlue)
                                Text("Waiting for the configured provider…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        } else {
                            Text(result, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun AiAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    JeeCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (enabled) Primary else TextMuted)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            androidx.compose.material3.TextButton(onClick = onClick, enabled = enabled) { Text("Run") }
        }
    }
}
