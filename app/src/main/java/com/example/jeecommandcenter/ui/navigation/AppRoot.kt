package com.example.jeecommandcenter.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.jeecommandcenter.data.AiChatHistoryRepository
import com.example.jeecommandcenter.data.JeeChapter
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.data.LearningRepository
import com.example.jeecommandcenter.data.TestAttemptRecord
import com.example.jeecommandcenter.ui.components.AppTab
import com.example.jeecommandcenter.ui.screens.*

private enum class SecondaryPage { PLANNER, REVISION, SETTINGS, CLOUD_ACCOUNT, AUTH, AI_SETTINGS, AI_HUB, AI_TUTOR, AI_HISTORY, ANALYTICS, ASSESSMENT, ASSESSMENT_HISTORY, ASSESSMENT_RESULT, MISTAKES, CHAPTER_DETAIL, CHAPTER_QUESTIONS }

@Composable
fun AppRoot() {
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
    val secondaryPage = secondaryStack.lastOrNull()
    fun selectTab(tab: AppTab) { secondaryStack = emptyList(); selectedTab = tab }
    fun openPage(page: SecondaryPage) { secondaryStack = secondaryStack + page }
    fun popPage() { if (secondaryStack.isNotEmpty()) secondaryStack = secondaryStack.dropLast(1) }
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

    AnimatedContent(
        targetState = screenKey,
        transitionSpec = {
            fadeIn(tween(130)).togetherWith(fadeOut(tween(90)))
        },
        label = "app-screen-transition"
    ) { key ->
        val targetSecondary = if (key.startsWith("secondary:")) SecondaryPage.valueOf(key.removePrefix("secondary:")) else null
        val targetTab = if (key.startsWith("tab:")) AppTab.valueOf(key.removePrefix("tab:")) else null
        when (targetSecondary) {
            SecondaryPage.PLANNER -> StudyPlannerScreen(repo, ::popPage) { openPage(SecondaryPage.REVISION) }
            SecondaryPage.REVISION -> RevisionScreen(repo, ::popPage) { openPage(SecondaryPage.PLANNER) }
            SecondaryPage.SETTINGS -> SettingsScreen(::popPage, { openPage(SecondaryPage.ANALYTICS) }, { openPage(SecondaryPage.AI_SETTINGS) }) { openPage(SecondaryPage.CLOUD_ACCOUNT) }
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
                onConversationCleared = ::clearAiConversation
            )
            SecondaryPage.AI_HISTORY -> AiHistoryScreen(context, ::popPage, ::openAiConversation)
            SecondaryPage.ANALYTICS -> AnalyticsScreen(repo, learning, ::popPage) { openPage(SecondaryPage.AI_TUTOR) }
            SecondaryPage.ASSESSMENT -> AssessmentScreen(learning, repo, ::popPage, { openPage(SecondaryPage.MISTAKES) }) { openPage(SecondaryPage.ASSESSMENT_HISTORY) }
            SecondaryPage.ASSESSMENT_HISTORY -> AssessmentHistoryScreen(learning, ::popPage) { attempt -> selectedAttempt = attempt; openPage(SecondaryPage.ASSESSMENT_RESULT) }
            SecondaryPage.ASSESSMENT_RESULT -> selectedAttempt?.let { AssessmentResultScreen(it, ::popPage) }
            SecondaryPage.MISTAKES -> MistakeBankScreen(learning, ::popPage) { openPage(SecondaryPage.AI_TUTOR) }
            SecondaryPage.CHAPTER_DETAIL -> selectedChapter?.let { chapter -> ChapterDetailScreen(context, repo, chapter, ::popPage) { openPage(SecondaryPage.CHAPTER_QUESTIONS) } }
            SecondaryPage.CHAPTER_QUESTIONS -> selectedChapter?.let { chapter -> ChapterQuestionPracticeScreen(chapter.id, chapter.name, ::popPage) }
            null -> when (targetTab ?: AppTab.HOME) {
                AppTab.HOME -> DashboardScreen(repo, selectedTab, ::selectTab, { openPage(SecondaryPage.AI_TUTOR) }, { openPage(SecondaryPage.PLANNER) }, { openPage(SecondaryPage.REVISION) }, { openPage(SecondaryPage.SETTINGS) }, { openPage(SecondaryPage.ASSESSMENT) }, { openPage(SecondaryPage.MISTAKES) }, { selectTab(AppTab.STATS) })
                AppTab.SYLLABUS -> SyllabusScreen(repo, selectedTab, ::selectTab, { openPage(SecondaryPage.AI_TUTOR) }, { openPage(SecondaryPage.PLANNER) }, { openPage(SecondaryPage.REVISION) }) { chapter -> selectedChapter = chapter; openPage(SecondaryPage.CHAPTER_DETAIL) }
                AppTab.TASKS -> TasksScreen(repo, selectedTab, ::selectTab) { openPage(SecondaryPage.AI_TUTOR) }
                AppTab.STATS -> StudyTimerScreen(repo, selectedTab, ::selectTab) { openPage(SecondaryPage.AI_TUTOR) }
            }
        }
    }
}
