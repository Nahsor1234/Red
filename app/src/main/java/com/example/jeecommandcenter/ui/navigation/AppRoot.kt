package com.example.jeecommandcenter.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.data.LearningRepository
import com.example.jeecommandcenter.ui.components.AppTab
import com.example.jeecommandcenter.ui.screens.*

private enum class SecondaryPage {
    PLANNER, REVISION, SETTINGS, AI_SETTINGS, AI_HUB, AI_TUTOR, ANALYTICS, ASSESSMENT, MISTAKES
}

@Composable
fun AppRoot() {
    val context = LocalContext.current.applicationContext
    val repo = remember { JeeRepository(context) }
    val learning = remember { LearningRepository(context) }
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var secondaryStack by remember { mutableStateOf<List<SecondaryPage>>(emptyList()) }
    val secondaryPage = secondaryStack.lastOrNull()

    fun selectTab(tab: AppTab) {
        secondaryStack = emptyList()
        selectedTab = tab
    }

    fun openPage(page: SecondaryPage) {
        secondaryStack = secondaryStack + page
    }

    fun popPage() {
        if (secondaryStack.isNotEmpty()) secondaryStack = secondaryStack.dropLast(1)
    }

    // Internal pages consume the Android system back event and unwind the same stack
    // used by the in-app back buttons. At the root, the system keeps its normal exit behavior.
    BackHandler(enabled = secondaryStack.isNotEmpty()) { popPage() }

    when (secondaryPage) {
        SecondaryPage.PLANNER -> StudyPlannerScreen(
            repo = repo,
            onBack = ::popPage,
            onOpenRevision = { openPage(SecondaryPage.REVISION) }
        )
        SecondaryPage.REVISION -> RevisionScreen(
            repo = repo,
            onBack = ::popPage,
            onOpenPlanner = { openPage(SecondaryPage.PLANNER) }
        )
        SecondaryPage.SETTINGS -> SettingsScreen(
            onBack = ::popPage,
            onOpenAnalytics = { openPage(SecondaryPage.ANALYTICS) },
            onOpenAiSettings = { openPage(SecondaryPage.AI_SETTINGS) }
        )
        SecondaryPage.AI_SETTINGS -> AiSettingsScreen(
            context = context,
            onBack = ::popPage
        )
        SecondaryPage.AI_HUB -> AiHubScreen(
            context = context,
            onBack = ::popPage,
            onOpenTutor = { openPage(SecondaryPage.AI_TUTOR) },
            onOpenSettings = { openPage(SecondaryPage.AI_SETTINGS) }
        )
        SecondaryPage.AI_TUTOR -> AiTutorScreen(
            context = context,
            jee = repo,
            learning = learning,
            onBack = ::popPage,
            onOpenSettings = { openPage(SecondaryPage.AI_SETTINGS) }
        )
        SecondaryPage.ANALYTICS -> AnalyticsScreen(
            repo = repo,
            learning = learning,
            onBack = ::popPage,
            onOpenTutor = { openPage(SecondaryPage.AI_TUTOR) }
        )
        SecondaryPage.ASSESSMENT -> AssessmentScreen(
            learning = learning,
            jee = repo,
            onBack = ::popPage,
            onOpenMistakes = { openPage(SecondaryPage.MISTAKES) }
        )
        SecondaryPage.MISTAKES -> MistakeBankScreen(
            learning = learning,
            onBack = ::popPage,
            onOpenTutor = { openPage(SecondaryPage.AI_TUTOR) }
        )
        null -> when (selectedTab) {
            AppTab.HOME -> DashboardScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab,
                onAiClick = { openPage(SecondaryPage.AI_HUB) },
                onOpenPlanner = { openPage(SecondaryPage.PLANNER) },
                onOpenRevision = { openPage(SecondaryPage.REVISION) },
                onOpenSettings = { openPage(SecondaryPage.SETTINGS) },
                onOpenAssessment = { openPage(SecondaryPage.ASSESSMENT) },
                onOpenMistakes = { openPage(SecondaryPage.MISTAKES) }
            )
            AppTab.SYLLABUS -> SyllabusScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab,
                onAiClick = { openPage(SecondaryPage.AI_HUB) },
                onOpenPlanner = { openPage(SecondaryPage.PLANNER) },
                onOpenRevision = { openPage(SecondaryPage.REVISION) }
            )
            AppTab.TASKS -> TasksScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab,
                onAiClick = { openPage(SecondaryPage.AI_HUB) }
            )
            AppTab.STATS -> StudyTimerScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab,
                onAiClick = { openPage(SecondaryPage.AI_HUB) }
            )
        }
    }
}
