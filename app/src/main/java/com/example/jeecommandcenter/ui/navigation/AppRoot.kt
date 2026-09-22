package com.example.jeecommandcenter.ui.navigation

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
    var secondaryPage by remember { mutableStateOf<SecondaryPage?>(null) }

    fun selectTab(tab: AppTab) {
        secondaryPage = null
        selectedTab = tab
    }

    fun openAiHub() {
        secondaryPage = SecondaryPage.AI_HUB
    }

    when (secondaryPage) {
        SecondaryPage.PLANNER -> StudyPlannerScreen(
            repo = repo,
            onBack = { secondaryPage = null },
            onOpenRevision = { secondaryPage = SecondaryPage.REVISION }
        )
        SecondaryPage.REVISION -> RevisionScreen(
            repo = repo,
            onBack = { secondaryPage = null },
            onOpenPlanner = { secondaryPage = SecondaryPage.PLANNER }
        )
        SecondaryPage.SETTINGS -> SettingsScreen(
            onBack = { secondaryPage = null },
            onOpenAnalytics = { secondaryPage = SecondaryPage.ANALYTICS },
            onOpenAssessment = { secondaryPage = SecondaryPage.ASSESSMENT },
            onOpenMistakes = { secondaryPage = SecondaryPage.MISTAKES },
            onOpenAiSettings = { secondaryPage = SecondaryPage.AI_SETTINGS }
        )
        SecondaryPage.AI_SETTINGS -> AiSettingsScreen(
            context = context,
            onBack = { secondaryPage = SecondaryPage.SETTINGS }
        )
        SecondaryPage.AI_HUB -> AiHubScreen(
            context = context,
            onBack = { secondaryPage = null },
            onOpenTutor = { secondaryPage = SecondaryPage.AI_TUTOR },
            onOpenSettings = { secondaryPage = SecondaryPage.AI_SETTINGS }
        )
        SecondaryPage.AI_TUTOR -> AiTutorScreen(
            context = context,
            jee = repo,
            learning = learning,
            onBack = { secondaryPage = SecondaryPage.AI_HUB },
            onOpenSettings = { secondaryPage = SecondaryPage.AI_SETTINGS }
        )
        SecondaryPage.ANALYTICS -> AnalyticsScreen(
            repo = repo,
            learning = learning,
            onBack = { secondaryPage = SecondaryPage.SETTINGS },
            onOpenTutor = { secondaryPage = SecondaryPage.AI_TUTOR }
        )
        SecondaryPage.ASSESSMENT -> AssessmentScreen(
            learning = learning,
            jee = repo,
            onBack = { secondaryPage = SecondaryPage.SETTINGS },
            onOpenMistakes = { secondaryPage = SecondaryPage.MISTAKES }
        )
        SecondaryPage.MISTAKES -> MistakeBankScreen(
            learning = learning,
            onBack = { secondaryPage = SecondaryPage.SETTINGS },
            onOpenTutor = { secondaryPage = SecondaryPage.AI_TUTOR }
        )
        null -> when (selectedTab) {
            AppTab.HOME -> DashboardScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab,
                onAiClick = ::openAiHub,
                onOpenPlanner = { secondaryPage = SecondaryPage.PLANNER },
                onOpenRevision = { secondaryPage = SecondaryPage.REVISION },
                onOpenSettings = { secondaryPage = SecondaryPage.SETTINGS }
            )
            AppTab.SYLLABUS -> SyllabusScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab,
                onAiClick = ::openAiHub,
                onOpenPlanner = { secondaryPage = SecondaryPage.PLANNER },
                onOpenRevision = { secondaryPage = SecondaryPage.REVISION }
            )
            AppTab.TASKS -> TasksScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab,
                onAiClick = ::openAiHub
            )
            AppTab.STATS -> StudyTimerScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab,
                onAiClick = ::openAiHub
            )
        }
    }
}
