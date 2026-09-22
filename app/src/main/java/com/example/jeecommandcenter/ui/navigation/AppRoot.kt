package com.example.jeecommandcenter.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.ui.components.AppTab
import com.example.jeecommandcenter.ui.screens.*

private enum class SecondaryPage {
    PLANNER,
    REVISION
}

@Composable
fun AppRoot() {
    val context = LocalContext.current.applicationContext
    val repo = remember { JeeRepository(context) }
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var secondaryPage by remember { mutableStateOf<SecondaryPage?>(null) }

    fun selectTab(tab: AppTab) {
        secondaryPage = null
        selectedTab = tab
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

        null -> when (selectedTab) {
            AppTab.HOME -> DashboardScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab,
                onFabClick = { selectTab(AppTab.TASKS) },
                onOpenPlanner = { secondaryPage = SecondaryPage.PLANNER },
                onOpenRevision = { secondaryPage = SecondaryPage.REVISION }
            )

            AppTab.SYLLABUS -> SyllabusScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab,
                onOpenPlanner = { secondaryPage = SecondaryPage.PLANNER },
                onOpenRevision = { secondaryPage = SecondaryPage.REVISION }
            )

            AppTab.TASKS -> TasksScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab
            )

            AppTab.STATS -> StudyTimerScreen(
                repo = repo,
                selectedTab = selectedTab,
                onTabSelected = ::selectTab
            )
        }
    }
}
