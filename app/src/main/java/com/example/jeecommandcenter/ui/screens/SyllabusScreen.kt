package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

data class Chapter(val number: Int, val name: String, val progress: Float, val progressColor: Color)

private val physicsChapters = listOf(
    Chapter(1, "Units and measurements", 1.0f, AccentGreen),
    Chapter(2, "Motion in a straight line", 0.8f, AccentGreen),
    Chapter(3, "Motion in a plane", 0.4f, AccentAmber),
    Chapter(4, "Laws of motion", 0f, BgDivider),
    Chapter(5, "Work, energy and power", 0.2f, TextPriorityBadge),
    Chapter(6, "System of particles", 0f, BgDivider),
    Chapter(7, "Rotational motion", 0f, BgDivider),
    Chapter(8, "Gravitation", 0f, BgDivider),
    Chapter(9, "Mechanical properties of solids", 0f, BgDivider)
)

@Composable
fun SyllabusScreen(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, onFabClick: () -> Unit = {}) {
    var selectedSubject by remember { mutableStateOf("Physics") }
    var selectedFilter by remember { mutableStateOf("All") }
    val subjects = listOf("Physics", "Chemistry", "Mathematics")
    val filters = listOf("All", "Not started", "In progress", "Completed")
    Scaffold(containerColor = BgApp, bottomBar = { BottomNavBar(selectedTab, onTabSelected, onFabClick) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Syllabus", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp))
                Icon(Icons.Filled.Search, "Search", tint = TextSecondary)
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjects) { subject -> FilterChip(subject, subject == selectedSubject) { selectedSubject = subject } }
            }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCardAlt).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(AccentBlueSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Science, null, tint = AccentBlueLight, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(selectedSubject, style = MaterialTheme.typography.titleLarge)
                        Text("18 / 33 chapters", color = TextMuted, fontSize = 12.sp)
                    }
                    Text("55%", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp))
                }
                Spacer(Modifier.height(10.dp))
                LinearStatBar(0.55f)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    StatLabel("9", "Completed")
                    StatLabel("6", "In progress")
                    StatLabel("18", "Not started")
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter -> FilterChip(filter, filter == selectedFilter) { selectedFilter = filter } }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(physicsChapters) { chapter ->
                    ChapterRow(chapter)
                    HorizontalDivider(color = BgDivider, thickness = 0.5.dp)
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable private fun StatLabel(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable private fun ChapterRow(chapter: Chapter) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (chapter.progress >= 1f) {
                Box(Modifier.size(18.dp).clip(CircleShape).background(AccentGreen), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, "Completed", tint = AccentGreenDark, modifier = Modifier.size(12.dp))
                }
            } else {
                Box(Modifier.size(18.dp).clip(CircleShape).background(BgDivider))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(chapter.number.toString() + ". " + chapter.name, color = TextOnCard, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                LinearStatBar(chapter.progress, Modifier.width(120.dp), fillColor = chapter.progressColor, height = 3.dp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text((chapter.progress * 100).toInt().toString() + "%", color = TextMuted, fontSize = 11.sp)
        Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(16.dp))
    }
}