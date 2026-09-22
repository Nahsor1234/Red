package com.example.jeecommandcenter.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.data.JeeChapter
import com.example.jeecommandcenter.data.LearningRepository
import com.example.jeecommandcenter.ui.components.AppTab
import com.example.jeecommandcenter.ui.screens.*

private enum class SecondaryPage { PLANNER, REVISION, SETTINGS, AI_SETTINGS, AI_HUB, AI_TUTOR, ANALYTICS, ASSESSMENT, MISTAKES, CHAPTER_DETAIL }

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

    when (secondaryPage) {
        SecondaryPage.PLANNER -> StudyPlannerScreen(repo, ::popPage) { openPage(SecondaryPage.REVISION) }
        SecondaryPage.REVISION -> RevisionScreen(repo, ::popPage) { openPage(SecondaryPage.PLANNER) }
        SecondaryPage.SETTINGS -> SettingsScreen(::popPage, { openPage(SecondaryPage.ANALYTICS) }, { openPage(SecondaryPage.AI_SETTINGS) })
        SecondaryPage.AI_SETTINGS -> AiSettingsScreen(context, ::popPage)
        SecondaryPage.AI_HUB -> AiHubScreen(context, ::popPage, { openPage(SecondaryPage.AI_TUTOR) }, { openPage(SecondaryPage.AI_SETTINGS) })
        SecondaryPage.AI_TUTOR -> AiTutorScreen(context, repo, learning, ::popPage) { openPage(SecondaryPage.AI_SETTINGS) }
        SecondaryPage.ANALYTICS -> AnalyticsScreen(repo, learning, ::popPage) { openPage(SecondaryPage.AI_TUTOR) }
        SecondaryPage.ASSESSMENT -> AssessmentScreen(learning, repo, ::popPage) { openPage(SecondaryPage.MISTAKES) }
        SecondaryPage.MISTAKES -> MistakeBankScreen(learning, ::popPage) { openPage(SecondaryPage.AI_TUTOR) }
        SecondaryPage.CHAPTER_DETAIL -> selectedChapter?.let { ChapterDetailScreen(context, repo, it, ::popPage) }
        null -> when (selectedTab) {
            AppTab.HOME -> DashboardScreen(repo, selectedTab, ::selectTab, { openPage(SecondaryPage.AI_HUB) }, { openPage(SecondaryPage.PLANNER) }, { openPage(SecondaryPage.REVISION) }, { openPage(SecondaryPage.SETTINGS) }, { openPage(SecondaryPage.ASSESSMENT) }, { openPage(SecondaryPage.MISTAKES) })
            AppTab.SYLLABUS -> SyllabusScreen(repo, selectedTab, ::selectTab, { openPage(SecondaryPage.AI_HUB) }, { openPage(SecondaryPage.PLANNER) }, { openPage(SecondaryPage.REVISION) }) { chapter -> selectedChapter = chapter; openPage(SecondaryPage.CHAPTER_DETAIL) }
            AppTab.TASKS -> TasksScreen(repo, selectedTab, ::selectTab) { openPage(SecondaryPage.AI_HUB) }
            AppTab.STATS -> StudyTimerScreen(repo, selectedTab, ::selectTab) { openPage(SecondaryPage.AI_HUB) }
        }
    }
}
