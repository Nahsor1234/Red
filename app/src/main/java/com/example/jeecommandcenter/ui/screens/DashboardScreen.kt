package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

@Composable
fun DashboardScreen(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, onFabClick: () -> Unit = {}) {
    var tasks by remember {
        mutableStateOf(listOf(
            TaskItem("1", "Study kinematics theory", "", "1h", done = true),
            TaskItem("2", "Solve 30 questions", "", "45m"),
            TaskItem("3", "Revise notes", "", "30m")
        ))
    }
    Scaffold(containerColor = BgApp, bottomBar = { BottomNavBar(selectedTab, onTabSelected, onFabClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(AccentBlueSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, tint = AccentBlueLight, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Good morning", style = MaterialTheme.typography.titleMedium)
                        Text("Let's make today count", color = TextMuted, fontSize = 12.sp)
                    }
                }
                Icon(Icons.Filled.Notifications, "Notifications", tint = TextSecondary)
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCardAlt).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bolt, null, tint = AccentBlue, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("JEE 2027", color = AccentBlueLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("252", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.width(4.dp))
                            Text("days left", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                    Text("Discipline compounds everything", color = TextMuted, fontSize = 11.sp, modifier = Modifier.widthIn(max = 100.dp))
                }
                Spacer(Modifier.height(8.dp))
                LinearStatBar(0.22f)
            }
            Spacer(Modifier.height(16.dp))
            Text("Overall preparation", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text("58%", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            LinearStatBar(0.58f)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Syllabus 60%", color = TextMuted, fontSize = 11.sp)
                Text("Practice 55%", color = TextMuted, fontSize = 11.sp)
                Text("Revision 48%", color = TextMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Today's goal", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("3h 40m", style = MaterialTheme.typography.titleLarge)
                        Text(" / 6h", color = TextMuted, fontSize = 12.sp)
                    }
                }
                Box(Modifier.size(46.dp).clip(CircleShape).background(BgDivider), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { 0.61f }, modifier = Modifier.size(46.dp), color = AccentGreen, trackColor = BgDivider, strokeWidth = 4.dp)
                    Text("61%", fontSize = 10.sp, color = AccentGreen, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgPriority).padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, null, tint = TextAmber, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Do this now", color = TextAmber, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(BgPriorityBadge).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("High priority", color = TextPriorityBadge, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("Kinematics — practice", style = MaterialTheme.typography.titleMedium)
                        Text("Solve 30 mixed questions (45m)", color = TextSecondary, fontSize = 12.sp)
                    }
                    Box(Modifier.size(34.dp).clip(CircleShape).background(AccentBlue), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, "Start", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SectionHeader("Today's tasks", "See all")
            Column {
                tasks.forEach { task ->
                    TaskRow(task) { id -> tasks = tasks.map { if (it.id == id) it.copy(done = !it.done) else it } }
                    if (task != tasks.last()) HorizontalDivider(color = BgDivider, thickness = 0.5.dp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}