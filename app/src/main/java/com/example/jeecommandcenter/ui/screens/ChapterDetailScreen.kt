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
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.LinearStatBar
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
    var questionsLoading by remember { mutableStateOf(true) }
    var questionBankFailed by remember { mutableStateOf(false) }
    var selectedTopic by remember(chapter.id) { mutableStateOf<ChapterTopic?>(null) }
    var showChapterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(chapter.id) {
        val localTopics = topicsRepo.topicsFor(chapter)
        topics = localTopics
        cloudReady = false
        questionsLoading = true
        questionBankFailed = false
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
            questionsLoading = false
        }.onFailure {
            cloudReady = false
            cloudTopicIdsByLocalId = emptyMap()
            questionCount = 0
            questionBankFailed = true
            questionsLoading = false
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
                JeeCard(modifier = Modifier.animateContentSize()) {
                    Text("CHAPTER MASTERY", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${completed} of ${topics.size} topics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(
                                when {
                                    topics.isEmpty() -> "No learning topics available"
                                    completed == topics.size -> "All listed topics are complete"
                                    completed == 0 -> "Start with any topic below"
                                    else -> "Continue checking off topics you have mastered"
                                },
                                color = TextSecondary, style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), color = Primary, trackColor = BgDivider, strokeWidth = 6.dp)
                            Text("${(progress * 100).roundToInt()}%", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearStatBar(progress, height = 6.dp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (cloudReady) Icons.Filled.Cloud else Icons.Filled.CloudOff, null, tint = if (cloudReady) Primary else TextSecondary, modifier = Modifier.size(18.dp))
                        Text(
                            if (cloudReady) "Cloud topic progress connected · changes also save locally"
                            else "Local topic progress · cloud sync unavailable or not mapped",
                            color = TextSecondary, style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                JeeCard(featured = true, modifier = Modifier.clickable(enabled = questionCount > 0, onClick = onPracticeQuestions)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(PrimarySoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.GpsFixed, null, tint = Primary, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Practice questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                when {
                                    questionsLoading -> "Checking question bank…"
                                    questionCount > 0 -> "$questionCount questions available"
                                    questionBankFailed -> "Question bank unavailable right now"
                                    else -> "No questions currently available in the bank"
                                },
                                color = TextSecondary, style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, "Open practice", tint = if (questionCount > 0) Primary else TextMuted)
                    }
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
                        "Mark topics as you master them",
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
                    Text(if (completed == topics.size && topics.isNotEmpty()) "Chapter completed" else "Mark all topics complete")
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
                    "Part of ${chapter.name}. This checklist records your own completion; topic lesson content is not available here.",
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
