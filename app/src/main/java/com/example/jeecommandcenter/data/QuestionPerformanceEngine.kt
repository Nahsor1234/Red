package com.example.jeecommandcenter.data

import kotlin.math.roundToInt

/** Deterministic performance layer built on recorded question attempts. */
class QuestionPerformanceEngine(private val learning: LearningRepository) {

    data class ChapterPerformance(
        val chapter: JeeChapter,
        val attempted: Int,
        val correct: Int,
        val accuracy: Float,
        val averageResponseSec: Int,
        val unresolvedMistakes: Int,
        val repeatedMistakes: Int,
        val strengthScore: Int
    )

    fun chapterPerformance(): List<ChapterPerformance> {
        val attempts = learning.getQuestionAttempts()
        val mistakes = learning.getMistakes()
        return JeeCatalog.chapters.map { chapter ->
            val rows = attempts.filter { JeeCatalog.normalizeChapterId(it.chapterId) == chapter.id }
            val correct = rows.count { it.correct }
            val unresolved = mistakes.count {
                JeeCatalog.normalizeChapterId(it.chapterId) == chapter.id && !it.resolved
            }
            val repeated = mistakes.count {
                JeeCatalog.normalizeChapterId(it.chapterId) == chapter.id && !it.resolved && it.count >= 2
            }
            val accuracy = if (rows.isEmpty()) 0f else correct.toFloat() / rows.size
            val accuracyScore = (accuracy * 60).roundToInt()
            val experienceScore = minOf(rows.size, 10) * 2
            val mistakePenalty = minOf(unresolved * 5 + repeated * 5, 30)
            ChapterPerformance(
                chapter = chapter,
                attempted = rows.size,
                correct = correct,
                accuracy = accuracy,
                averageResponseSec = rows.takeIf { it.isNotEmpty() }?.map { it.responseTimeSec }?.average()?.roundToInt() ?: 0,
                unresolvedMistakes = unresolved,
                repeatedMistakes = repeated,
                strengthScore = (accuracyScore + experienceScore - mistakePenalty).coerceIn(0, 100)
            )
        }
    }

    fun weakChapters(limit: Int = 8): List<ChapterPerformance> =
        chapterPerformance()
            .filter { it.attempted > 0 || it.unresolvedMistakes > 0 }
            .sortedBy { it.strengthScore }
            .take(limit)

}
