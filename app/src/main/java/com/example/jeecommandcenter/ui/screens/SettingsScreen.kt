package com.example.jeecommandcenter.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.data.BackupRepository
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenAnalytics: () -> Unit, onOpenAiSettings: () -> Unit, onOpenCloudAccount: () -> Unit = {}, onOpenNotifications: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var backupStatus by remember { mutableStateOf<String?>(null) }
    val backup = remember { BackupRepository(context) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(backup.exportJson()) }
                ?: error("Could not open destination.")
            backupStatus = "Backup exported. API key is not included."
        }.onFailure { backupStatus = "Export failed: ${it.message ?: "unknown error"}" }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Could not open backup.")
            backup.importJson(json).getOrThrow()
            backupStatus = "Backup restored. Reopen JeE to refresh all screens. A recovery point was created first."
        }.onFailure { backupStatus = "Import failed: ${it.message ?: "invalid backup"}" }
    }

    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "Settings", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JeePageHeader(
                title = "Make JeE yours",
                subtitle = "Choose what to connect, and keep control of your study data.",
                eyebrow = "PREFERENCES"
            )

            SectionHeader(title = "Account & sync")
            SettingsCard("Cloud account", "Optional sync and cloud syllabus access", Icons.Filled.Cloud, onOpenCloudAccount)

            SectionHeader(title = "Study tools")
            SettingsCard("Notifications", "Study routine and Daily Progress reminders", Icons.Filled.Notifications, onOpenNotifications)
            SettingsCard("Analytics", "Review stored study and performance metrics", Icons.Filled.Insights, onOpenAnalytics)
            SettingsCard("AI configuration", "Provider, model and API key", Icons.Filled.AutoAwesome, onOpenAiSettings)

            SectionHeader(title = "Data protection")
            JeeCard(modifier = Modifier.animateContentSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = Success, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Local backup", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Export or restore your local JeE history. API keys remain in Android Keystore and are never included.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("jee-backup.json") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) { Text("Export backup") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) { Text("Restore backup") }
                }
                if (backup.hasRecoveryPoint()) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            backup.recoverLastKnownGood().fold(
                                onSuccess = { backupStatus = "Previous data restored from the local recovery point." },
                                onFailure = { backupStatus = "Recovery failed: ${it.message ?: "unknown error"}" }
                            )
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Recover previous data") }
                }
                backupStatus?.let { status ->
                    val failed = status.startsWith("Export failed") || status.startsWith("Import failed") || status.startsWith("Recovery failed")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        status,
                        color = if (failed) MaterialTheme.colorScheme.error else Success,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                "JeE works offline. AI is optional and does not replace local revision, planning, analytics or backup.",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    JeeCard(modifier = Modifier.semantics { role = Role.Button }.premiumClick(onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
