package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.AiChatHistoryRepository
import com.example.jeecommandcenter.data.AiConversation
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeePageHeader
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.components.SectionHeader
import com.example.jeecommandcenter.ui.theme.BgApp
import com.example.jeecommandcenter.ui.theme.Primary
import com.example.jeecommandcenter.ui.theme.TextMuted
import com.example.jeecommandcenter.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiHistoryScreen(
    context: android.content.Context,
    onBack: () -> Unit,
    onOpenConversation: (Long) -> Unit = {}
) {
    val repo = remember { AiChatHistoryRepository(context) }
    var conversations by remember { mutableStateOf(repo.getConversations()) }
    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "Chat history", onBack = onBack) }) { padding ->
        if (conversations.isEmpty()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
            ) {
                item {
                    JeePageHeader(
                        title = "Your AI archive",
                        subtitle = "Conversations saved on this device.",
                        eyebrow = "LOCAL HISTORY"
                    )
                }
                item {
                    JeeCard(featured = true) {
                        Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = Primary, modifier = Modifier.padding(bottom = 12.dp))
                        Text("No conversations yet", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "After the coach returns a response, that conversation will be available here. This archive reads saved conversation history from this device only.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                item { Text("Start a conversation from the AI study coach.", style = MaterialTheme.typography.bodyMedium, color = TextMuted) }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp)
            ) {
                item {
                    JeePageHeader(
                        title = "Your AI archive",
                        subtitle = "Saved conversations on this device.",
                        eyebrow = "LOCAL HISTORY"
                    )
                }
                item {
                    Text(
                        "Continue a previous study-coach conversation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                item { SectionHeader("Conversations · ${conversations.size}") }
                items(conversations, key = { it.id }) { conversation ->
                    ConversationCard(conversation) { onOpenConversation(conversation.id) }
                }
            }
        }
    }
    LaunchedEffect(Unit) { conversations = repo.getConversations() }
}

@Composable
private fun ConversationCard(conversation: AiConversation, onClick: () -> Unit) {
    val user = conversation.messages.firstOrNull { it.role == "USER" }?.text.orEmpty()
    val latest = conversation.messages.lastOrNull()?.text.orEmpty().replace("\n", " ").take(140)
    JeeCard(modifier = Modifier.animateContentSize()) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(conversation.title, style = MaterialTheme.typography.titleMedium)
                if (user.isNotBlank()) {
                    Text(user, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 2)
                }
                if (latest.isNotBlank()) {
                    Text(latest, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 2)
                }
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.height(16.dp))
                        Text(
                            SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(conversation.updatedAt)),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                    Text("${conversation.messages.size} messages · Continue", style = MaterialTheme.typography.labelMedium, color = Primary, fontSize = 12.sp)
                }
            }
        }
    }
}
