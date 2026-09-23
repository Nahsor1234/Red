package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.AiChatHistoryRepository
import com.example.jeecommandcenter.data.AiConversation
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiHistoryScreen(context: android.content.Context, onBack: () -> Unit, onOpenConversation: (Long) -> Unit = {}) {
    val repo = remember { AiChatHistoryRepository(context) }
    var conversations by remember { mutableStateOf(repo.getConversations()) }
    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "Chat history", onBack = onBack) }) { padding ->
        if (conversations.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.Center) {
                JeeCard {
                    Icon(Icons.Filled.ChatBubbleOutline, null, tint = PrimaryLight, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(10.dp)); Text("No conversations yet", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(5.dp))
                    Text("Your AI study-coach conversations will appear here after the first successful response.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(conversations, key = { it.id }) { conversation -> ConversationCard(conversation) { onOpenConversation(conversation.id) } }
            }
        }
    }
    LaunchedEffect(Unit) { conversations = repo.getConversations() }
}

@Composable private fun ConversationCard(conversation: AiConversation, onClick: () -> Unit) {
    val user = conversation.messages.firstOrNull { it.role == "USER" }?.text.orEmpty()
    val latest = conversation.messages.lastOrNull()?.text.orEmpty().replace("\n", " ").take(140)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).border(1.dp, BgCardBorder.copy(alpha = .8f), RoundedCornerShape(16.dp)).premiumClick(onClick).padding(15.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(conversation.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium); Text(SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(conversation.updatedAt)), color = TextMuted, fontSize = 9.sp) }
        Spacer(Modifier.height(5.dp)); if (user.isNotBlank()) Text(user, color = TextSecondary, fontSize = 11.sp, maxLines = 2); Spacer(Modifier.height(6.dp)); if (latest.isNotBlank()) Text(latest, color = TextMuted, fontSize = 10.sp, maxLines = 2); Spacer(Modifier.height(7.dp)); Text("${conversation.messages.size} messages · Tap to continue", color = PrimaryLight, fontSize = 10.sp)
    }
}
