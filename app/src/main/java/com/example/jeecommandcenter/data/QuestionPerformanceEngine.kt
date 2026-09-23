package com.example.jeecommandcenter.data

import kotlin.math.roundToInt

/** Deterministic performance layer built on recorded question attempts. */
class QuestionPerformanceEngine(private val learning: LearningRepository) {
    data class TopicPerformance(
        val subject: String,
        val chapterId: String,
        val topic: String,
        val attempted: Int,
        val correct: Int,
        val accuracy: Float,
        val averageResponseSec: Int,
        val incorrect: Int
    )

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

    fun topicPerformance(): List<TopicPerformance> {
        val questions = learning.getQuestions().associateBy { it.id }
        return learning.getQuestionAttempts()
            .groupBy { attempt ->
                val q = questions[attempt.questionId]
                q?.topic?.takeIf { it.isNotBlank() } ?: "General"
            }
            .map { (topic, attempts) ->
                val first = attempts.first()
                val correct = attempts.count { it.correct }
                TopicPerformance(
                    subject = first.subject,
                    chapterId = JeeCatalog.normalizeChapterId(first.chapterId),
                    topic = topic,
                    attempted = attempts.size,
                    correct = correct,
                    accuracy = correct.toFloat() / attempts.size,
                    averageResponseSec = attempts.map { it.responseTimeSec }.average().roundToInt(),
                    incorrect = attempts.size - correct
                )
            }
            .sortedWith(compareBy({ it.subject }, { it.chapterId }, { it.topic }))
    }

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

    fun accuracyForChapter(chapterId: String): Float =
        chapterPerformance().firstOrNull { it.chapter.id == JeeCatalog.normalizeChapterId(chapterId) }?.accuracy ?: 0f
}
