package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.premiumClick
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun ChapterDetailScreen(chapterId: String, repo: JeeRepository, learning: LearningRepository, onBack: () -> Unit) {
    val chapter = JeeCatalog.find(chapterId) ?: return
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val topicsRepo = remember { ChapterTopicRepository(context) }
    val topics = remember(chapterId) { topicsRepo.topicsFor(chapter, learning) }
    var refresh by remember { mutableIntStateOf(0) }
    val completed = remember(refresh, chapterId) { topicsRepo.completedCount(chapterId, topics) }
    val progress = if (topics.isEmpty()) repo.getChapterState(chapterId).progress else completed.toFloat() / topics.size
    val animatedProgress by animateFloatAsState(progress, label = "chapter-detail-progress")

    Scaffold(containerColor = BgApp, topBar = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) { Text(chapter.name, style = MaterialTheme.typography.titleLarge); Text("${chapter.subject} · Chapter ${chapter.number}", color = TextSecondary, fontSize = 11.sp) }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BgCardAlt).border(1.dp, BgCardBorder, RoundedCornerShape(20.dp)).padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) { Column(Modifier.weight(1f)) { Text("Topic progress", color = TextSecondary, fontSize = 12.sp); Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium) }; Text("$completed / ${topics.size}", color = AccentBlueLight, fontSize = 12.sp) }
                    Spacer(Modifier.height(10.dp)); LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(JeeShapes.pill), color = AccentBlue, trackColor = BgDivider)
                    Spacer(Modifier.height(8.dp)); Text(if (progress >= 1f) "All topics checked. Chapter complete." else "Check topics as you finish them.", color = TextMuted, fontSize = 11.sp)
                }
            }
            item { Text("Topics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
            items(topics, key = { it }) { topic ->
                val checked = topicsRepo.isComplete(chapterId, topic)
                TopicCheckRow(topic, checked) {
                    topicsRepo.setComplete(chapterId, topic, !checked)
                    val newCompleted = topicsRepo.completedCount(chapterId, topics)
                    repo.setChapterProgress(chapter.subject, chapter.number, if (topics.isEmpty()) 0f else newCompleted.toFloat() / topics.size)
                    refresh++
                }
            }
            item { OutlinedButton(onClick = { topics.forEach { topicsRepo.setComplete(chapterId, it, true) }; repo.setChapterProgress(chapter.subject, chapter.number, 1f); refresh++ }, enabled = progress < 1f, modifier = Modifier.fillMaxWidth()) { Text("Mark chapter complete") } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TopicCheckRow(topic: String, checked: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (checked) BgCardAlt else BgCard).border(1.dp, if (checked) AccentBlue.copy(alpha = .55f) else BgCardBorder, RoundedCornerShape(16.dp)).premiumClick(onClick = onClick).padding(14.dp).animateContentSize(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(if (checked) AccentBlue else BgApp), Alignment.Center) {
            AnimatedContent(targetState = checked, label = "topic-check") { value -> Icon(if (value) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked, null, tint = if (value) AccentBlue else TextMuted, modifier = Modifier.size(17.dp)) }
        }
        Spacer(Modifier.width(12.dp)); Text(topic, color = TextPrimary, modifier = Modifier.weight(1f)); Text(if (checked) "Done" else "Check", color = if (checked) AccentGreen else TextMuted, fontSize = 11.sp)
    }
}
