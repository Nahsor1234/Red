package com.example.jeecommandcenter.data

/** Connects completed assessments to the same evidence used by the learning engine. */
class TestIntegration(
    private val learning: LearningRepository
) {
    data class SubjectResult(val subject: String, val attempted: Int, val correct: Int, val accuracy: Float)

    data class TestFocus(
        val title: String,
        val subject: String,
        val chapterId: String?,
        val durationMin: Int,
        val priority: PriorityLevel,
        val reason: String
    )

    fun recent(limit: Int = 10): List<TestAttemptRecord> =
        learning.getTestAttempts().sortedByDescending { it.completedAt }.take(limit)

    fun subjectResults(): List<SubjectResult> {
        val attempts = learning.getQuestionAttempts()
        return listOf("Physics", "Chemistry", "Mathematics").map { subject ->
            val rows = attempts.filter { it.subject == subject }
            val correct = rows.count { it.correct }
            SubjectResult(subject, rows.size, correct, if (rows.isEmpty()) 0f else correct.toFloat() / rows.size)
        }
    }

    fun weakestSubject(): SubjectResult? =
        subjectResults().filter { it.attempted >= 3 }.minByOrNull { it.accuracy }

    fun focus(): TestFocus? {
        val tests = recent()
        val weakest = weakestSubject()
        if (tests.isEmpty() && weakest == null) return null
        return weakest?.let {
            TestFocus(
                title = "Practice ${it.subject}",
                subject = it.subject,
                chapterId = null,
                durationMin = 30,
                priority = if (it.accuracy < 0.5f) PriorityLevel.HIGH else PriorityLevel.MEDIUM,
                reason = "${(it.accuracy * 100).toInt()}% accuracy across ${it.attempted} recent questions"
            )
        } ?: tests.firstOrNull()?.let {
            TestFocus(
                title = "Review your latest test",
                subject = "All subjects",
                chapterId = null,
                durationMin = 20,
                priority = PriorityLevel.MEDIUM,
                reason = "Use the latest result to review incorrect and skipped questions"
            )
        }
    }

    fun performanceTrend(): Float? {
        val tests = recent(6).reversed().filter { it.totalMarks > 0 }
        if (tests.size < 2) return null
        val first = tests.take(tests.size / 2).map { it.score * 100f / it.totalMarks }.average().toFloat()
        val last = tests.drop(tests.size / 2).map { it.score * 100f / it.totalMarks }.average().toFloat()
        return last - first
    }
}
