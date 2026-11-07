package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SyllabusScreen(
    repo: JeeRepository,
    onOpenRevision: () -> Unit = {},
    onOpenChapter: (JeeChapter) -> Unit = {}
) {
    var subject by remember { mutableStateOf("Physics") }
    var filter by remember { mutableStateOf("All") }
    var refresh by remember { mutableIntStateOf(0) }
    var cloudChapters by remember { mutableStateOf<List<CloudChapter>>(emptyList()) }
    var cloudProgress by remember { mutableStateOf<Map<String, CloudChapterProgress>>(emptyMap()) }
    var cloudTopicProgress by remember { mutableStateOf<Map<String, CloudTopicProgress>>(emptyMap()) }
    var cloudAvailable by remember { mutableStateOf(false) }
    var cloudError by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val cloud = remember { CloudJeeRepository() }
    val scope = rememberCoroutineScope()

    fun refreshCloud() {
        scope.launch {
            cloudError = null
            runCatching {
                val user = cloud.ensureSession()
                cloud.migrateLocalProgress(context.applicationContext)
                cloudChapters = cloud.chapters(subject.lowercase())
                cloudProgress = cloud.chapterProgressForUser(user.id).associateBy { it.chapterId }
                cloudTopicProgress = cloud.topicProgressForUser(user.id).associateBy { it.topicId }
                cloudAvailable = true
            }.onFailure {
                cloudAvailable = false
                cloudError = it.message
            }
        }
    }

    LaunchedEffect(subject) {
        refreshCloud()
    }

    val localChapters = remember(refresh, subject) { repo.chapters(subject) }
    val intelligence = remember(context) {
        JeeIntelligence(repo, LearningRepository(context.applicationContext))
    }
    var weak by remember { mutableStateOf<Set<String>>(emptySet()) }
    var due by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(refresh, subject) {
        val signals = withContext(Dispatchers.Default) {
            intelligence.weakChapters(20).map { it.chapter.id }.toSet() to
                repo.getRevisionQueue().map { it.chapter.id }.toSet()
        }
        weak = signals.first
        due = signals.second
    }

    fun progressFor(id: String): Float {
        cloudProgress[id]?.progress?.let { return it }
        val topicIds = cloudTopicProgress.keys.filter { it.startsWith(id + "_") }
        if (topicIds.isNotEmpty()) {
            return topicIds.count { cloudTopicProgress[it]?.completed == true }.toFloat() / topicIds.size
        }
        return localChapters
            .firstOrNull { it.subject.lowercase() + "_" + it.number == id }
            ?.progress
            ?: 0f
    }

    // Built once per data change; progressFor scans topics, so this must not run on every recomposition.
    val allModels = remember(cloudAvailable, cloudChapters, cloudProgress, cloudTopicProgress, localChapters, subject) {
        if (cloudAvailable) {
            cloudChapters.map { chapter ->
                JeeChapter(
                    chapter.id,
                    subject,
                    chapter.number,
                    chapter.name,
                    chapter.difficulty,
                    chapter.estimatedMinutes
                ) to progressFor(chapter.id)
            }
        } else {
            localChapters.map { chapter ->
                val id = "${chapter.subject.lowercase()}_${chapter.number}"
                val model = JeeCatalog.forSubject(subject).firstOrNull { it.number == chapter.number }
                    ?: JeeChapter(id, chapter.subject, chapter.number, chapter.name)
                model to chapter.progress
            }
        }
    }
    val models = remember(allModels, filter, weak) {
        allModels.filter { (chapter, progress) ->
            when (filter) {
                "In progress" -> progress > 0f && progress < 1f
                "Done" -> progress >= 1f
                "Weak" -> weak.contains(chapter.id)
                else -> true
            }
        }
    }
    // The summary describes the whole subject, not the active filter.
    val avg = remember(allModels) { if (allModels.isEmpty()) 0f else allModels.map { it.second }.average().toFloat() }
    val attention = remember(allModels, weak) { allModels.firstOrNull { weak.contains(it.first.id) } }

    Scaffold(containerColor = BgApp) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Syllabus", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            if (cloudAvailable) Icons.Filled.Cloud else Icons.Filled.CloudOff,
                            if (cloudAvailable) "Synced" else "Offline",
                            tint = if (cloudAvailable) PrimaryLight else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    cloudError?.let { Text("Offline syllabus · local fallback", style = MaterialTheme.typography.labelSmall) }
                }
                TextButton(onClick = onOpenRevision) {
                    Text("Notes", style = MaterialTheme.typography.labelLarge, color = PrimaryLight)
                }
            }

            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Physics", "Chemistry", "Mathematics")) { value ->
                    JeeFilterChip(value, value == subject) { subject = value; filter = "All" }
                }
            }

            Spacer(Modifier.height(14.dp))
            JeeCard(featured = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(progress = avg, diameter = 92.dp, strokeWidth = 9.dp) {
                        Text("${(avg * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(subject, style = MaterialTheme.typography.titleMedium)
                        Text("${allModels.count { it.second >= 1f }} of ${allModels.size} chapters complete", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${allModels.count { it.second > 0f && it.second < 1f }} active · ${allModels.count { it.second == 0f }} not started",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Weak", "In progress", "Done")) { value ->
                    JeeFilterChip(value, value == filter) { filter = value }
                }
            }

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
            ) {
                if (filter == "All" && attention != null) {
                    item(key = "attention") {
                        JeeCard {
                            Text("Next attention", style = MaterialTheme.typography.labelLarge, color = PrimaryLight)
                            Text(attention.first.name, style = MaterialTheme.typography.titleMedium)
                            Text("Low confidence, incomplete or weak chapter", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))
                            LinearStatBar(attention.second, height = 5.dp)
                        }
                    }
                }
                items(models, key = { it.first.id }) { (chapter, progress) ->
                    val badge = when {
                        due.contains(chapter.id) -> "Review due" to Warning
                        weak.contains(chapter.id) -> "Weak" to PrimaryLight
                        else -> null
                    }
                    ChapterRow(chapter, progress, badge?.first, badge?.second ?: TextMuted) { onOpenChapter(chapter) }
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(chapter: JeeChapter, progress: Float, badge: String?, badgeColor: Color, onClick: () -> Unit) {
    val done = progress >= 1f
    Row(
        Modifier
            .fillMaxWidth()
            .clip(JeeShapes.medium)
            .background(BgCard)
            .border(1.dp, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
            .premiumClick(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProgressRing(progress, diameter = 40.dp, strokeWidth = 4.dp, color = if (done) Success else Primary) {
            if (done) Icon(Icons.Filled.Check, "Complete", tint = Success, modifier = Modifier.size(18.dp))
            else Text(chapter.number.toString(), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(chapter.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${(progress * 100).toInt()}% · ${chapter.estimatedMinutes} min", style = MaterialTheme.typography.labelSmall)
        }
        if (badge != null) Text(badge, style = MaterialTheme.typography.labelSmall, color = badgeColor, modifier = Modifier.padding(horizontal = 6.dp))
        Icon(Icons.Filled.ChevronRight, "Open chapter", tint = TextMuted, modifier = Modifier.size(20.dp))
    }
}
