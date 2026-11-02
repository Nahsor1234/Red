package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.AiConfig
import com.example.jeecommandcenter.data.AiEngine
import com.example.jeecommandcenter.data.AiProvider
import com.example.jeecommandcenter.data.AiSettingsRepository
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeePageHeader
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.components.SectionHeader
import com.example.jeecommandcenter.ui.theme.BgApp
import com.example.jeecommandcenter.ui.theme.BgCardAlt
import com.example.jeecommandcenter.ui.theme.BgCardBorder
import com.example.jeecommandcenter.ui.theme.Danger
import com.example.jeecommandcenter.ui.theme.Primary
import com.example.jeecommandcenter.ui.theme.Success
import com.example.jeecommandcenter.ui.theme.TextMuted
import com.example.jeecommandcenter.ui.theme.TextSecondary
import com.example.jeecommandcenter.ui.theme.Warning
import kotlinx.coroutines.launch

private fun isTrustedHttpsEndpoint(raw: String): Boolean = runCatching {
    val uri = java.net.URI(raw.trim())
    uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo.isNullOrBlank()
}.getOrDefault(false)

@Composable
fun AiSettingsScreen(context: android.content.Context, onBack: () -> Unit) {
    val settings = remember { AiSettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(settings.getConfig()) }
    var key by remember { mutableStateOf("") }
    var keyConfigured by remember { mutableStateOf(settings.hasApiKey()) }
    var model by remember { mutableStateOf(config.model) }
    var endpoint by remember { mutableStateOf(config.endpoint) }
    var expanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    val customEndpoint = config.provider == AiProvider.CUSTOM_OPENAI_COMPATIBLE
    val hasUnsavedSettings = model != config.model || endpoint != config.endpoint

    fun save(): Boolean {
        if (model.isBlank()) {
            status = "Enter a model ID."
            statusIsError = true
            return false
        }
        if (customEndpoint && !isTrustedHttpsEndpoint(endpoint)) {
            status = "Enter a valid HTTPS URL with a host and no embedded credentials."
            statusIsError = true
            return false
        }
        settings.saveConfig(config.provider, model, endpoint)
        config = settings.getConfig()
        status = "AI configuration saved."
        statusIsError = false
        return true
    }

    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "AI configuration", onBack = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                JeePageHeader(
                    title = "AI configuration",
                    subtitle = "Choose a provider and manage its connection details.",
                    eyebrow = "OPTIONAL · CLOUD SERVICE"
                )
            }
            item {
                SectionHeader("Provider")
                JeeCard {
                    Text("Service", style = MaterialTheme.typography.titleMedium)
                    Text("JeE sends requests to the provider you select.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    androidx.compose.foundation.layout.Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(config.provider.label, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ExpandMore, contentDescription = "Choose provider")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            AiProvider.entries.forEach { provider ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            provider.label,
                                            color = if (provider == config.provider) Primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (provider == config.provider) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        val newModel = settings.defaultModel(provider)
                                        val newEndpoint = settings.defaultEndpoint(provider)
                                        config = AiConfig(provider, newModel, newEndpoint)
                                        model = newModel
                                        endpoint = newEndpoint
                                        settings.saveConfig(provider, newModel, newEndpoint)
                                        expanded = false
                                        status = "Provider changed to ${provider.label}."
                                        statusIsError = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text("Provider selection is saved immediately.", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }
            item {
                SectionHeader("Model and endpoint")
                JeeCard {
                    Text("Model ID", style = MaterialTheme.typography.titleMedium)
                    Text("Enter a model supported by the selected provider.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Model ID") },
                        singleLine = true
                    )
                    if (customEndpoint) {
                        Spacer(Modifier.height(14.dp))
                        Text("Custom API endpoint", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "This host receives your API key and request content. Use only a server you trust.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Warning
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = { endpoint = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("HTTPS endpoint") },
                            placeholder = { Text("https://your-host/v1/chat/completions") },
                            singleLine = true,
                            isError = endpoint.isNotBlank() && !isTrustedHttpsEndpoint(endpoint)
                        )
                        Text(
                            "Use HTTPS with a hostname and no embedded credentials.",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("Save model settings") }
                    if (hasUnsavedSettings) {
                        Spacer(Modifier.height(5.dp))
                        Text("Model or endpoint edits are not saved yet.", style = MaterialTheme.typography.labelMedium, color = Warning)
                    }
                }
            }
            item {
                SectionHeader("API key and device security")
                JeeCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = Primary)
                        Column {
                            Text("Key stored on this device", style = MaterialTheme.typography.titleMedium)
                            Text("Stored locally using Android Keystore encryption. Keys are not included in backups.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("${config.provider.label} API key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (key.isNotBlank()) runCatching {
                                    settings.saveApiKey(key)
                                    settings.saveConfig(config.provider, model, endpoint)
                                }.onSuccess {
                                    key = ""
                                    keyConfigured = true
                                    status = "API key saved securely on this device."
                                    statusIsError = false
                                }.onFailure {
                                    status = "Could not save API key: ${it.message ?: "secure storage failed"}"
                                    statusIsError = true
                                }
                            },
                            enabled = key.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Save key") }
                        OutlinedButton(
                            onClick = {
                                settings.clearApiKey()
                                key = ""
                                keyConfigured = false
                                status = "API key removed."
                                statusIsError = false
                            },
                            enabled = keyConfigured,
                            modifier = Modifier.weight(1f)
                        ) { Text("Remove key") }
                    }
                }
            }
            item {
                SectionHeader("Connection status")
                JeeCard(modifier = Modifier.animateContentSize()) {
                    ListItem(
                        headlineContent = { Text(if (keyConfigured) "Ready to test" else "API key required") },
                        supportingContent = {
                            Text(if (keyConfigured) "A test request will be sent to ${config.provider.label}." else "Add a provider API key before testing or using AI actions.")
                        },
                        leadingContent = {
                            Icon(
                                if (keyConfigured) Icons.Filled.CheckCircle else Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = if (keyConfigured) Success else TextMuted
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (!save()) return@Button
                            busy = true
                            status = null
                            scope.launch {
                                val response = AiEngine(settings).ask("Reply with exactly: JeE AI connection OK")
                                busy = false
                                status = if (response.success) response.text.trim() else "Connection failed: ${response.error}"
                                statusIsError = !response.success
                            }
                        },
                        enabled = keyConfigured && !busy && model.isNotBlank() &&
                            (!customEndpoint || isTrustedHttpsEndpoint(endpoint)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.NetworkCheck, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (busy) "Testing connection…" else "Test connection")
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = status != null) {
                        Surface(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            color = if (statusIsError) Danger.copy(alpha = .08f) else BgCardAlt,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                status.orEmpty(),
                                Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (statusIsError) Danger else TextSecondary
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
