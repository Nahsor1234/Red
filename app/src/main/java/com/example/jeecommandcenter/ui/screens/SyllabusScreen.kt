package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SyllabusScreen(
    repo: JeeRepository,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onAiClick: () -> Unit = {},
    onOpenPlanner: () -> Unit = {},
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
    val weak = remember(refresh, subject) {
        intelligence.weakChapters(20).map { it.chapter.id }.toSet()
    }
    val due = remember(refresh) {
        repo.getRevisionQueue().map { it.chapter.id }.toSet()
    }

    fun progressFor(id: String): Float {
        cloudProgress[id]?.progress?.let { return it }
        val topicIds = cloudTopicProgress.keys.filter { it.startsWith(id + "_") }
        if (topicIds.isNotEmpty()) {
            return topicIds.count { cloudTopicProgress[it]?.completed == true }.toFloat() / topicIds.size
        }
        return localChapters
            .firstOrNull { ${it.subject.lowercase()}_${it.number} == id }
            ?.progress
            ?: 0f
    }

    val models = if (cloudAvailable) {
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
    }.filter { (chapter, progress) ->
        when (filter) {
            "In progress" -> progress > 0f && progress < 1f
            "Done" -> progress >= 1f
            "Weak" -> weak.contains(chapter.id)
            else -> true
        }
    }

    val avg = if (models.isEmpty()) 0f else models.map { it.second }.average().toFloat()
    val attention = models.firstOrNull { weak.contains(it.first.id) }

    Box(Modifier.fillMaxSize()) {
        JeeBackground()
        Scaffold(
            containerColor = BgApp.copy(alpha = 0f),
            bottomBar = { BottomNavBar(selectedTab, onTabSelected, onAiClick) }
        ) { padding ->
            Column(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 18.dp)
            ) {
                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Syllabus", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(7.dp))
                            Icon(
                                if (cloudAvailable) Icons.Filled.Cloud else Icons.Filled.CloudOff,
                                null,
                                tint = if (cloudAvailable) PrimaryLight else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        cloudError?.let {
                            Text("Offline syllabus · local fallback", color = TextMuted, fontSize = 9.sp)
                        }
                    }
                    TextButton(onClick = onOpenRevision) {
                        Text("Review", fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Physics", "Chemistry", "Mathematics")) { value ->
                        JeeFilterChip(value, value == subject) {
                            subject = value
                            filter = "All"
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BgCardAlt)
                        .border(
                            JeeSurfaceTokens.borderWidth,
                            BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha),
                            JeeShapes.medium
                        )
                        .padding(17.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(subject.uppercase(), color = TextSecondary, fontSize = 12.sp)
                            Text("${(avg * 100).toInt()}%", style = MaterialTheme.typography.displaySmall)
                            Text(
                                "${models.count { it.second >= 1f }} of ${models.size} chapters complete",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${models.count { it.second > 0f && it.second < 1f }} active",
                                color = PrimaryLight
                            )
                            Text(
                                "${models.count { it.second == 0f }} not started",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearStatBar(avg)
                }

                Spacer(Modifier.height(14.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Weak", "In progress", "Done")) { value ->
                        JeeFilterChip(value, value == filter) { filter = value }
                    }
                }

                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    if (filter == "All" && attention != null) {
                        item {
                            Column(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(BgCard)
                                    .border(
                                        JeeSurfaceTokens.borderWidth,
                                        BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha),
                                        JeeShapes.medium
                                    )
                                    .padding(15.dp)
                            ) {
                                Text("NEXT ATTENTION", color = TextSecondary, fontSize = 11.sp)
                                Text(attention.first.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Low confidence, incomplete or weak chapter",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                LinearStatBar(attention.second, height = 5.dp)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    items(models, key = { it.first.id }) { (chapter, progress) ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(BgCard)
                                .border(
                                    1.dp,
                                    BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha),
                                    RoundedCornerShape(16.dp)
                                )
                                .premiumClick { onOpenChapter(chapter) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(13.dp)
                                        .clip(CircleShape)
                                        .background(if (progress >= 1f) AccentGreen else BgDivider),
                                    Alignment.Center
                                ) {
                                    if (progress >= 1f) {
                                        Icon(
                                            Icons.Filled.Check,
                                            null,
                                            tint = AccentGreenDark,
                                            modifier = Modifier.size(9.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "${chapter.number}. ${chapter.name}",
                                        color = TextOnCard,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "${(progress * 100).toInt()}% · ${chapter.estimatedMinutes} min",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    LinearStatBar(
                                        progress,
                                        Modifier.width(120.dp),
                                        height = 3.dp
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (due.contains(chapter.id)) {
                                    Text("Review due", color = AccentAmber, fontSize = 10.sp)
                                } else if (weak.contains(chapter.id)) {
                                    Text("Weak", color = PrimaryLight, fontSize = 10.sp)
                                }
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    "Open chapter",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
