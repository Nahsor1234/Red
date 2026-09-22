package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.util.UUID

class LearningRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("jee_learning_engine", Context.MODE_PRIVATE)

    fun getQuestions(): List<Question> {
        val raw = runCatching {
            context.assets.open("questions/starter.json").use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }
        }.getOrNull()

        if (raw.isNullOrBlank()) return StarterQuestionBank.fallback

        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                Question(
                    id = item.getString("id"),
                    subject = item.getString("subject"),
                    chapterId = item.getString("chapterId"),
                    chapter = item.getString("chapter"),
                    topic = item.getString("topic"),
                    prompt = item.getString("prompt"),
                    options = List(item.getJSONArray("options").length()) { i ->
                        item.getJSONArray("options").getString(i)
                    },
                    correctIndex = item.getInt("correctIndex"),
                    explanation = item.getString("explanation"),
                    difficulty = item.optInt("difficulty", 3),
                    source = item.optString("source", "JeE question bank"),
                    year = if (item.isNull("year")) null else item.optInt("year"),
                    marks = item.optInt("marks", 4),
                    negativeMarks = item.optDouble("negativeMarks", 1.0).toFloat()
                )
            }
        }.getOrElse { StarterQuestionBank.fallback }
    }

    fun selectQuestions(
        mode: TestMode,
        subject: String?,
        count: Int
    ): List<Question> {
        val source = getQuestions().let { questions ->
            when {
                mode == TestMode.PRACTICE && !subject.isNullOrBlank() ->
                    questions.filter { it.subject == subject }
                else -> questions
            }
        }

        return source.shuffled().take(count.coerceIn(1, source.size))
    }

    fun saveQuestionAttempt(
        testId: String,
        question: Question,
        selectedIndex: Int,
        responseTimeSec: Int
    ): QuestionAttemptRecord {
        val record = QuestionAttemptRecord(
            id = System.currentTimeMillis(),
            testId = testId,
            questionId = question.id,
            selectedIndex = selectedIndex,
            correct = selectedIndex == question.correctIndex,
            responseTimeSec = responseTimeSec.coerceAtLeast(0),
            subject = question.subject,
            chapterId = question.chapterId
        )
        val all = getQuestionAttempts().toMutableList().apply { add(record) }
        saveQuestionAttempts(all.takeLast(1000))
        if (!record.correct) registerMistake(question, selectedIndex)
        return record
    }

    fun saveTestAttempt(record: TestAttemptRecord) {
        val array = JSONArray()
        getTestAttempts().plus(record).takeLast(100).forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("mode", item.mode.name)
                    put("startedAt", item.startedAt)
                    put("completedAt", item.completedAt)
                    put("questionCount", item.questionCount)
                    put("correctCount", item.correctCount)
                    put("score", item.score)
                    put("totalMarks", item.totalMarks)
                    put("durationSec", item.durationSec)
                    put("name", item.name)
                    put("attemptedCount", item.attemptedCount)
                    put("incorrectCount", item.incorrectCount)
                    put("skippedCount", item.skippedCount)
                    val subjectObj = JSONObject()
                    item.subjectBreakdown.forEach { (subject, pair) ->
                        subjectObj.put(subject, JSONObject().apply {
                            put("correct", pair.first)
                            put("attempted", pair.second)
                        })
                    }
                    put("subjectBreakdown", subjectObj)
                }
            )
        }
        prefs.edit().putString("test_attempts", array.toString()).apply()
    }

    fun getQuestionAttempts(): List<QuestionAttemptRecord> {
        val raw = prefs.getString("question_attempts", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                QuestionAttemptRecord(
                    id = obj.getLong("id"),
                    testId = obj.getString("testId"),
                    questionId = obj.getString("questionId"),
                    selectedIndex = obj.getInt("selectedIndex"),
                    correct = obj.getBoolean("correct"),
                    responseTimeSec = obj.optInt("responseTimeSec", 0),
                    subject = obj.getString("subject"),
                    chapterId = obj.getString("chapterId"),
                    mistakeType = obj.optString("mistakeType").takeIf { it.isNotBlank() }
                        ?.let { runCatching { MistakeType.valueOf(it) }.getOrNull() },
                    createdAt = obj.optLong("createdAt", obj.getLong("id"))
                )
            }
        }.getOrDefault(emptyList())
    }

    fun getTestAttempts(): List<TestAttemptRecord> {
        val raw = prefs.getString("test_attempts", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                val subjectObj = obj.optJSONObject("subjectBreakdown") ?: JSONObject()
                val breakdown = subjectObj.keys().asSequence().associateWith { key ->
                    val item = subjectObj.getJSONObject(key)
                    item.getInt("correct") to item.getInt("attempted")
                }
                TestAttemptRecord(
                    id = obj.getString("id"),
                    mode = TestMode.valueOf(obj.getString("mode")),
                    startedAt = obj.getLong("startedAt"),
                    completedAt = obj.getLong("completedAt"),
                    questionCount = obj.getInt("questionCount"),
                    correctCount = obj.getInt("correctCount"),
                    score = obj.getInt("score"),
                    totalMarks = obj.getInt("totalMarks"),
                    durationSec = obj.getInt("durationSec"),
                    subjectBreakdown = breakdown,
                    name = obj.optString("name"),
                    attemptedCount = obj.optInt("attemptedCount", obj.optInt("questionCount")),
                    incorrectCount = obj.optInt("incorrectCount", obj.optInt("questionCount") - obj.optInt("correctCount")),
                    skippedCount = obj.optInt("skippedCount", 0)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun getMistakes(): List<MistakeRecord> {
        val raw = prefs.getString("mistakes", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                MistakeRecord(
                    id = obj.getString("id"),
                    questionId = obj.getString("questionId"),
                    subject = obj.getString("subject"),
                    chapterId = obj.getString("chapterId"),
                    questionPrompt = obj.getString("questionPrompt"),
                    correctAnswer = obj.getString("correctAnswer"),
                    lastSelectedAnswer = obj.getString("lastSelectedAnswer"),
                    count = obj.optInt("count", 1),
                    mistakeType = runCatching {
                        MistakeType.valueOf(obj.optString("mistakeType", MistakeType.UNCLASSIFIED.name))
                    }.getOrDefault(MistakeType.UNCLASSIFIED),
                    source = obj.optString("source", "assessment"),
                    correction = obj.optString("correction", ""),
                    resolved = obj.optBoolean("resolved", false),
                    lastSeenAt = obj.optLong("lastSeenAt", System.currentTimeMillis())
                )
            }.sortedByDescending { it.count }
        }.getOrDefault(emptyList())
    }

    fun updateMistakeType(id: String, type: MistakeType) {
        saveMistakes(getMistakes().map { if (it.id == id) it.copy(mistakeType = type) else it })
    }

    fun resolveMistake(id: String) {
        saveMistakes(getMistakes().map { if (it.id == id) it.copy(resolved = true) else it })
    }

    fun analytics(jeeRepository: JeeRepository): AnalyticsSnapshot {
        val attempts = getQuestionAttempts()
        val correct = attempts.count { it.correct }
        val tests = getTestAttempts()
        val averageScore = tests.takeIf { it.isNotEmpty() }?.map { attempt ->
            if (attempt.totalMarks == 0) 0f else attempt.score * 100f / attempt.totalMarks
        }?.average()?.toFloat() ?: 0f

        val cutoff = System.currentTimeMillis() - 7L * 86_400_000L
        val study7d = jeeRepository.getSessions()
            .filter { it.startedAt >= cutoff }
            .sumOf { it.minutes }

        val bySubject = listOf("Physics", "Chemistry", "Mathematics").map { subject ->
            val subjectAttempts = attempts.filter { it.subject == subject }
            val subjectCorrect = subjectAttempts.count { it.correct }
            SubjectAnalytics(
                subject = subject,
                attempted = subjectAttempts.size,
                correct = subjectCorrect,
                accuracy = if (subjectAttempts.isEmpty()) 0f
                else subjectCorrect.toFloat() / subjectAttempts.size
            )
        }

        val unresolved = getMistakes().filterNot { it.resolved }
        return AnalyticsSnapshot(
            studyMinutes7d = study7d,
            totalQuestionsAttempted = attempts.size,
            correctAnswers = correct,
            accuracy = if (attempts.isEmpty()) 0f else correct.toFloat() / attempts.size,
            testsCompleted = tests.size,
            averageTestScore = averageScore,
            unresolvedMistakes = unresolved.size,
            repeatedMistakes = unresolved.count { it.count >= 2 },
            masteredChapters = jeeRepository.getMasteredChapterCount(),
            activeRevisionItems = jeeRepository.getActiveRevisionCount(),
            subjectAnalytics = bySubject
        )
    }

    fun mistakeStats(): List<MistakeStats> {
        return getMistakes()
            .filterNot { it.resolved }
            .groupingBy { it.mistakeType }
            .eachCount()
            .map { MistakeStats(it.key, it.value) }
            .sortedByDescending { it.count }
    }

    private fun registerMistake(question: Question, selectedIndex: Int) {
        val selected = question.options.getOrNull(selectedIndex) ?: "No answer"
        val correct = question.options.getOrNull(question.correctIndex) ?: "Unknown"
        val existing = getMistakes().firstOrNull { it.questionId == question.id }
        val updated = if (existing == null) {
            MistakeRecord(
                id = "mistake_" + question.id,
                questionId = question.id,
                subject = question.subject,
                chapterId = question.chapterId,
                questionPrompt = question.prompt,
                correctAnswer = correct,
                lastSelectedAnswer = selected,
                count = 1,
                mistakeType = MistakeType.UNCLASSIFIED,
                source = "assessment",
                correction = question.explanation
            )
        } else {
            existing.copy(
                lastSelectedAnswer = selected,
                count = existing.count + 1,
                resolved = false,
                lastSeenAt = System.currentTimeMillis()
            )
        }
        saveMistakes(getMistakes().filterNot { it.id == updated.id } + updated)
    }

    private fun saveMistakes(items: List<MistakeRecord>) {
        val array = JSONArray()
        items.takeLast(500).forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("questionId", item.questionId)
                    put("subject", item.subject)
                    put("chapterId", item.chapterId)
                    put("questionPrompt", item.questionPrompt)
                    put("correctAnswer", item.correctAnswer)
                    put("lastSelectedAnswer", item.lastSelectedAnswer)
                    put("count", item.count)
                    put("mistakeType", item.mistakeType.name)
                    put("source", item.source)
                    put("correction", item.correction)
                    put("resolved", item.resolved)
                    put("lastSeenAt", item.lastSeenAt)
                }
            )
        }
        prefs.edit().putString("mistakes", array.toString()).apply()
    }

    private fun saveQuestionAttempts(items: List<QuestionAttemptRecord>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("testId", item.testId)
                    put("questionId", item.questionId)
                    put("selectedIndex", item.selectedIndex)
                    put("correct", item.correct)
                    put("responseTimeSec", item.responseTimeSec)
                    put("subject", item.subject)
                    put("chapterId", item.chapterId)
                    put("mistakeType", item.mistakeType?.name ?: "")
                    put("createdAt", item.createdAt)
                }
            )
        }
        prefs.edit().putString("question_attempts", array.toString()).apply()
    }
}
