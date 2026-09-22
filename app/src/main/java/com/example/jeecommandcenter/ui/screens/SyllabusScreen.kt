package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun SyllabusScreen(repo: JeeRepository, selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit = {}, onOpenPlanner: () -> Unit = {}, onOpenRevision: () -> Unit = {}) {
    var subject by remember { mutableStateOf("Physics") }
    var filter by remember { mutableStateOf("All") }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedChapter by remember { mutableStateOf<ChapterProgress?>(null) }
    val subjects = listOf("Physics", "Chemistry", "Mathematics")
    val filters = listOf("All", "Weak", "In progress", "Done")
    val all = remember(refresh, subject) { repo.chapters(subject) }
    val context = LocalContext.current
    val intelligence = remember(context) { JeeIntelligence(repo, LearningRepository(context.applicationContext)) }
    val weak = remember(refresh, subject) { intelligence.weakChapters(20).map { it.chapter.id }.toSet() }
    val due = remember(refresh) { repo.getRevisionQueue().map { it.chapter.id }.toSet() }
    val catalog = remember(subject) { JeeCatalog.forSubject(subject) }
    val visible = all.filter { chapter ->
        when (filter) {
            "In progress" -> chapter.progress > 0f && chapter.progress < 1f
            "Done" -> chapter.progress >= 1f
            "Weak" -> catalog.firstOrNull { it.number == chapter.number }?.id?.let(weak::contains) == true
            else -> true
        }
    }
    val attention = all.firstOrNull { chapter -> catalog.firstOrNull { it.number == chapter.number }?.id?.let(weak::contains) == true }
    val avg = if (all.isEmpty()) 0f else all.map { it.progress }.average().toFloat()

    Scaffold(containerColor = BgApp, bottomBar = { BottomNavBar(selectedTab, onTabSelected, onAiClick) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Syllabus", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onOpenRevision) { Text("Review", fontSize = 12.sp) }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(subjects) { value -> FilterChip(value, value == subject) { subject = value } } }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BgCardAlt).padding(17.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(subject.uppercase(), color = TextSecondary, fontSize = 12.sp)
                        Text("${(avg * 100).toInt()}%", style = MaterialTheme.typography.displaySmall)
                        Text("${all.count { it.progress >= 1f }} of ${all.size} chapters complete", color = TextMuted, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${all.count { it.progress > 0f && it.progress < 1f }} active", color = AccentBlue)
                        Text("${all.count { it.progress == 0f }} not started", color = TextMuted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp)); LinearStatBar(avg)
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(filters) { value -> FilterChip(value, value == filter) { filter = value } } }
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.weight(1f)) {
                if (filter == "All" && attention != null) {
                    item {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCard).padding(15.dp)) {
                            Text("NEXT ATTENTION", color = TextSecondary, fontSize = 11.sp)
                            Text(attention.name, style = MaterialTheme.typography.titleMedium)
                            Text("Low confidence or no recent study", color = TextMuted, fontSize = 11.sp)
                            Spacer(Modifier.height(8.dp)); LinearStatBar(attention.progress, height = 5.dp)
                        }
                    }
                }
                items(visible, key = { it.number }) { chapter ->
                    val id = catalog.firstOrNull { it.number == chapter.number }?.id
                    Row(
                        Modifier.fillMaxWidth()
                            .premiumClick { selectedChapter = chapter }
                            .padding(vertical = 14.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(13.dp).clip(CircleShape).background(if (chapter.progress >= 1f) AccentGreen else BgDivider), Alignment.Center) {
                                if (chapter.progress >= 1f) Icon(Icons.Filled.Check, null, tint = AccentGreenDark, modifier = Modifier.size(9.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("${chapter.number}. ${chapter.name}", color = TextOnCard, fontSize = 14.sp)
                                Text("${(chapter.progress * 100).toInt()}% · confidence ${chapter.confidence}/5", color = TextMuted, fontSize = 10.sp)
                                Spacer(Modifier.height(5.dp)); LinearStatBar(chapter.progress, Modifier.width(120.dp), height = 3.dp)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (due.contains(id)) Text("Review due", color = AccentAmber, fontSize = 10.sp)
                            else if (weak.contains(id)) Text("Weak", color = AccentBlueLight, fontSize = 10.sp)
                            Icon(Icons.Filled.ChevronRight, "Open chapter", tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = BgDivider, thickness = .5.dp)
                }
            }
        }
    }

    selectedChapter?.let { chapter ->
        var editProgress by remember(chapter) { mutableFloatStateOf(chapter.progress) }
        AlertDialog(
            onDismissRequest = { selectedChapter = null },
            title = { Text(chapter.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Chapter ${chapter.number} · ${chapter.subject}", color = TextSecondary, fontSize = 12.sp)
                    Text("Progress ${((editProgress * 100).toInt())}%", style = MaterialTheme.typography.titleMedium)
                    Slider(value = editProgress, onValueChange = { editProgress = it }, valueRange = 0f..1f, steps = 3)
                    Text("Confidence: ${chapter.confidence}/5", color = TextMuted, fontSize = 11.sp)
                    Text("Progress changes are explicit here; tapping the chapter row no longer changes it automatically.", color = TextMuted, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    repo.setChapterProgress(chapter.subject, chapter.number, editProgress)
                    selectedChapter = null
                    refresh++
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { selectedChapter = null }) { Text("Cancel") } }
        )
    }
}
