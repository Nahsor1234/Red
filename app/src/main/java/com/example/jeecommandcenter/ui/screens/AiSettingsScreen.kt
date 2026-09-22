package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiSettingsScreen(context: android.content.Context, onBack: () -> Unit) {
    val settings = remember { AiSettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(settings.getConfig()) }
    var key by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(config.model) }
    var endpoint by remember { mutableStateOf(config.endpoint) }
    var expanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val customEndpoint = config.provider == AiProvider.CUSTOM_OPENAI_COMPATIBLE

    fun save(): Boolean {
        if (model.isBlank()) { status = "Enter a model ID."; return false }
        if (customEndpoint && !endpoint.trim().startsWith("https://")) { status = "Enter a valid HTTPS API endpoint."; return false }
        settings.saveConfig(config.provider, model, endpoint)
        config = settings.getConfig()
        status = "AI configuration saved."
        return true
    }

    Scaffold(
        containerColor = BgApp,
        topBar = { com.example.jeecommandcenter.ui.theme.JeeTopBar(title = "AI configuration", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Provider", style = MaterialTheme.typography.titleLarge)
                        Text("Choose the AI service JeE should call.", color = TextMuted, fontSize = 11.sp)
                        Spacer(Modifier.height(10.dp))
                        Box {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(config.provider.label)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Filled.ExpandMore, null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                AiProvider.entries.forEach { provider ->
                                    DropdownMenuItem(
                                        text = { Text(provider.label) },
                                        onClick = {
                                            val newModel = settings.defaultModel(provider)
                                            val newEndpoint = settings.defaultEndpoint(provider)
                                            config = AiConfig(provider, newModel, newEndpoint)
                                            model = newModel
                                            endpoint = newEndpoint
                                            settings.saveConfig(provider, newModel, newEndpoint)
                                            expanded = false
                                            status = "Provider changed to " + provider.label + "."
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Model", style = MaterialTheme.typography.titleLarge)
                        Text("Use any model ID supported by the selected provider.", color = TextMuted, fontSize = 11.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = model, onValueChange = { model = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Model ID") }, singleLine = true)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("Save model settings") }
                    }
                }
            }
            if (customEndpoint) {
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("API endpoint", style = MaterialTheme.typography.titleLarge)
                            Text("Required for a custom OpenAI-compatible server.", color = TextMuted, fontSize = 11.sp)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, modifier = Modifier.fillMaxWidth(), label = { Text("HTTPS endpoint") }, placeholder = { Text("https://your-host/v1/chat/completions") }, singleLine = true)
                        }
                    }
                }
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("API key", style = MaterialTheme.typography.titleLarge)
                        Text("Stored locally using Android Keystore encryption.", color = TextMuted, fontSize = 11.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = key, onValueChange = { key = it }, modifier = Modifier.fillMaxWidth(), label = { Text(config.provider.label + " API key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (key.isNotBlank()) runCatching {
                                    settings.saveApiKey(key)
                                    settings.saveConfig(config.provider, model, endpoint)
                                }.onSuccess {
                                    key = ""
                                    status = "API key saved securely on this device."
                                }.onFailure {
                                    status = "Could not save API key: " + (it.message ?: "secure storage failed")
                                }
                            }, modifier = Modifier.weight(1f)) { Text("Save key") }
                            OutlinedButton(onClick = {
                                settings.clearApiKey()
                                key = ""
                                status = "API key removed."
                            }, enabled = settings.hasApiKey(), modifier = Modifier.weight(1f)) { Text("Remove") }
                        }
                    }
                }
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(if (settings.hasApiKey()) "AI is ready" else "No API key configured") },
                        supportingContent = { Text(if (settings.hasApiKey()) "JeE can use the analysis and tutor actions." else "The AI action center will show configuration guidance.") },
                        leadingContent = {
                            Icon(
                                if (settings.hasApiKey()) Icons.Filled.CheckCircle else Icons.Filled.CloudOff,
                                null,
                                tint = if (settings.hasApiKey()) AccentGreen else TextMuted
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        if (!save()) return@Button
                        busy = true
                        status = null
                        scope.launch {
                            val result = AiEngine(settings).ask("Reply with exactly: JeE AI connection OK")
                            busy = false
                            status = if (result.success) result.text.trim() else "Connection failed: " + result.error
                        }
                    },
                    enabled = settings.hasApiKey() && !busy && model.isNotBlank() &&
                        (!customEndpoint || endpoint.trim().startsWith("https://")),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.NetworkCheck, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (busy) "Testing..." else "Test connection")
                }
            }
            status?.let { message ->
                item {
                    AssistChip(onClick = {}, enabled = false, label = { Text(message) })
                }
            }
            item {
                AssistChip(
                    onClick = {},
                    label = { Text("API keys never go into backups") },
                    leadingIcon = { Icon(Icons.Filled.Security, null, modifier = Modifier.size(16.dp)) }
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
