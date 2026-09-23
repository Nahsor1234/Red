package com.example.jeecommandcenter.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.data.JeeChapter
import com.example.jeecommandcenter.data.LearningRepository
import com.example.jeecommandcenter.ui.components.AppTab
import com.example.jeecommandcenter.ui.screens.*

private enum class SecondaryPage { PLANNER, REVISION, SETTINGS, AI_SETTINGS, AI_HUB, AI_TUTOR, AI_HISTORY, ANALYTICS, ASSESSMENT, ASSESSMENT_HISTORY, MISTAKES, CHAPTER_DETAIL }

@Composable
fun AppRoot() {
    val context = LocalContext.current.applicationContext
    val repo = remember { JeeRepository(context) }
    val learning = remember { LearningRepository(context) }
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var secondaryStack by remember { mutableStateOf<List<SecondaryPage>>(emptyList()) }
    var selectedChapter by remember { mutableStateOf<JeeChapter?>(null) }
    val secondaryPage = secondaryStack.lastOrNull()
    fun selectTab(tab: AppTab) { secondaryStack = emptyList(); selectedTab = tab }
    fun openPage(page: SecondaryPage) { secondaryStack = secondaryStack + page }
    fun popPage() { if (secondaryStack.isNotEmpty()) secondaryStack = secondaryStack.dropLast(1) }
    BackHandler(enabled = secondaryStack.isNotEmpty()) { popPage() }

    val screenKey = secondaryPage?.let { "secondary:" + it.name } ?: ("tab:" + selectedTab.name)

    AnimatedContent(
        targetState = screenKey,
        transitionSpec = {
            (fadeIn(tween(210)) + slideInHorizontally(tween(210), initialOffsetX = { it / 28 }))
                .togetherWith(fadeOut(tween(170)) + slideOutHorizontally(tween(170), targetOffsetX = { -it / 28 }))
        },
        label = "app-screen-transition"
    ) { key ->
        val targetSecondary = if (key.startsWith("secondary:")) {
            SecondaryPage.valueOf(key.removePrefix("secondary:"))
        } else null
        val targetTab = if (key.startsWith("tab:")) {
            AppTab.valueOf(key.removePrefix("tab:"))
        } else null

        when (targetSecondary) {
            SecondaryPage.PLANNER -> StudyPlannerScreen(repo, ::popPage) { openPage(SecondaryPage.REVISION) }
            SecondaryPage.REVISION -> RevisionScreen(repo, ::popPage) { openPage(SecondaryPage.PLANNER) }
            SecondaryPage.SETTINGS -> SettingsScreen(::popPage, { openPage(SecondaryPage.ANALYTICS) }, { openPage(SecondaryPage.AI_SETTINGS) })
            SecondaryPage.AI_SETTINGS -> AiSettingsScreen(context, ::popPage)
            SecondaryPage.AI_HUB -> AiHubScreen(context, ::popPage, { openPage(SecondaryPage.AI_TUTOR) }, { openPage(SecondaryPage.AI_SETTINGS) })
            SecondaryPage.AI_TUTOR -> AiTutorScreen(context, repo, learning, ::popPage, { openPage(SecondaryPage.AI_SETTINGS) }) { openPage(SecondaryPage.AI_HISTORY) }
            SecondaryPage.AI_HISTORY -> AiHistoryScreen(context, ::popPage)
            SecondaryPage.ANALYTICS -> AnalyticsScreen(repo, learning, ::popPage) { openPage(SecondaryPage.AI_TUTOR) }
            SecondaryPage.ASSESSMENT -> AssessmentScreen(learning, repo, ::popPage, { openPage(SecondaryPage.MISTAKES) }) { openPage(SecondaryPage.ASSESSMENT_HISTORY) }
            SecondaryPage.ASSESSMENT_HISTORY -> AssessmentHistoryScreen(learning, ::popPage)
            SecondaryPage.MISTAKES -> MistakeBankScreen(learning, ::popPage) { openPage(SecondaryPage.AI_TUTOR) }
            SecondaryPage.CHAPTER_DETAIL -> selectedChapter?.let { ChapterDetailScreen(context, repo, it, ::popPage) }
            null -> when (targetTab ?: AppTab.HOME) {
                // The AI destination is a direct entry point into the actual coaching workspace.
                AppTab.HOME -> DashboardScreen(repo, selectedTab, ::selectTab, { openPage(SecondaryPage.AI_TUTOR) }, { openPage(SecondaryPage.PLANNER) }, { openPage(SecondaryPage.REVISION) }, { openPage(SecondaryPage.SETTINGS) }, { openPage(SecondaryPage.ASSESSMENT) }, { openPage(SecondaryPage.MISTAKES) }, { selectTab(AppTab.STATS) })
                AppTab.SYLLABUS -> SyllabusScreen(repo, selectedTab, ::selectTab, { openPage(SecondaryPage.AI_TUTOR) }, { openPage(SecondaryPage.PLANNER) }, { openPage(SecondaryPage.REVISION) }) { chapter -> selectedChapter = chapter; openPage(SecondaryPage.CHAPTER_DETAIL) }
                AppTab.TASKS -> TasksScreen(repo, selectedTab, ::selectTab) { openPage(SecondaryPage.AI_TUTOR) }
                AppTab.STATS -> StudyTimerScreen(repo, selectedTab, ::selectTab) { openPage(SecondaryPage.AI_TUTOR) }
            }
        }
    }
}
