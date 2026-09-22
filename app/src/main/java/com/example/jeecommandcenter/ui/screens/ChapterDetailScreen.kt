package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(context: android.content.Context, repo: JeeRepository, chapter: JeeChapter, onBack: () -> Unit) {
    val topicsRepo = remember { TopicRepository(context) }
    var topics by remember(chapter.id) { mutableStateOf(topicsRepo.topicsFor(chapter)) }
    val completed = topics.count { it.completed }
    val progress by animateFloatAsState(if (topics.isEmpty()) 0f else completed.toFloat() / topics.size, label = "topic-progress")

    Scaffold(containerColor = BgApp, topBar = {
        TopAppBar(title = { Column { Text(chapter.name); Text(chapter.subject, color = TextSecondary, fontSize = 11.sp) } }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BgApp))
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCardAlt).padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Topic progress", style = MaterialTheme.typography.titleMedium); Text("$completed / ${topics.size}", color = AccentBlueLight) }
                    Spacer(Modifier.height(10.dp)); LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = AccentBlue, trackColor = BgDivider)
                    Spacer(Modifier.height(6.dp)); Text("Check topics as you complete them. Chapter progress follows this checklist.", color = TextSecondary, fontSize = 11.sp)
                }
            }
            item { Text("Topics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp)) }
            items(topics, key = { it.id }) { topic ->
                val checkColor by animateColorAsState(if (topic.completed) AccentGreen else BgCard, label = "topic-color")
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(BgCard).clickable {
                    val next = !topic.completed
                    topicsRepo.setCompleted(chapter.id, topic.id, next)
                    topics = topics.map { if (it.id == topic.id) it.copy(completed = next) else it }
                    val p = if (topics.isEmpty()) 0f else topics.count { it.completed }.toFloat() / topics.size
                    repo.setChapterProgress(chapter.subject, chapter.number, p)
                }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(checkColor), Alignment.Center) { if (topic.completed) Icon(Icons.Filled.Check, null, tint = AccentGreenDark, modifier = Modifier.size(17.dp)) }
                    Spacer(Modifier.width(12.dp)); Text(topic.title, color = if (topic.completed) TextSecondary else TextOnCard, modifier = Modifier.weight(1f))
                }
            }
            item { Button(onClick = { topics.forEach { topicsRepo.setCompleted(chapter.id, it.id, true) }; topics = topics.map { it.copy(completed = true) }; repo.setChapterProgress(chapter.subject, chapter.number, 1f) }, modifier = Modifier.fillMaxWidth()) { Text("Mark chapter complete") } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
