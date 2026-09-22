package com.example.jeecommandcenter.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class PerformanceTrendPoint(
    val date: LocalDate,
    val studyMinutes: Int,
    val questions: Int,
    val accuracy: Float?,
    val tests: Int,
    val testScore: Float?
)

data class ChapterEvidence(
    val chapterId: String,
    val chapterName: String,
    val subject: String,
    val studyMinutes: Int,
    val questions: Int,
    val accuracy: Float?,
    val unresolvedMistakes: Int,
    val revisions: Int,
    val lastStudiedAt: Long
)

class PerformanceAnalytics(
    private val jee: JeeRepository,
    private val learning: LearningRepository
) {
    private val zone = ZoneId.systemDefault()

    fun trend(days: Int = 14): List<PerformanceTrendPoint> {
        val safeDays = days.coerceIn(7, 30)
        val today = LocalDate.now()
        val sessions = jee.getSessions()
        val attempts = learning.getQuestionAttempts()
        val tests = learning.getTestAttempts()

        return (safeDays - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val daySessions = sessions.filter { it.startedAt in start until end }
            val dayAttempts = attempts.filter { it.createdAt in start until end }
            val dayTests = tests.filter { it.completedAt in start until end }
            val correct = dayAttempts.count { it.correct }
            val scores = dayTests.mapNotNull {
                if (it.totalMarks <= 0) null else it.score * 100f / it.totalMarks
            }
            PerformanceTrendPoint(
                date = date,
                studyMinutes = daySessions.sumOf { it.minutes },
                questions = dayAttempts.size,
                accuracy = dayAttempts.takeIf { it.isNotEmpty() }?.let {
                    correct.toFloat() / it.size
                },
                tests = dayTests.size,
                testScore = scores.takeIf { it.isNotEmpty() }?.average()?.toFloat()
            )
        }
    }

    fun chapterEvidence(limit: Int = 12): List<ChapterEvidence> {
        val sessions = jee.getSessions()
        val attempts = learning.getQuestionAttempts()
        val mistakes = learning.getMistakes()
        val revisions = jee.getRevisionHistory()

        return JeeCatalog.chapters.map { chapter ->
            val chapterSessions = sessions.filter { it.chapterId == chapter.id }
            val chapterAttempts = attempts.filter { it.chapterId == chapter.id }
            val correct = chapterAttempts.count { it.correct }
            ChapterEvidence(
                chapterId = chapter.id,
                chapterName = chapter.name,
                subject = chapter.subject,
                studyMinutes = chapterSessions.sumOf { it.minutes },
                questions = chapterAttempts.size,
                accuracy = chapterAttempts.takeIf { it.isNotEmpty() }?.let {
                    correct.toFloat() / it.size
                },
                unresolvedMistakes = mistakes.count {
                    it.chapterId == chapter.id && !it.resolved
                },
                revisions = revisions.count { it.chapterId == chapter.id },
                lastStudiedAt = chapterSessions.maxOfOrNull { it.startedAt }
                    ?: jee.getChapterState(chapter.id).lastStudiedAt
            )
        }.filter {
            it.studyMinutes > 0 || it.questions > 0 || it.unresolvedMistakes > 0 || it.revisions > 0
        }.sortedWith(
            compareByDescending<ChapterEvidence> { it.unresolvedMistakes }
                .thenBy { it.accuracy ?: 0f }
                .thenByDescending { it.questions + it.studyMinutes }
        ).take(limit)
    }
}
