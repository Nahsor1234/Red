package com.example.jeecommandcenter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.jeecommandcenter.ui.components.AppTab
import com.example.jeecommandcenter.ui.screens.DashboardScreen
import com.example.jeecommandcenter.ui.screens.StudyTimerScreen
import com.example.jeecommandcenter.ui.screens.SyllabusScreen
import com.example.jeecommandcenter.ui.screens.TasksScreen

@Composable
fun AppRoot() {
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }

    when (selectedTab) {
        AppTab.HOME -> DashboardScreen(selectedTab, { selectedTab = it }, {})
        AppTab.SYLLABUS -> SyllabusScreen(selectedTab, { selectedTab = it }, {})
        AppTab.TASKS -> TasksScreen(selectedTab, { selectedTab = it }, {})
        AppTab.STATS -> StudyTimerScreen(selectedTab, { selectedTab = it }, {})
    }
}