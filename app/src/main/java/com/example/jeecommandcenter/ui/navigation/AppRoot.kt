package com.example.jeecommandcenter.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import com.example.jeecommandcenter.data.AiChatHistoryRepository
import com.example.jeecommandcenter.data.JeeCatalog
import com.example.jeecommandcenter.data.JeeChapter
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.data.LearningRepository
import com.example.jeecommandcenter.data.TestAttemptRecord
import com.example.jeecommandcenter.ui.components.AppTab
import com.example.jeecommandcenter.ui.screens.*

private enum class SecondaryPage { PLANNER, REVISION, SETTINGS, CLOUD_ACCOUNT, AUTH, AI_SETTINGS, AI_HUB, AI_TUTOR, AI_HISTORY, ANALYTICS, ASSESSMENT, ASSESSMENT_HISTORY, ASSESSMENT_RESULT, MISTAKES, CHAPTER_DETAIL, CHAPTER_QUESTIONS, DAILY_LOG, NOTIFICATIONS, NOTIFICATION_SETTINGS }

@Composable
fun AppRoot(initialTarget: String? = null, launchRequestId: Int = 0) {
    val context = LocalContext.current.applicationContext
    val repo = remember { JeeRepository(context) }
    val learning = remember { LearningRepository(context) }
    val aiHistory = remember { AiChatHistoryRepository(context) }
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var secondaryStack by remember { mutableStateOf<List<SecondaryPage>>(emptyList()) }
    var selectedChapter by remember { mutableStateOf<JeeChapter?>(null) }
    var selectedAttempt by remember { mutableStateOf<TestAttemptRecord?>(null) }
    var authMode by remember { mutableStateOf(CloudAuthMode.SIGN_IN) }
    var activeAiConversationId by remember { mutableStateOf(aiHistory.getActiveConversationId()?.takeIf { aiHistory.getConversation(it) != null }) }
    var pendingAiQuestionContext by remember { mutableStateOf<String?>(null) }
    var aiQuestionRequestId by remember { mutableIntStateOf(0) }
    val secondaryPage = secondaryStack.lastOrNull()
    val saveableStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(initialTarget, launchRequestId) {
        when (initialTarget) {
            "daily_log", "daily_progress" -> secondaryStack = listOf(SecondaryPage.DAILY_LOG)
            "notifications" -> secondaryStack = listOf(SecondaryPage.NOTIFICATIONS)
            "timer" -> { secondaryStack = emptyList(); selectedTab = AppTab.STATS }
            "home" -> { secondaryStack = emptyList(); selectedTab = AppTab.HOME }
        }
    }

    fun selectTab(tab: AppTab) { secondaryStack = emptyList(); selectedTab = tab }
    fun openPage(page: SecondaryPage) { secondaryStack = secondaryStack + page }
    fun popPage() { if (secondaryStack.isNotEmpty()) { secondaryStack = secondaryStack.dropLast(1) } }
    fun openAiTutorForQuestion(questionContext: String) {
        pendingAiQuestionContext = questionContext
        aiQuestionRequestId += 1
        openPage(SecondaryPage.AI_TUTOR)
    }
    fun openAuth(mode: CloudAuthMode) { authMode = mode; openPage(SecondaryPage.AUTH) }
    fun openAiConversation(id: Long) {
        activeAiConversationId = id
        aiHistory.setActiveConversationId(id)
        secondaryStack = when (secondaryStack.lastOrNull()) {
            SecondaryPage.AI_HISTORY -> secondaryStack.dropLast(1)
            SecondaryPage.AI_TUTOR -> secondaryStack
            else -> secondaryStack + SecondaryPage.AI_TUTOR
        }
    }

    fun clearAiConversation() {
        activeAiConversationId = null
        aiHistory.setActiveConversationId(null)
    }
    BackHandler(enabled = secondaryStack.isNotEmpty()) { popPage() }
    val screenKey = secondaryPage?.let { "secondary:" + it.name } ?: ("tab:" + selectedTab.name)

    key(screenKey) {
        saveableStateHolder.SaveableStateProvider(screenKey) {
            val key = screenKey

            val targetSecondary = if (key.startsWith("secondary:")) SecondaryPage.valueOf(key.removePrefix("secondary:")) else null
            val targetTab = if (key.startsWith("tab:")) AppTab.valueOf(key.removePrefix("tab:")) else null
            when (targetSecondary) {
                SecondaryPage.PLANNER -> StudyPlannerScreen(
                    repo = repo,
                    onBack = ::popPage,
                    onOpenRevision = { openPage(SecondaryPage.REVISION) },
                    onOpenChapter = { chapterId ->
                        JeeCatalog.find(chapterId)?.let { chapter ->
                            selectedChapter = chapter
                            openPage(SecondaryPage.CHAPTER_DETAIL)
                        } ?: openPage(SecondaryPage.REVISION)
                    }
                )
                SecondaryPage.REVISION -> RevisionScreen(repo, ::popPage) { openPage(SecondaryPage.PLANNER) }
                SecondaryPage.SETTINGS -> SettingsScreen(onBack = ::popPage, onOpenAnalytics = { openPage(SecondaryPage.ANALYTICS) }, onOpenAiSettings = { openPage(SecondaryPage.AI_SETTINGS) }, onOpenCloudAccount = { openPage(SecondaryPage.CLOUD_ACCOUNT) }, onOpenNotifications = { openPage(SecondaryPage.NOTIFICATION_SETTINGS) })
                SecondaryPage.CLOUD_ACCOUNT -> CloudAccountScreen(context, ::popPage, ::openAuth)
                SecondaryPage.AUTH -> AuthScreen(authMode, ::popPage) { popPage() }
                SecondaryPage.AI_SETTINGS -> AiSettingsScreen(context, ::popPage)
                SecondaryPage.AI_HUB -> AiHubScreen(context, ::popPage, { openPage(SecondaryPage.AI_TUTOR) }, { openPage(SecondaryPage.AI_SETTINGS) })
                SecondaryPage.AI_TUTOR -> AiTutorScreen(
                    context = context,
                    jee = repo,
                    learning = learning,
                    onBack = ::popPage,
                    onOpenSettings = { openPage(SecondaryPage.AI_SETTINGS) },
                    onOpenHistory = { openPage(SecondaryPage.AI_HISTORY) },
                    conversationId = activeAiConversationId,
                    onConversationOpened = { id -> activeAiConversationId = id; aiHistory.setActiveConversationId(id) },
                    onConversationCleared = ::clearAiConversation,
                    initialQuestionContext = pendingAiQuestionContext,
                    initialQuestionRequestId = aiQuestionRequestId,
                    onInitialQuestionContextConsumed = { pendingAiQuestionContext = null }
                )
                SecondaryPage.AI_HISTORY -> AiHistoryScreen(context, ::popPage, ::openAiConversation)
                SecondaryPage.ANALYTICS -> AnalyticsScreen(repo, learning, ::popPage) { openPage(SecondaryPage.AI_TUTOR) }
                SecondaryPage.ASSESSMENT -> AssessmentScreen(learning, repo, ::popPage, { openPage(SecondaryPage.MISTAKES) }) { openPage(SecondaryPage.ASSESSMENT_HISTORY) }
                SecondaryPage.ASSESSMENT_HISTORY -> AssessmentHistoryScreen(learning, ::popPage) { attempt -> selectedAttempt = attempt; openPage(SecondaryPage.ASSESSMENT_RESULT) }
                SecondaryPage.ASSESSMENT_RESULT -> selectedAttempt?.let { AssessmentResultScreen(it, ::popPage) }
                SecondaryPage.MISTAKES -> MistakeBankScreen(learning, ::popPage) { openPage(SecondaryPage.AI_TUTOR) }
                SecondaryPage.CHAPTER_DETAIL -> selectedChapter?.let { chapter -> ChapterDetailScreen(context, repo, chapter, ::popPage) { openPage(SecondaryPage.CHAPTER_QUESTIONS) } }
                SecondaryPage.CHAPTER_QUESTIONS -> selectedChapter?.let { chapter ->
                    ChapterQuestionPracticeScreen(
                        chapterId = chapter.id,
                        chapterName = chapter.name,
                        onBack = ::popPage,
                        onOpenAiTutor = ::openAiTutorForQuestion
                    )
                }
                SecondaryPage.DAILY_LOG -> DailyLogScreen(::popPage)
                SecondaryPage.NOTIFICATIONS -> NotificationCenterScreen(::popPage) { openPage(SecondaryPage.NOTIFICATION_SETTINGS) }
                SecondaryPage.NOTIFICATION_SETTINGS -> NotificationSettingsScreen(::popPage)
                null -> when (targetTab ?: AppTab.HOME) {
                    AppTab.HOME -> DashboardScreen(repo, selectedTab, ::selectTab, { openPage(SecondaryPage.AI_TUTOR) }, { openPage(SecondaryPage.PLANNER) }, { openPage(SecondaryPage.REVISION) }, { openPage(SecondaryPage.SETTINGS) }, { openPage(SecondaryPage.ASSESSMENT) }, { openPage(SecondaryPage.MISTAKES) }, { selectTab(AppTab.STATS) }, { openPage(SecondaryPage.DAILY_LOG) }, { openPage(SecondaryPage.NOTIFICATIONS) })
                    AppTab.SYLLABUS -> SyllabusScreen(repo, selectedTab, ::selectTab, { openPage(SecondaryPage.AI_TUTOR) }, { openPage(SecondaryPage.PLANNER) }, { openPage(SecondaryPage.REVISION) }) { chapter -> selectedChapter = chapter; openPage(SecondaryPage.CHAPTER_DETAIL) }
                    AppTab.AI -> AiTutorScreen(
                        context = context,
                        jee = repo,
                        learning = learning,
                        onBack = { selectTab(AppTab.HOME) },
                        onOpenSettings = { openPage(SecondaryPage.AI_SETTINGS) },
                        onOpenHistory = { openPage(SecondaryPage.AI_HISTORY) },
                        conversationId = activeAiConversationId,
                        onConversationOpened = { id -> activeAiConversationId = id; aiHistory.setActiveConversationId(id) },
                        onConversationCleared = ::clearAiConversation,
                        showMainNavigation = true,
                        selectedTab = AppTab.AI,
                        onTabSelected = ::selectTab
                    )
                    AppTab.TASKS -> TasksScreen(repo, selectedTab, ::selectTab) { openPage(SecondaryPage.AI_TUTOR) }
                    AppTab.STATS -> StudyTimerScreen(repo, selectedTab, ::selectTab) { openPage(SecondaryPage.AI_TUTOR) }
                }
            }
        }
    }
}
