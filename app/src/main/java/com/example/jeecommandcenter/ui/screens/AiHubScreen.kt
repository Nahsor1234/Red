package com.example.jeecommandcenter.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiHubScreen(
    context: android.content.Context,
    onBack: () -> Unit,
    onOpenTutor: () -> Unit
) {
    val settings = remember { AiSettingsRepository(context) }
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(settings.getConfig()) }
    var key by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(config.model) }
    var endpoint by remember { mutableStateOf(config.endpoint) }
    var providerExpanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val needsCustomEndpoint =
        config.provider == AiProvider.CUSTOM_OPENAI_COMPATIBLE

    fun saveConfiguration(): Boolean {
        if (model.isBlank()) {
            status = "Enter a model ID."
            return false
        }
        if (needsCustomEndpoint && !endpoint.trim().startsWith("https://")) {
            status = "Enter a valid HTTPS API endpoint."
            return false
        }

        settings.saveConfig(
            provider = config.provider,
            model = model,
            endpoint = endpoint
        )
        config = settings.getConfig()
        status = "AI configuration saved."
        return true
    }

    Scaffold(
        containerColor = BgApp,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("AI Hub", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(48.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HubSection(
                    title = "Provider",
                    subtitle = "Choose the AI service JeE should call."
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { providerExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(config.provider.label)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.ExpandMore, null)
                        }

                        DropdownMenu(
                            expanded = providerExpanded,
                            onDismissRequest = { providerExpanded = false }
                        ) {
                            AiProvider.entries.forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.label) },
                                    onClick = {
                                        val newModel = settings.defaultModel(provider)
                                        val newEndpoint = settings.defaultEndpoint(provider)

                                        config = AiConfig(provider, newModel, newEndpoint)
                                        model = newModel
                                        endpoint = newEndpoint
                                        settings.saveConfig(
                                            provider,
                                            newModel,
                                            newEndpoint
                                        )
                                        providerExpanded = false
                                        status = "Provider changed to " +
                                            provider.label +
                                            "."
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                HubSection(
                    title = "Model",
                    subtitle = "Type any model ID supported by the selected provider."
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Model ID") },
                        placeholder = { Text("e.g. provider/model-name") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "JeE no longer limits you to a fixed model list.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { saveConfiguration() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save model settings")
                    }
                }
            }

            if (needsCustomEndpoint) {
                item {
                    HubSection(
                        title = "API endpoint",
                        subtitle = "For a custom OpenAI-compatible server."
                    ) {
                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = { endpoint = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("HTTPS endpoint") },
                            placeholder = {
                                Text("https://your-host/v1/chat/completions")
                            },
                            singleLine = true
                        )
                    }
                }
            }

            item {
                HubSection(
                    title = "API key",
                    subtitle = "Stored locally using Android Keystore encryption."
                ) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(config.provider.label + " API key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (key.isNotBlank()) {
                                    runCatching {
                                        settings.saveApiKey(key)
                                        settings.saveConfig(
                                            config.provider,
                                            model,
                                            endpoint
                                        )
                                    }.onSuccess {
                                        key = ""
                                        status = "API key saved securely on this device."
                                    }.onFailure { error ->
                                        status =
                                            "Could not save API key: " +
                                                (error.message ?: "secure storage failed")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue
                            )
                        ) {
                            Text("Save key")
                        }

                        OutlinedButton(
                            onClick = {
                                settings.clearApiKey()
                                key = ""
                                status = "API key removed."
                            },
                            enabled = settings.hasApiKey()
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (settings.hasApiKey()) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.CloudOff
                        },
                        null,
                        tint = if (settings.hasApiKey()) AccentGreen else TextMuted
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (settings.hasApiKey()) {
                            "Provider key configured"
                        } else {
                            "No API key configured"
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (!saveConfiguration()) return@Button
                        busy = true
                        status = null

                        scope.launch {
                            val result = AiEngine(settings).ask(
                                "Reply with exactly: JeE AI connection OK"
                            )
                            busy = false
                            status = if (result.success) {
                                result.text.trim()
                            } else {
                                "Connection failed: " + result.error
                            }
                        }
                    },
                    enabled = settings.hasApiKey() &&
                        !busy &&
                        model.isNotBlank() &&
                        (!needsCustomEndpoint ||
                            endpoint.trim().startsWith("https://")),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPurple
                    )
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = TextPrimary
                        )
                    } else {
                        Icon(
                            Icons.Filled.NetworkCheck,
                            null,
                            Modifier.size(17.dp)
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(if (busy) "Testing..." else "Test connection")
                }
            }

            status?.let { message ->
                item {
                    Text(
                        message,
                        color = if (
                            message.contains("failed", true) ||
                            message.contains("could not", true) ||
                            message.contains("valid", true)
                        ) {
                            AccentAmber
                        } else {
                            AccentGreen
                        },
                        fontSize = 12.sp
                    )
                }
            }

            item {
                Button(
                    onClick = onOpenTutor,
                    enabled = settings.hasApiKey(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BgCardAlt
                    )
                ) {
                    Icon(Icons.Filled.School, null)
                    Spacer(Modifier.width(7.dp))
                    Text("Open AI Tutor")
                }
            }

            item {
                Text(
                    "API usage and billing are controlled by the selected provider. Keep your key private.",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HubSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = TextMuted, fontSize = 10.sp)
        Spacer(Modifier.height(10.dp))
        content()
    }
}
