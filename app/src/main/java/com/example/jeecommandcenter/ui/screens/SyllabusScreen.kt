package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun SyllabusScreen(
    repo: JeeRepository,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onFabClick: () -> Unit = {}
) {
    var selectedSubject by remember { mutableStateOf("Physics") }
    var selectedFilter by remember { mutableStateOf("All") }
    var refresh by remember { mutableIntStateOf(0) }

    val subjects = listOf("Physics", "Chemistry", "Mathematics")
    val filters = listOf("All", "Not started", "In progress", "Completed")
    val all = remember(refresh, selectedSubject) { repo.chapters(selectedSubject) }
    val chapters = all.filter { chapter ->
        when (selectedFilter) {
            "Not started" -> chapter.progress == 0f
            "In progress" -> chapter.progress > 0f && chapter.progress < 1f
            "Completed" -> chapter.progress >= 1f
            else -> true
        }
    }
    val avg = if (all.isEmpty()) 0f else all.map { it.progress }.average().toFloat()

    Scaffold(
        containerColor = BgApp,
        bottomBar = { BottomNavBar(selectedTab, onTabSelected, onFabClick) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Syllabus",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp)
            )

            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjects) { subject ->
                    FilterChip(subject, subject == selectedSubject) {
                        selectedSubject = subject
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCardAlt)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentBlueSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Science,
                            null,
                            tint = AccentBlueLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(selectedSubject, style = MaterialTheme.typography.titleLarge)
                        Text(
                            all.count { it.progress >= 1f }.toString() + " / " + all.size + " chapters complete",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        (avg * 100).toInt().toString() + "%",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp)
                    )
                }

                Spacer(Modifier.height(10.dp))
                LinearStatBar(avg)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    StatLabel(all.count { it.progress >= 1f }.toString(), "Completed")
                    StatLabel(
                        all.count { it.progress > 0f && it.progress < 1f }.toString(),
                        "In progress"
                    )
                    StatLabel(all.count { it.progress == 0f }.toString(), "Not started")
                }
            }

            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    FilterChip(filter, filter == selectedFilter) {
                        selectedFilter = filter
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(chapters, key = { it.number }) { chapter ->
                    ChapterRow(chapter) {
                        val next = when {
                            chapter.progress == 0f -> 0.5f
                            chapter.progress < 1f -> 1f
                            else -> 0f
                        }
                        repo.setChapterProgress(
                            chapter.subject,
                            chapter.number,
                            next
                        )
                        refresh++
                    }
                    HorizontalDivider(color = BgDivider, thickness = 0.5.dp)
                }
                item {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun StatLabel(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun ChapterRow(
    chapter: ChapterProgress,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .premiumClick(onClick)
            .padding(vertical = 12.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (chapter.progress >= 1f) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(AccentGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        "Completed",
                        tint = AccentGreenDark,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(BgDivider)
                )
            }

            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    chapter.number.toString() + ". " + chapter.name,
                    color = TextOnCard,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                LinearStatBar(
                    chapter.progress,
                    Modifier.width(120.dp),
                    fillColor = when {
                        chapter.progress == 0f -> BgDivider
                        chapter.progress < 1f -> AccentAmber
                        else -> AccentGreen
                    },
                    height = 3.dp
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Text(
            (chapter.progress * 100).toInt().toString() + "%",
            color = TextMuted,
            fontSize = 11.sp
        )
    }
}
