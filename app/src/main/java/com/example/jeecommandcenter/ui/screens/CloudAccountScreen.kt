package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.data.CloudJeeRepository
import com.example.jeecommandcenter.data.CloudSyncCoordinator
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeePageHeader
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.components.SectionHeader
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
                message = "Synced: ↑ ${result.chaptersUploaded} chapters, ${result.topicsUploaded} topics, ${result.attemptsUploaded} attempts, ${result.preferenceDataUploaded} other records · ↓ ${result.chaptersRestored} chapters, ${result.topicsRestored} topics, ${result.attemptsRestored} attempts, ${result.preferenceDataRestored} other records restored."
            }
        }.onFailure {
            // Keep the account state on a sync failure and never expose raw provider exception text.
            message = safeCloudErrorMessage(it)
        }
        loading = false
    }

    LaunchedEffect(Unit) { refreshAccount(syncAfterLogin = true) }
    val networkUnavailable = message?.contains("could not reach the server", ignoreCase = true) == true
    val signedOutAfterAction = message?.startsWith("Signed out.", ignoreCase = true) == true
    val syncFailed = connected && message != null && !message.orEmpty().startsWith("Synced:")
    val statusTitle = when {
        loading && connected -> "Updating cloud account"
        loading -> "Checking cloud account"
        syncFailed -> "Sync needs attention"
        connected -> "Cloud sync available"
        signedOutAfterAction -> "Signed out"
        networkUnavailable -> "Connection unavailable"
        message != null -> "Cloud status unavailable"
        else -> "Signed out"
    }
    val statusSubtitle = when {
        loading && connected -> email ?: "Refreshing your cloud data"
        loading -> "Checking the saved session…"
        syncFailed -> "${email ?: "Account signed in"} · the latest sync did not finish."
        connected -> email ?: "Signed-in Sigma JE account"
        signedOutAfterAction -> "Sigma JE is still available offline."
        networkUnavailable -> "The server could not be reached. Local JeE data remains available offline."
        message != null -> "${message} Local JeE data remains available offline."
        else -> "No cloud account is connected. You can continue using JeE offline."
    }

    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "Cloud account", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            JeePageHeader(
                title = "Your cloud connection",
                subtitle = "Cloud sync is optional; local study data stays on this device and works offline.",
                eyebrow = "ACCOUNT & SYNC"
            )

            JeeCard(featured = true, modifier = Modifier.animateContentSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when {
                            syncFailed || networkUnavailable || message != null -> Icons.Filled.ErrorOutline
                            connected -> Icons.Filled.CloudDone
                            else -> Icons.Filled.CloudOff
                        },
                        contentDescription = null,
                        tint = when {
                            connected && !syncFailed -> Success
                            networkUnavailable || message != null -> Warning
                            else -> TextMuted
                        },
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(statusTitle, style = MaterialTheme.typography.titleLarge)
                        Text(statusSubtitle, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (loading || syncing) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = Primary)
                }
            }

            if (connected) {
                SectionHeader(title = "Sync")
                JeeCard {
                    Text(
                        "Sync includes chapter and topic progress, question attempts, daily progress, study sessions, tasks, revision notes, reminders, mistakes, assessment history, AI chat history and other saved user data.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    userId?.let { id ->
                        val lastSync = syncCoordinator.lastSyncAt(id)
                        if (lastSync > 0L) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Last sync · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastSync))}",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    Icon(Icons.Filled.Sync, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (syncing) "Syncing…" else "Sync now")
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
                                .onFailure { message = it.message ?: "Could not sign out." }
                            loading = false
                        }
                    },
                    enabled = !loading && !syncing,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out")
                }
            } else if (!loading) {
                SectionHeader(title = if (networkUnavailable || (message != null && !signedOutAfterAction)) "Try again when ready" else "Connect an account")
                if (networkUnavailable || (message != null && !signedOutAfterAction)) {
                    message?.let { errorMessage ->
                        JeeCard {
                            Text(errorMessage, color = if (networkUnavailable) Warning else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { scope.launch { refreshAccount(syncAfterLogin = true) } },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) { Text("Check connection again") }
                        }
                    }
                }
                Button(
                    onClick = { onOpenAuth(CloudAuthMode.SIGN_IN) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("Sign in") }
                OutlinedButton(
                    onClick = { onOpenAuth(CloudAuthMode.SIGN_UP) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("Create account") }
            }

            message?.takeIf { connected }?.let { syncMessage ->
                val failed = !syncMessage.startsWith("Synced:")
                JeeCard(modifier = Modifier.animateContentSize()) {
                    Text(if (failed) "Sync status" else "Latest sync result", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        syncMessage,
                        color = if (failed) MaterialTheme.colorScheme.error else TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun safeCloudErrorMessage(error: Throwable): String {
    val raw = error.message.orEmpty().lowercase()
    return when {
        "not-null" in raw || "null value in column" in raw ->
            "Cloud sync failed. Some required cloud data could not be saved."
        "jwt" in raw || "authorization" in raw || "bearer" in raw || "unauthorized" in raw ->
            "Cloud session expired. Sign in again and retry sync."
        "network" in raw || "timeout" in raw || "host" in raw ->
            "Cloud sync could not reach the server. Check your connection and try again."
        else ->
            "Cloud sync failed. Please try again."
    }
}
