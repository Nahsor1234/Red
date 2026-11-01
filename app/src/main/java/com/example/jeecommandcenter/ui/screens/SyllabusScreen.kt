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
            containerColor = BgApp.copy(alpha = 0f),
            bottomBar = { AnimatedBottomNavBar(selectedTab, onTabSelected, visible = selectedTab != AppTab.AI) }
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
                                if (cloudAvailable) "Cloud progress available" else "Using local progress",
                                tint = if (cloudAvailable) PrimaryLight else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            if (cloudAvailable) "Cloud progress" else "Local progress · offline-ready",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!cloudAvailable) {
                            IconButton(onClick = { refreshCloud() }) {
                                Icon(Icons.Filled.Refresh, "Retry cloud connection", tint = PrimaryLight)
                            }
                        }
                        TextButton(onClick = onOpenRevision) {
                            Text("Revision", style = MaterialTheme.typography.labelLarge)
                        }
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
                                "$completedCount of ${allModels.size} chapters complete",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$activeCount active", color = PrimaryLight, style = MaterialTheme.typography.labelLarge)
                            Text("$notStartedCount not started", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearStatBar(avg)
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search chapters by name or number") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search chapters") },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear chapter search")
                            }
                        }
                    },
                    shape = JeeShapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = BgCard,
                        unfocusedContainerColor = BgCard,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = BgCardBorder,
                        focusedPlaceholderColor = TextSecondary,
                        unfocusedPlaceholderColor = TextSecondary,
                        focusedLeadingIconColor = PrimaryLight,
                        unfocusedLeadingIconColor = TextSecondary
                    )
                )

                Spacer(Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { JeeFilterChip("All ${allModels.size}", filter == "All") { filter = "All" } }
                    item { JeeFilterChip("Weak $weakCount", filter == "Weak") { filter = "Weak" } }
                    item { JeeFilterChip("In progress $activeCount", filter == "In progress") { filter = "In progress" } }
                    item { JeeFilterChip("Done $completedCount", filter == "Done") { filter = "Done" } }
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

                    if (models.isEmpty()) {
                        item {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No matching chapters", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Try another search or clear the selected filter.",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(onClick = { query = ""; filter = "All" }) {
                                    Text("Clear search and filters")
                                }
                            }
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
                                        .background(if (progress >= 1f) Success else BgDivider),
                                    Alignment.Center
                                ) {
                                    if (progress >= 1f) {
                                        Icon(
                                            Icons.Filled.Check,
                                            null,
                                            tint = BgCard,
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
                                    Text("Review due", color = Warning, style = MaterialTheme.typography.labelSmall)
                                } else if (weak.contains(chapter.id)) {
                                    Text("Weak", color = PrimaryLight, style = MaterialTheme.typography.labelSmall)
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
