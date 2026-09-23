package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.AiChatHistoryRepository
import com.example.jeecommandcenter.data.AiHistoryEntry
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiHistoryScreen(context: android.content.Context, onBack: () -> Unit) {
    val repo = remember { AiChatHistoryRepository(context) }
    val entries = remember { repo.getEntries() }
    var selected by remember { mutableStateOf<AiHistoryEntry?>(null) }

    Scaffold(
        containerColor = BgApp,
        topBar = { JeeTopBar(title = "Chat history", onBack = onBack) }
    ) { padding ->
        if (entries.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                JeeCard {
                    Icon(Icons.Filled.ChatBubbleOutline, null, tint = PrimaryLight, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("No conversations yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Your AI study-coach conversations will appear here after the first successful response.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgCard)
                            .border(1.dp, BgCardBorder.copy(alpha = .8f), RoundedCornerShape(16.dp))
                            .premiumClick { selected = entry }
                            .padding(15.dp)
                    ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(entry.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(entry.createdAt)),
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(entry.prompt, color = TextSecondary, fontSize = 11.sp, maxLines = 2)
                            Spacer(Modifier.height(6.dp))
                            Text(entry.response.replace("\n", " ").take(120), color = TextMuted, fontSize = 10.sp, maxLines = 2)
                        }
                    }
                }
            }
        }
    }

    selected?.let { entry ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(entry.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("You", color = PrimaryLight, fontSize = 11.sp)
                    Text(entry.prompt, color = TextPrimary, fontSize = 13.sp)
                    Text("Coach", color = PrimaryLight, fontSize = 11.sp)
                    Text(entry.response, color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }
}
