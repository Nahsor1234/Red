package com.example.jeecommandcenter.data

/** Final deterministic aggregation layer. It orchestrates existing intelligence engines. */
class FinalIntelligence(
    private val jee: JeeRepository,
    private val learning: LearningRepository
) {
    private val performance = QuestionPerformanceEngine(learning)
    private val mistakes = MistakeIntelligence(learning)
    private val revision = RevisionIntelligence(jee, learning)
    private val tests = TestIntegration(learning)

    fun revisionSignals(limit: Int = 10): List<RevisionIntelligence.RevisionSignal> = revision.signals(limit)

    fun weakChapters(limit: Int = 8): List<QuestionPerformanceEngine.ChapterPerformance> =
        performance.weakChapters(limit)

    fun topMistakePatterns(limit: Int = 5): List<MistakeIntelligence.Pattern> =
        mistakes.recurringPatterns().take(limit)

    fun testFocus(): TestIntegration.TestFocus? = tests.focus()

    fun questionAccuracy(): Float {
        val attempts = learning.getQuestionAttempts()
        return if (attempts.isEmpty()) 0f else attempts.count { it.correct }.toFloat() / attempts.size
    }

    fun unresolvedMistakes(): Int = learning.getMistakes().count { !it.resolved }

    fun currentState(): StudentState {
        val attempts = learning.getQuestionAttempts()
        val testsCompleted = learning.getTestAttempts().size
        val revisionSignals = revision.signals(Int.MAX_VALUE)
        val weak = performance.weakChapters(Int.MAX_VALUE)
        val highRevision = revisionSignals.count { it.priority == PriorityLevel.HIGH }
        return StudentState(
            questionAttempts = attempts.size,
            questionAccuracy = questionAccuracy(),
            unresolvedMistakes = unresolvedMistakes(),
            highPriorityRevisions = highRevision,
            weakChapterCount = weak.count { it.strengthScore < 50 },
            testsCompleted = testsCompleted,
            recentTestTrend = tests.performanceTrend()
        )
    }

    data class StudentState(
        val questionAttempts: Int,
        val questionAccuracy: Float,
        val unresolvedMistakes: Int,
        val highPriorityRevisions: Int,
        val weakChapterCount: Int,
        val testsCompleted: Int,
        val recentTestTrend: Float?
    )
}
