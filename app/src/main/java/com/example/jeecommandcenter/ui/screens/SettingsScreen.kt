package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.BackupRepository
import com.example.jeecommandcenter.ui.components.premiumClick
import com.example.jeecommandcenter.ui.theme.*
import com.example.jeecommandcenter.ui.components.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenAssessment: () -> Unit,
    onOpenMistakes: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var backupStatus by remember { mutableStateOf<String?>(null) }
    val backup = remember { BackupRepository(context) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(backup.exportJson())
                } ?: error("Could not open destination.")
                backupStatus = "Backup exported. API key is not included."
            }.onFailure {
                backupStatus = "Export failed: " + (it.message ?: "unknown error")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not open backup.")
                backup.importJson(json).getOrThrow()
                backupStatus = "Backup restored. Reopen JeE to refresh every screen."
            }.onFailure {
                backupStatus = "Import failed: " + (it.message ?: "invalid backup")
            }
        }
    }

    Scaffold(
        containerColor = BgApp,
        topBar = {
            JeeTopBar(title = "Settings", onBack = onBack)
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsCard("Analytics", "Real performance and study metrics", Icons.Filled.Insights, onOpenAnalytics)
            SettingsCard("Assessment", "Practice tests and starter mocks", Icons.Filled.Quiz, onOpenAssessment)
            SettingsCard("Mistake bank", "Automatic error tracking and review", Icons.Filled.ErrorOutline, onOpenMistakes)
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .padding(14.dp)
            ) {
                Text("Data backup", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Export or restore local JeE data. API keys remain in Android Keystore and are never exported.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("jee-backup.json") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore")
                    }
                }
                backupStatus?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = TextMuted, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "AI is optional. JeE's planner, revision and analytics continue to work without an AI provider.",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard)
            .premiumClick(onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = AccentBlueLight, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
    }
}
