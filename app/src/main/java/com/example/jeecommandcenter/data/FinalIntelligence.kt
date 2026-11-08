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

}
