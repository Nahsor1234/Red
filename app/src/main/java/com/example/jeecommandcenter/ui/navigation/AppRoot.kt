package com.example.jeecommandcenter.ui.navigation
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.ui.components.AppTab
import com.example.jeecommandcenter.ui.screens.*
@Composable fun AppRoot(){val repo=remember{JeeRepository(LocalContext.current.applicationContext)};var selectedTab by remember{mutableStateOf(AppTab.HOME)};when(selectedTab){AppTab.HOME->DashboardScreen(repo,selectedTab,{selectedTab=it}){selectedTab=AppTab.TASKS};AppTab.SYLLABUS->SyllabusScreen(repo,selectedTab,{selectedTab=it});AppTab.TASKS->TasksScreen(repo,selectedTab,{selectedTab=it});AppTab.STATS->StudyTimerScreen(repo,selectedTab,{selectedTab=it})}}