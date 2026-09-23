package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.CloudJeeRepository
import com.example.jeecommandcenter.data.CloudQuestion
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChapterQuestionPracticeScreen(
    chapterId: String,
    chapterName: String,
    onBack: () -> Unit
) {
    val cloud = remember { CloudJeeRepository() }
    val scope = rememberCoroutineScope()
    var userId by remember { mutableStateOf<String?>(null) }
    var questions by remember { mutableStateOf<List<CloudQuestion>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var submitted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chapterId) {
        loading = true
        error = null
        runCatching {
            val user = cloud.ensureSession()
            userId = user.id
            questions = cloud.questions(chapterId).shuffled()
        }.onFailure {
            error = it.message ?: "Could not load questions."
        }
        loading = false
    }

    val question = questions.getOrNull(index)

    Scaffold(
        containerColor = BgApp,
        topBar = {
            JeeTopBar(
                title = "Practice",
                onBack = onBack,
                trailing = {
                    Icon(Icons.Filled.Cloud, null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                }
            )
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }

            error != null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Cloud questions unavailable", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(error.orEmpty(), color = TextSecondary, fontSize = 12.sp)
            }

            question == null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("No questions yet", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "This chapter is connected to the question database, but no questions have been seeded for it yet.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
            ) {
                item {
                    Text(chapterName, color = PrimaryLight, fontSize = 11.sp)
                    Text("Question ${index + 1} / ${questions.size}", color = TextSecondary, fontSize = 11.sp)
                }

                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = BgCard,
                        border = BorderStroke(1.dp, BgCardBorder.copy(alpha = .85f))
                    ) {
                        Text(question.prompt, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                    }
                }

                items(question.options.indices.toList()) { option ->
                    val chosen = selected == option
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (chosen) PrimarySoft else BgCard)
                            .border(
                                1.dp,
                                if (chosen) Primary else BgCardBorder.copy(alpha = .75f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !submitted) { selected = option }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(chosen, onClick = null, enabled = !submitted)
                        Spacer(Modifier.width(8.dp))
                        Text(question.options[option], color = TextPrimary)
                    }
                }

                item {
                    if (!submitted) {
                        Button(
                            onClick = {
                                if (selected >= 0) {
                                    submitted = true
                                    userId?.let { uid ->
                                        scope.launch {
                                            runCatching {
                                                cloud.recordQuestionAttempt(uid, question, selected, 0)
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = selected >= 0,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Check answer")
                        }
                    } else {
                        val correct = selected == question.correctIndex
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = BgCardAlt,
                            border = BorderStroke(
                                1.dp,
                                if (correct) Success else Danger
                            )
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (correct) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                        null,
                                        tint = if (correct) Success else Danger,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (correct) "Correct" else "Review this one",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(7.dp))
                                Text(question.explanation, color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = {
                                if (index + 1 < questions.size) {
                                    index++
                                    selected = -1
                                    submitted = false
                                } else {
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (index + 1 < questions.size) "Next question" else "Finish practice")
                        }
                    }
                }
            }
        }
    }
}
