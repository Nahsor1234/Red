package com.example.jeecommandcenter.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.CloudJeeRepository
import com.example.jeecommandcenter.data.CloudQuestion
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch

private const val BOOKMARK_PREFS = "sigma_practice_bookmarks"
private const val BOOKMARKS_KEY = "bookmarked_questions"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterQuestionPracticeScreen(
    chapterId: String,
    chapterName: String,
    onBack: () -> Unit,
    onOpenAiTutor: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val cloud = remember { CloudJeeRepository() }
    val scope = rememberCoroutineScope()
    val bookmarkPrefs = remember(context) {
        context.getSharedPreferences(BOOKMARK_PREFS, Context.MODE_PRIVATE)
    }
    val jumpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var userId by remember { mutableStateOf<String?>(null) }
    var questions by remember(chapterId) { mutableStateOf<List<CloudQuestion>>(emptyList()) }
    var index by remember(chapterId) { mutableIntStateOf(0) }
    var selectedByQuestion by remember(chapterId) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var checkedQuestionIds by remember(chapterId) { mutableStateOf<Set<String>>(emptySet()) }
    var bookmarkedKeys by remember(chapterId) {
        mutableStateOf(bookmarkPrefs.getStringSet(BOOKMARKS_KEY, emptySet()).orEmpty().toSet())
    }
    var showQuestionPicker by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chapterId) {
        loading = true
        error = null
        index = 0
        selectedByQuestion = emptyMap()
        checkedQuestionIds = emptySet()
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
    val selectedIndex = question?.let { selectedByQuestion[it.id] ?: -1 } ?: -1
    val submitted = question?.let { it.id in checkedQuestionIds } == true
    val bookmarked = question?.let { bookmarkKey(chapterId, it.id) in bookmarkedKeys } == true

    fun toggleBookmark(questionId: String) {
        val key = bookmarkKey(chapterId, questionId)
        val updated = if (key in bookmarkedKeys) bookmarkedKeys - key else bookmarkedKeys + key
        bookmarkedKeys = updated
        bookmarkPrefs.edit().putStringSet(BOOKMARKS_KEY, updated.toSet()).apply()
    }

    fun openCurrentQuestionInAi() {
        val currentQuestion = question ?: return
        onOpenAiTutor(
            createQuestionTutorContext(
                chapterId = chapterId,
                chapterName = chapterName,
                questionNumber = index + 1,
                totalQuestions = questions.size,
                question = currentQuestion,
                selectedIndex = selectedByQuestion[currentQuestion.id] ?: -1,
                checked = currentQuestion.id in checkedQuestionIds
            )
        )
    }

    Scaffold(
        containerColor = BgApp,
        topBar = {
            JeeTopBar(
                title = "Practice",
                onBack = onBack,
                trailing = {
                    TextButton(onClick = ::openCurrentQuestionInAi, enabled = question != null) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Tutor", fontSize = 12.sp)
                    }
                }
            )
        },
        bottomBar = {
            if (!loading && error == null && question != null) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(BgAppBase.copy(alpha = .97f))
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!submitted) {
                        Button(
                            onClick = {
                                if (selectedIndex >= 0) {
                                    checkedQuestionIds = checkedQuestionIds + question.id
                                    userId?.let { uid ->
                                        scope.launch {
                                            runCatching {
                                                cloud.recordQuestionAttempt(uid, question, selectedIndex, 0)
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = selectedIndex >= 0,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                        ) {
                            Text("Check answer")
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { index = (index - 1).coerceAtLeast(0) },
                            enabled = index > 0,
                            modifier = Modifier.weight(1f).height(48.dp),
                            contentPadding = PaddingValues(horizontal = 5.dp)
                        ) {
                            Icon(Icons.Filled.ChevronLeft, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Previous", maxLines = 1, fontSize = 12.sp)
                        }

                        Surface(
                            modifier = Modifier.size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { toggleBookmark(question.id) },
                            color = if (bookmarked) PrimarySoft else BgCard,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (bookmarked) Primary else BgCardBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = if (bookmarked) "Remove bookmark" else "Bookmark question",
                                    tint = if (bookmarked) PrimaryLight else TextSecondary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (index < questions.lastIndex) index++ else onBack()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(if (index < questions.lastIndex) "Next" else "Finish", maxLines = 1, fontSize = 12.sp)
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                if (index < questions.lastIndex) Icons.Filled.ChevronRight else Icons.Filled.Check,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
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
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                chapterName,
                                color = PrimaryLight,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Question ${index + 1}/${questions.size}",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TextButton(onClick = { showQuestionPicker = true }) {
                            Icon(Icons.Filled.GridView, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Jump")
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { (index + 1).toFloat() / questions.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(99.dp)),
                        color = Primary,
                        trackColor = BgDivider
                    )
                }

                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = BgCardAlt,
                        border = BorderStroke(1.dp, BgCardBorder.copy(alpha = .85f))
                    ) {
                        Text(
                            question.prompt,
                            Modifier.padding(18.dp),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                }

                listItems(question.options.indices.toList(), key = { it }) { option ->
                    val chosen = selectedIndex == option
                    val isCorrectOption = submitted && option == question.correctIndex
                    val isWrongSelection = submitted && chosen && !isCorrectOption
                    val borderColor = when {
                        isCorrectOption -> Primary
                        isWrongSelection -> Danger
                        chosen -> Primary
                        else -> BgCardBorder.copy(alpha = .8f)
                    }
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (chosen || isCorrectOption) PrimarySoft else BgCard)
                            .border(
                                BorderStroke(if (chosen || isCorrectOption) 1.5.dp else 1.dp, borderColor),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = !submitted) {
                                selectedByQuestion = selectedByQuestion + (question.id to option)
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = chosen,
                            onClick = null,
                            enabled = !submitted,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Primary,
                                unselectedColor = TextMuted,
                                disabledSelectedColor = Primary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            question.options[option],
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCorrectOption) {
                            Icon(Icons.Filled.CheckCircle, null, tint = PrimaryLight, modifier = Modifier.size(19.dp))
                        } else if (isWrongSelection) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = Danger, modifier = Modifier.size(19.dp))
                        }
                    }
                }

                if (submitted) {
                    item {
                        val correct = selectedIndex == question.correctIndex
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = BgCardAlt,
                            border = BorderStroke(1.dp, if (correct) Primary else Danger)
                        ) {
                            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (correct) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                        null,
                                        tint = if (correct) PrimaryLight else Danger,
                                        modifier = Modifier.size(21.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (correct) "Correct answer" else "Review this one",
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Text(question.explanation, color = TextSecondary, fontSize = 13.sp)
                                TextButton(onClick = ::openCurrentQuestionInAi) {
                                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Discuss with AI tutor")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuestionPicker && questions.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showQuestionPicker = false },
            sheetState = jumpSheetState,
            containerColor = BgCardAlt,
            contentColor = TextPrimary,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Jump to question", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${checkedQuestionIds.size} checked · ${questions.size} questions",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    gridItems(questions.indices.toList(), key = { it }) { questionIndex ->
                        val paletteQuestion = questions[questionIndex]
                        val paletteBookmark = bookmarkKey(chapterId, paletteQuestion.id) in bookmarkedKeys
                        val wasChecked = paletteQuestion.id in checkedQuestionIds
                        val hasAnswer = selectedByQuestion.containsKey(paletteQuestion.id)
                        val cellColor = when {
                            questionIndex == index -> Primary
                            wasChecked -> PrimarySoft
                            hasAnswer -> BgCard
                            else -> BgAppBase
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                .clickable {
                                    index = questionIndex
                                    showQuestionPicker = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = cellColor,
                            border = BorderStroke(
                                1.dp,
                                when {
                                    questionIndex == index -> Primary
                                    paletteBookmark -> PrimaryLight
                                    wasChecked -> Primary.copy(alpha = .7f)
                                    else -> BgCardBorder
                                }
                            )
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "${questionIndex + 1}",
                                    color = if (questionIndex == index) BgAppBase else TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (paletteBookmark) {
                                    Icon(
                                        Icons.Filled.Bookmark,
                                        null,
                                        tint = if (questionIndex == index) BgAppBase else PrimaryLight,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(Primary))
                        Text("Current", color = TextSecondary, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(PrimarySoft).border(1.dp, Primary))
                        Text("Checked", color = TextSecondary, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Filled.Bookmark, null, tint = PrimaryLight, modifier = Modifier.size(12.dp))
                        Text("Bookmarked", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun bookmarkKey(chapterId: String, questionId: String): String = "${chapterId}::${questionId}"

private fun createQuestionTutorContext(
    chapterId: String,
    chapterName: String,
    questionNumber: Int,
    totalQuestions: Int,
    question: CloudQuestion,
    selectedIndex: Int,
    checked: Boolean
): String = buildString {
    appendLine("Subject/chapter: $chapterName")
    appendLine("Chapter ID: $chapterId")
    appendLine("Question number: $questionNumber of $totalQuestions")
    appendLine("Question: ${question.prompt}")
    appendLine("Options:")
    question.options.forEachIndexed { index, option -> appendLine("${'A' + index}. $option") }
    appendLine("Student's selected answer: ${question.options.getOrNull(selectedIndex) ?: "No answer selected"}")
    appendLine("Answer checked: $checked")
    if (checked) {
        appendLine("Correct answer: ${question.options.getOrNull(question.correctIndex) ?: "Unavailable"}")
        appendLine("Explanation: ${question.explanation}")
    } else {
        appendLine("Tutor instruction: Do not reveal the correct option immediately. Start with a concise hint and help the student reason it out.")
    }
}
