package com.example.jeecommandcenter.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import com.example.jeecommandcenter.data.AiChatHistoryRepository
import com.example.jeecommandcenter.data.JeeChapter
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.data.LearningRepository
import com.example.jeecommandcenter.data.TestAttemptRecord
import com.example.jeecommandcenter.ui.components.AppTab
import com.example.jeecommandcenter.ui.components.FloatingNavBar
import com.example.jeecommandcenter.ui.screens.*

private enum class SecondaryPage { REVISION, SETTINGS, CLOUD_ACCOUNT, AUTH, AI_SETTINGS, AI_TUTOR, AI_HISTORY, ANALYTICS, ASSESSMENT, ASSESSMENT_HISTORY, ASSESSMENT_RESULT, MISTAKES, CHAPTER_DETAIL, CHAPTER_QUESTIONS, DAILY_LOG, NOTIFICATIONS, NOTIFICATION_SETTINGS }

/** A destination: the top secondary page (if any) over the selected tab, plus stack depth for transition direction. */
private data class Route(val page: SecondaryPage?, val tab: AppTab, val depth: Int) {
    val key: String get() = page?.let { "secondary:" + it.name } ?: ("tab:" + tab.name)
}

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
    val route = Route(secondaryPage, selectedTab, secondaryStack.size)
    // The AI tab is a full-screen chat with its own bottom input, so it hides the bar.
    val showTabBar = secondaryPage == null && selectedTab != AppTab.AI

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showTabBar,
                enter = slideInVertically(tween(240)) { it } + fadeIn(tween(240)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160))
            ) {
                FloatingNavBar(selectedTab, ::selectTab)
            }
        }
    ) { hostPadding ->
        // Tab screens sit above the floating bar, which already pads for the navigation bar.
        Box(Modifier.padding(hostPadding).then(if (showTabBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier)) {
            AnimatedContent(
                targetState = route,
                transitionSpec = {
                    val transition = when {
                        targetState.depth > initialState.depth ->
                            (slideInHorizontally(tween(280)) { it / 5 } + fadeIn(tween(280))) togetherWith fadeOut(tween(160))
                        targetState.depth < initialState.depth ->
                            fadeIn(tween(220)) togetherWith (slideOutHorizontally(tween(260)) { it / 5 } + fadeOut(tween(220)))
                        else -> fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120))
                    }
                    transition.using(SizeTransform(clip = false) { _, _ -> snap() })
                },
                label = "screen"
            ) { screen ->
                saveableStateHolder.SaveableStateProvider(screen.key) {
                when (screen.page) {
                SecondaryPage.REVISION -> RevisionScreen(::popPage)
                SecondaryPage.SETTINGS -> SettingsScreen(onBack = ::popPage, onOpenAnalytics = { openPage(SecondaryPage.ANALYTICS) }, onOpenAiSettings = { openPage(SecondaryPage.AI_SETTINGS) }, onOpenCloudAccount = { openPage(SecondaryPage.CLOUD_ACCOUNT) }, onOpenNotifications = { openPage(SecondaryPage.NOTIFICATION_SETTINGS) })
                SecondaryPage.CLOUD_ACCOUNT -> CloudAccountScreen(context, ::popPage, ::openAuth)
                SecondaryPage.AUTH -> AuthScreen(authMode, ::popPage) { popPage() }
                SecondaryPage.AI_SETTINGS -> AiSettingsScreen(context, ::popPage)
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
                    null -> when (screen.tab) {
                        AppTab.HOME -> DashboardScreen(
                            repo = repo,
                            onTabSelected = ::selectTab,
                            onOpenRevision = { openPage(SecondaryPage.REVISION) },
                            onOpenSettings = { openPage(SecondaryPage.SETTINGS) },
                            onOpenAssessment = { openPage(SecondaryPage.ASSESSMENT) },
                            onOpenMistakes = { openPage(SecondaryPage.MISTAKES) },
                            onOpenStudyTimer = { selectTab(AppTab.STATS) },
                            onOpenDailyLog = { openPage(SecondaryPage.DAILY_LOG) },
                            onOpenNotifications = { openPage(SecondaryPage.NOTIFICATIONS) }
                        )
                        AppTab.SYLLABUS -> SyllabusScreen(
                            repo = repo,
                            onOpenRevision = { openPage(SecondaryPage.REVISION) },
                            onOpenChapter = { chapter -> selectedChapter = chapter; openPage(SecondaryPage.CHAPTER_DETAIL) }
                        )
                        AppTab.AI -> AiTutorScreen(
                            context = context,
                            jee = repo,
                            learning = learning,
                            onBack = { selectTab(AppTab.HOME) },
                            onOpenSettings = { openPage(SecondaryPage.AI_SETTINGS) },
                            onOpenHistory = { openPage(SecondaryPage.AI_HISTORY) },
                            conversationId = activeAiConversationId,
                            onConversationOpened = { id -> activeAiConversationId = id; aiHistory.setActiveConversationId(id) },
                            onConversationCleared = ::clearAiConversation
                        )
                        AppTab.TASKS -> TasksScreen(repo)
                        AppTab.STATS -> StudyTimerScreen(repo)
                    }
                }
                }
            }
        }
    }
}
