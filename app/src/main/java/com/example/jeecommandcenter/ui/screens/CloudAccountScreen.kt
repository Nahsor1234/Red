package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
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
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun CloudAccountScreen(
    context: android.content.Context,
    onBack: () -> Unit,
    onOpenAuth: (CloudAuthMode) -> Unit = {}
) {
    val cloud = remember { CloudJeeRepository() }
    val syncCoordinator = remember { CloudSyncCoordinator(context, cloud) }
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var syncing by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refreshAccount(syncAfterLogin: Boolean) {
        loading = true
        message = null
        runCatching {
            val user = cloud.currentUser()
            if (user == null) {
                connected = false
                userId = null
                email = null
                return@runCatching
            }
            userId = user.id
            email = user.email
            connected = true
            if (syncAfterLogin) {
                val result = syncCoordinator.sync()
                message = "Synced ${result.chaptersUploaded} chapters, ${result.topicsUploaded} topics and ${result.attemptsUploaded} new question attempts."
            }
        }.onFailure {
            connected = false
            message = it.message?.takeIf(String::isNotBlank) ?: "Cloud sync failed."
        }
        loading = false
    }

    LaunchedEffect(Unit) { refreshAccount(syncAfterLogin = true) }

    Scaffold(
        containerColor = BgApp,
        topBar = { JeeTopBar(title = "Cloud account", onBack = onBack) }
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
                        tint = if (connected) Primary else Danger,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (connected) "Cloud sync active" else "Cloud account not connected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (connected) email ?: "Signed-in Sigma JE account"
                            else "Sign in to sync your progress across devices.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (connected) {
                Text(
                    "Your local progress stays available offline. Sync uploads your progress and question attempts; the shared syllabus and question catalog remain read-only.",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                userId?.let { id ->
                    val lastSync = syncCoordinator.lastSyncAt(id)
                    if (lastSync > 0L) {
                        Text(
                            "Last sync: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastSync))}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                if (loading || syncing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = Primary)
                }

                Button(
                    onClick = {
                        scope.launch {
                            syncing = true
                            refreshAccount(syncAfterLogin = true)
                            syncing = false
                        }
                    },
                    enabled = !loading && !syncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Sync, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sync now")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            loading = true
                            runCatching { cloud.signOut() }
                                .onSuccess {
                                    connected = false
                                    userId = null
                                    email = null
                                    message = "Signed out. Sigma JE is still available offline."
                                }
                                .onFailure {
                                    message = it.message ?: "Could not sign out."
                                }
                            loading = false
                        }
                    },
                    enabled = !loading && !syncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out")
                }
            } else {
                Text(
                    "Authentication is optional. Continue using Sigma JE offline, or connect an account when you want cloud backup and multi-device sync.",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Button(
                    onClick = { onOpenAuth(CloudAuthMode.SIGN_IN) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in")
                }

                OutlinedButton(
                    onClick = { onOpenAuth(CloudAuthMode.SIGN_UP) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create account")
                }
            }

            message?.let {
                Text(it, color = if (connected) TextSecondary else Danger, fontSize = 11.sp)
            }
        }
    }
}
