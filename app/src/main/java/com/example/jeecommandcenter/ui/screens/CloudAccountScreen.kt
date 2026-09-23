package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.CloudJeeRepository
import com.example.jeecommandcenter.data.CloudSyncCoordinator
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun CloudAccountScreen(context: android.content.Context, onBack: () -> Unit) {
    val cloud = remember { CloudJeeRepository() }
    val syncCoordinator = remember { CloudSyncCoordinator(context, cloud) }
    var connected by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var syncing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun connectAndSync() {
        loading = true
        message = null
        runCatching {
            cloud.ensureSession()
            syncCoordinator.sync()
        }.onSuccess { result ->
            connected = true
            message = "Synced ${result.chaptersUploaded} chapters, ${result.topicsUploaded} topics and ${result.attemptsUploaded} new question attempts."
        }.onFailure {
            connected = false
            message = "Cloud sync unavailable. Enable Anonymous Sign-Ins in Supabase Auth. Local data remains available offline."
        }
        loading = false
    }

    LaunchedEffect(Unit) { connectAndSync() }

    Scaffold(
        containerColor = BgApp,
        topBar = { JeeTopBar(title = "Cloud sync", onBack = onBack) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JeeCard(featured = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (connected) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        null,
                        tint = if (connected) Primary else Danger
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            if (connected) "Cloud sync active" else "Cloud sync unavailable",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (connected) "Supabase PostgreSQL is connected to Sigma JE."
                            else "The app continues with local data until cloud authentication is available.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (loading || syncing) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Primary)
            }

            message?.let { Text(it, color = TextSecondary, fontSize = 12.sp) }

            OutlinedButton(
                onClick = {
                    syncing = true
                },
                enabled = !loading && !syncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sync now")
            }

            LaunchedEffect(syncing) {
                if (!syncing) return@LaunchedEffect
                connectAndSync()
                syncing = false
            }

            Text(
                "Sigma JE stays usable offline. Sync uploads only the student's progress and question attempts; the shared syllabus/question catalog is read-only.",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
