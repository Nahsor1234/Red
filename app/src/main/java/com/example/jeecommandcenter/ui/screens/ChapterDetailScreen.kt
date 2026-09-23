package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch

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
    var cloudUserId by remember { mutableStateOf<String?>(null) }
    var cloudReady by remember { mutableStateOf(false) }
    var questionCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(chapter.id) {
        runCatching {
            val user = cloud.ensureSession()
            cloudUserId = user.id
            val cloudTopics = cloud.topics(chapter.id)
            val progress = cloud.topicProgressForUser(user.id).associateBy { it.topicId }

            if (cloudTopics.isNotEmpty()) {
                topics = cloudTopics.map {
                    ChapterTopic(
                        id = it.id,
                        chapterId = it.chapterId,
                        title = it.title,
                        completed = progress[it.id]?.completed == true
                    )
                }
                cloudReady = true
            }

            questionCount = cloud.questions(chapter.id).size
        }.onFailure {
            cloudReady = false
        }
    }

    val completed = topics.count { it.completed }
    val progress by animateFloatAsState(
        if (topics.isEmpty()) 0f else completed.toFloat() / topics.size,
        label = "topic-progress"
    )

    Scaffold(
        containerColor = BgApp,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(chapter.name)
                        Text(chapter.subject, color = TextSecondary, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgApp)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(BgCardAlt)
                        .border(
                            JeeSurfaceTokens.borderWidth,
                            BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha),
                            JeeShapes.medium
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween
                    ) {
                        Text("Topic progress", style = MaterialTheme.typography.titleMedium)
                        Text("${completed} / ${topics.size}", color = PrimaryLight)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Primary,
                        trackColor = BgDivider
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (cloudReady) {
                            "Synced to Supabase. Topic completion updates cloud progress."
                        } else {
                            "Local fallback active. Cloud sync resumes when available."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            item {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (questionCount > 0) PrimarySoft else BgCard,
                    border = BorderStroke(
                        1.dp,
                        if (questionCount > 0) Primary.copy(alpha = .7f)
                        else BgCardBorder.copy(alpha = .8f)
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            null,
                            tint = PrimaryLight,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Practice questions", fontWeight = FontWeight.Medium)
                            Text(
                                if (questionCount > 0) {
                                    "${questionCount} questions available"
                                } else {
                                    "No questions seeded for this chapter yet"
                                },
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        TextButton(
                            enabled = questionCount > 0,
                            onClick = onPracticeQuestions
                        ) {
                            Text("Practice")
                        }
                    }
                }
            }

            item {
                Text(
                    "Topics",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(topics, key = { it.id }) { topic ->
                val checkColor by animateColorAsState(
                    if (topic.completed) AccentGreen else BgCard,
                    label = "topic-color"
                )

                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(BgCard)
                        .border(
                            1.dp,
                            BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha),
                            RoundedCornerShape(15.dp)
                        )
                        .clickable {
                            val next = !topic.completed
                            topicsRepo.setCompleted(chapter.id, topic.id, next)
                            topics = topics.map {
                                if (it.id == topic.id) it.copy(completed = next) else it
                            }

                            val chapterProgress =
                                if (topics.isEmpty()) 0f
                                else topics.count { it.completed }.toFloat() / topics.size

                            repo.setChapterProgress(
                                chapter.subject,
                                chapter.number,
                                chapterProgress
                            )

                            cloudUserId?.let { uid ->
                                scope.launch {
                                    runCatching {
                                        cloud.setTopicCompleted(uid, topic.id, next)
                                        cloud.setChapterProgress(
                                            uid,
                                            chapter.id,
                                            chapterProgress
                                        )
                                    }
                                }
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(checkColor),
                        Alignment.Center
                    ) {
                        if (topic.completed) {
                            Icon(
                                Icons.Filled.Check,
                                null,
                                tint = AccentGreenDark,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        topic.title,
                        color = if (topic.completed) TextSecondary else TextOnCard,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        topics.forEach {
                            topicsRepo.setCompleted(chapter.id, it.id, true)
                        }
                        topics = topics.map { it.copy(completed = true) }
                        repo.setChapterProgress(chapter.subject, chapter.number, 1f)

                        cloudUserId?.let { uid ->
                            scope.launch {
                                runCatching {
                                    topics.forEach {
                                        cloud.setTopicCompleted(uid, it.id, true)
                                    }
                                    cloud.setChapterProgress(uid, chapter.id, 1f)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark chapter complete")
                }
            }
        }
    }
}
