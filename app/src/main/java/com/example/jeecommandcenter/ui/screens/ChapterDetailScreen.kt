package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    context: android.content.Context,
    repo: JeeRepository,
    chapter: JeeChapter,
    onBack: () -> Unit,
    onPracticeQuestions: () -> Unit = {}
) {
    val topicsRepo = remember { TopicRepository(context) }
    val cloud = remember { CloudJeeRepository() }
    val scope = rememberCoroutineScope()
    var topics by remember(chapter.id) { mutableStateOf(topicsRepo.topicsFor(chapter)) }
    var cloudTopicIdsByLocalId by remember(chapter.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var cloudUserId by remember { mutableStateOf<String?>(null) }
    var cloudReady by remember { mutableStateOf(false) }
    var questionCount by remember { mutableIntStateOf(0) }
    var selectedTopic by remember(chapter.id) { mutableStateOf<ChapterTopic?>(null) }
    var showChapterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(chapter.id) {
        val localTopics = topicsRepo.topicsFor(chapter)
        topics = localTopics
        cloudReady = false
        runCatching {
            val user = cloud.ensureSession()
            cloudUserId = user.id
            val remoteTopics = cloud.topics(chapter.id)
            val progressByRemoteId = cloud.topicProgressForUser(user.id).associateBy { it.topicId }
            val remoteByTitle = remoteTopics.associateBy { it.title.trim().lowercase() }

            if (remoteTopics.isNotEmpty()) {
                cloudTopicIdsByLocalId = localTopics.mapNotNull { local ->
                    remoteByTitle[local.title.trim().lowercase()]?.id?.let { remoteId -> local.id to remoteId }
                }.toMap()

                topics = localTopics.map { local ->
                    val remote = remoteByTitle[local.title.trim().lowercase()]
                    local.copy(
                        completed = local.completed || (remote?.id?.let { progressByRemoteId[it]?.completed } == true)
                    )
                }
                cloudReady = cloudTopicIdsByLocalId.isNotEmpty()
            } else {
                cloudTopicIdsByLocalId = emptyMap()
            }

            questionCount = cloud.questions(chapter.id).size
        }.onFailure {
            cloudReady = false
            cloudTopicIdsByLocalId = emptyMap()
        }
    }

    val completed = topics.count { it.completed }
    val progress by animateFloatAsState(
        targetValue = if (topics.isEmpty()) 0f else completed.toFloat() / topics.size,
        label = "chapter-mastery-progress"
    )

    fun updateTopic(topic: ChapterTopic, isCompleted: Boolean) {
        topicsRepo.setCompleted(chapter.id, topic.id, isCompleted)
        val updatedTopics = topics.map {
            if (it.id == topic.id) it.copy(completed = isCompleted) else it
        }
        topics = updatedTopics
        selectedTopic = updatedTopics.firstOrNull { it.id == topic.id }
        val chapterProgress = if (updatedTopics.isEmpty()) 0f
        else updatedTopics.count { it.completed }.toFloat() / updatedTopics.size
        repo.setChapterProgress(chapter.subject, chapter.number, chapterProgress)

        cloudUserId?.let { uid ->
            scope.launch {
                runCatching {
                    cloudTopicIdsByLocalId[topic.id]?.let { remoteTopicId ->
                        cloud.setTopicCompleted(uid, remoteTopicId, isCompleted)
                    }
                    cloud.setChapterProgress(uid, chapter.id, chapterProgress)
                }
            }
        }
    }

    fun markChapterComplete() {
        topics.forEach { topicsRepo.setCompleted(chapter.id, it.id, true) }
        topics = topics.map { it.copy(completed = true) }
        selectedTopic = selectedTopic?.copy(completed = true)
        repo.setChapterProgress(chapter.subject, chapter.number, 1f)

        cloudUserId?.let { uid ->
            scope.launch {
                runCatching {
                    topics.forEach { topic ->
                        cloudTopicIdsByLocalId[topic.id]?.let { remoteTopicId ->
                            cloud.setTopicCompleted(uid, remoteTopicId, true)
                        }
                    }
                    cloud.setChapterProgress(uid, chapter.id, 1f)
                }
            }
        }
    }

    Scaffold(
        containerColor = BgApp,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            chapter.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${chapter.subject} · Chapter details",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showChapterMenu = true }) {
                            Icon(Icons.Filled.MoreHoriz, contentDescription = "Chapter options", tint = TextSecondary)
                        }
                        DropdownMenu(
                            expanded = showChapterMenu,
                            onDismissRequest = { showChapterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mark chapter complete") },
                                onClick = {
                                    showChapterMenu = false
                                    markChapterComplete()
                                },
                                leadingIcon = { Icon(Icons.Filled.CheckCircle, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgApp)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(BgCardAlt.copy(alpha = .78f))
                        .border(
                            JeeSurfaceTokens.borderWidth,
                            BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "CHAPTER MASTERY",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.1.sp
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                "${completed} of ${topics.size} topics",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                when {
                                    topics.isEmpty() -> "No learning topics available"
                                    completed == topics.size -> "Chapter mastery achieved"
                                    completed == 0 -> "Your progress starts here"
                                    else -> "Keep building your mastery"
                                },
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                color = Primary,
                                trackColor = BgDivider,
                                strokeWidth = 7.dp
                            )
                            Text(
                                "${(progress * 100).roundToInt()}%",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)),
                        color = Primary,
                        trackColor = BgDivider
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (cloudReady) Icons.Filled.Cloud else Icons.Filled.CloudOff,
                            null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            if (cloudReady) {
                                "Synced to Supabase. Topic completion updates cloud progress."
                            } else {
                                "Local fallback active · Sync resumes when available"
                            },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BgCard.copy(alpha = .96f))
                        .border(1.dp, BgCardBorder.copy(alpha = .9f), RoundedCornerShape(20.dp))
                        .clickable(enabled = questionCount > 0, onClick = onPracticeQuestions)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Box(
                        Modifier.size(58.dp).clip(RoundedCornerShape(16.dp))
                            .background(PrimarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.GpsFixed,
                            null,
                            tint = PrimaryLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Practice questions", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
                        Text(
                            "Test your understanding of this chapter",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            if (questionCount > 0) {
                                "Question bank · $questionCount available"
                            } else {
                                "Question availability depends on the bank"
                            },
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        null,
                        tint = if (questionCount > 0) PrimaryLight else TextMuted,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Learning topics",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        "Tap a topic to preview",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            items(topics, key = { it.id }) { topic ->
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgCard.copy(alpha = .94f))
                        .border(
                            1.dp,
                            if (topic.completed) Primary.copy(alpha = .7f)
                            else BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedTopic = topic }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                            .background(if (topic.completed) PrimarySoft else BgCardAlt),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (topic.completed) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow,
                            null,
                            tint = if (topic.completed) PrimaryLight else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            topic.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            if (topic.completed) "Completed" else "Not completed",
                            color = if (topic.completed) PrimaryLight else TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
                }
            }

            item {
                Button(
                    onClick = ::markChapterComplete,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = topics.isNotEmpty() && completed < topics.size
                ) {
                    Text(if (completed == topics.size && topics.isNotEmpty()) "Chapter completed" else "Mark chapter complete")
                }
            }
        }
    }

    selectedTopic?.let { topic ->
        ModalBottomSheet(
            onDismissRequest = { selectedTopic = null },
            containerColor = BgCardAlt,
            contentColor = TextPrimary,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("LEARNING TOPIC", color = PrimaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(topic.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Part of ${chapter.name}. Use this checkpoint to track your understanding, key formulas, and ability to solve JEE-style questions.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                LinearProgressIndicator(
                    progress = { if (topic.completed) 1f else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary,
                    trackColor = BgDivider
                )
                Text(
                    if (topic.completed) "This topic is marked complete." else "Mark this topic complete when you can explain the concept and solve standard questions.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Button(
                    onClick = { updateTopic(topic, !topic.completed) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (topic.completed) "Mark as incomplete" else "Mark as complete")
                }
            }
        }
    }
}
