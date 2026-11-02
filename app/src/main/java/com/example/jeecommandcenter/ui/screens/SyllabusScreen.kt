package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SyllabusScreen(
    repo: JeeRepository,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onAiClick: () -> Unit = {},
    onOpenRevision: () -> Unit = {},
    onOpenChapter: (JeeChapter) -> Unit = {}
) {
    var subject by remember { mutableStateOf("Physics") }
    var filter by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
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

    val allModels = if (cloudAvailable) {
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

    val models = allModels.filter { (chapter, progress) ->
        val matchesStatus = when (filter) {
            "In progress" -> progress > 0f && progress < 1f
            "Done" -> progress >= 1f
            "Weak" -> weak.contains(chapter.id)
            else -> true
        }
        matchesStatus && (query.isBlank() || chapter.name.contains(query.trim(), ignoreCase = true) ||
            chapter.number.toString() == query.trim())
    }

    // Keep the subject summary stable while the user searches or changes a filter.
    val avg = if (allModels.isEmpty()) 0f else allModels.map { it.second }.average().toFloat()
    val completedCount = allModels.count { it.second >= 1f }
    val activeCount = allModels.count { it.second > 0f && it.second < 1f }
    val notStartedCount = allModels.count { it.second <= 0f }
    val weakCount = allModels.count { weak.contains(it.first.id) }
    val attention = allModels.firstOrNull { weak.contains(it.first.id) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BgApp,
            bottomBar = { AnimatedBottomNavBar(selectedTab, onTabSelected, visible = selectedTab != AppTab.AI) }
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "syllabus-header") {
                    JeePageHeader(
                        title = "Syllabus",
                        eyebrow = "JEE 2027 · LEARNING",
                        subtitle = "Build steady progress, one chapter at a time",
                        trailing = {
                            TextButton(onClick = onOpenRevision) {
                                Text("Notes", color = PrimaryLight, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    )
                }
                item(key = "syllabus-search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Search chapters by name or number") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear chapter search")
                            }
                        },
                        shape = JeeShapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedContainerColor = BgCard, unfocusedContainerColor = BgCard,
                            focusedBorderColor = Primary, unfocusedBorderColor = BgCardBorder,
                            focusedPlaceholderColor = TextSecondary, unfocusedPlaceholderColor = TextSecondary,
                            focusedLeadingIconColor = Primary, unfocusedLeadingIconColor = TextSecondary
                        )
                    )
                }
                item(key = "syllabus-subject") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("SUBJECT", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Physics", "Chemistry", "Mathematics"), key = { it }) { value ->
                                JeeFilterChip(value, value == subject) {
                                    subject = value
                                    filter = "All"
                                    query = ""
                                }
                            }
                        }
                    }
                }
                item(key = "syllabus-progress") {
                    JeeCard(modifier = Modifier.animateContentSize(animationSpec = tween(JeeMotion.layoutMillis))) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("$subject progress", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                                AnimatedContent(targetState = (avg * 100).toInt(), label = "syllabus-subject-progress") { percent ->
                                    Text("$percent%", style = MaterialTheme.typography.headlineMedium)
                                }
                                Text("$completedCount of ${allModels.size} chapters complete", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("$activeCount in progress", color = PrimaryLight, style = MaterialTheme.typography.labelMedium)
                                Text("$notStartedCount not started", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearStatBar(avg, height = 6.dp)
                        Spacer(Modifier.height(8.dp))
                        Text("$weakCount weak · $completedCount complete", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                item(key = "syllabus-provenance") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (cloudAvailable) Icons.Filled.Cloud else Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = if (cloudAvailable) Primary else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                cloudAvailable -> "Cloud progress · local progress remains available"
                                cloudError != null -> "Cloud unavailable · showing local progress"
                                else -> "Local progress · offline-ready"
                            },
                            modifier = Modifier.weight(1f), color = TextSecondary, style = MaterialTheme.typography.bodySmall
                        )
                        if (!cloudAvailable) {
                            IconButton(onClick = { refreshCloud() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Retry cloud connection", tint = PrimaryLight)
                            }
                        }
                    }
                }
                item(key = "syllabus-filters") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { JeeFilterChip("All ${allModels.size}", filter == "All") { filter = "All" } }
                        item { JeeFilterChip("Weak $weakCount", filter == "Weak") { filter = "Weak" } }
                        item { JeeFilterChip("In progress $activeCount", filter == "In progress") { filter = "In progress" } }
                        item { JeeFilterChip("Done $completedCount", filter == "Done") { filter = "Done" } }
                    }
                }
                item(key = "syllabus-chapter-heading") {
                    Column {
                        SectionHeader(title = "Chapters")
                        Text("${models.size} shown", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (filter == "All" && query.isBlank() && attention != null) {
                    item(key = "review-attention") {
                        JeeCard {
                            Text("NEXT TO REVIEW", color = PrimaryLight, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(3.dp))
                            Text(attention.first.name, style = MaterialTheme.typography.titleSmall)
                            Text("Low confidence or incomplete progress", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            LinearStatBar(attention.second, height = 4.dp)
                        }
                    }
                }
                if (models.isEmpty()) {
                    item(key = "empty-chapters") {
                        Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (allModels.isEmpty()) "No chapters available" else "No matching chapters", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (allModels.isEmpty()) "No chapters were returned from the current source for ${subject.lowercase()}."
                                else "Try another search or clear the selected filter.",
                                color = TextSecondary, style = MaterialTheme.typography.bodyMedium
                            )
                            if (allModels.isNotEmpty()) TextButton(onClick = { query = ""; filter = "All" }) { Text("Clear search and filters") }
                        }
                    }
                }
                items(models, key = { it.first.id }) { (chapter, progress) ->
                    Column(Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(JeeMotion.standardMillis))) {
                        Row(
                            Modifier.fillMaxWidth().premiumClick { onOpenChapter(chapter) }
                                .heightIn(min = 76.dp).padding(horizontal = 4.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(28.dp).clip(CircleShape).background(if (progress >= 1f) Success else BgCardAlt),
                                contentAlignment = Alignment.Center
                            ) {
                                if (progress >= 1f) Icon(Icons.Filled.Check, "Completed", tint = BgCard, modifier = Modifier.size(16.dp))
                                else Text("${chapter.number}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(chapter.name, color = TextPrimary, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedContent(targetState = (progress * 100).toInt(), label = "syllabus-chapter-progress") { percent ->
                                        Text("$percent%", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                                    }
                                    Text("${chapter.estimatedMinutes} min", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                                }
                                LinearStatBar(progress, height = 4.dp)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (due.contains(chapter.id)) Text("Review due", color = Warning, style = MaterialTheme.typography.labelSmall)
                                else if (weak.contains(chapter.id)) Text("Weak", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Icon(Icons.Filled.ChevronRight, "Open chapter", tint = TextMuted, modifier = Modifier.size(20.dp))
                            }
                        }
                        HorizontalDivider(Modifier.padding(start = 40.dp), color = BgDivider.copy(alpha = .65f), thickness = 0.7.dp)
                    }
                }
            }
        }
    }
}
