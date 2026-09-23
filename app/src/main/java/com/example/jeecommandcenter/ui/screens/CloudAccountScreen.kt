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
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun CloudAccountScreen(context: android.content.Context, onBack: () -> Unit) {
    val cloud = remember { CloudJeeRepository() }
    var connected by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var syncing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun connectAndSync() {
        loading = true
        message = null
        runCatching {
            cloud.ensureSession()
            cloud.migrateLocalProgress(context)
        }.onSuccess {
            connected = true
            message = "Cloud session is active. Local syllabus progress has been migrated when needed."
        }.onFailure {
            connected = false
            message = "Cloud session unavailable. Enable Anonymous Sign-Ins in Supabase Auth. Local fallback remains available."
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
                    // Keep the operation coroutine-safe without introducing another state owner.
                    loading = true
                },
                enabled = !loading && !syncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sync local progress")
            }

            LaunchedEffect(syncing) {
                if (!syncing) return@LaunchedEffect
                connectAndSync()
                syncing = false
            }

            Text(
                "Anonymous authentication is the first cloud foundation. The app remains usable offline and only uploads the student's own progress.",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
