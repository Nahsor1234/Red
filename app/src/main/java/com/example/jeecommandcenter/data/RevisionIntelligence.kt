package com.example.jeecommandcenter.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Deterministic revision policy. Question performance and unresolved mistakes are first-class signals. */
class RevisionIntelligence(
    private val jee: JeeRepository,
    private val learning: LearningRepository
) {
    private val zone = ZoneId.systemDefault()

    data class RevisionSignal(
        val chapter: JeeChapter,
        val priority: PriorityLevel,
        val score: Int,
        val reason: String,
        val due: Boolean,
        val accuracy: Float?,
        val unresolvedMistakes: Int
    )

    fun signals(limit: Int = 12): List<RevisionSignal> {
        val today = LocalDate.now()
        val attempts = learning.getQuestionAttempts()
        val mistakes = learning.getMistakes()
        val queue = jee.getRevisionQueue().associateBy { it.chapter.id }

        return JeeCatalog.chapters.mapNotNull { chapter ->
            val state = jee.getChapterState(chapter.id)
            if (state.progress <= 0f && attempts.none { JeeCatalog.normalizeChapterId(it.chapterId) == chapter.id }) return@mapNotNull null

            val chapterAttempts = attempts.filter { JeeCatalog.normalizeChapterId(it.chapterId) == chapter.id }
            val accuracy = chapterAttempts.takeIf { it.size >= 3 }?.let {
                it.count { attempt -> attempt.correct }.toFloat() / it.size
            }
            val unresolved = mistakes.count { JeeCatalog.normalizeChapterId(it.chapterId) == chapter.id && !it.resolved }
            val latestRevision = jee.getRevisionHistory(chapter.id).maxByOrNull { it.reviewedAt }
            val daysSinceRevision = latestRevision?.let {
                ChronoUnit.DAYS.between(Instant.ofEpochMilli(it.reviewedAt).atZone(zone).toLocalDate(), today)
            } ?: 999L
            val dueItem = queue[chapter.id]
            val due = dueItem != null && !LocalDate.parse(dueItem.revision.dueDate).isAfter(today)

            var score = 0
            val reasons = mutableListOf<String>()
            if (due) {
                score += 35
                reasons += if (dueItem!!.revision.dueDate < today.toString()) "revision overdue" else "revision due today"
            } else if (daysSinceRevision >= 7) {
                score += minOf(25, 10 + (daysSinceRevision - 7).toInt())
                reasons += "${daysSinceRevision}d since revision"
            }
            if (state.confidence <= 2) {
                score += 15
                reasons += "low confidence"
            }
            if (accuracy != null && accuracy < 0.65f) {
                score += ((0.65f - accuracy) * 80).roundToInt()
                reasons += "${(accuracy * 100).roundToInt()}% accuracy"
            }
            if (unresolved > 0) {
                score += minOf(20, unresolved * 5)
                reasons += "$unresolved unresolved mistake${if (unresolved == 1) "" else "s"}"
            }
            if (score == 0) return@mapNotNull null

            RevisionSignal(
                chapter = chapter,
                priority = when { score >= 55 -> PriorityLevel.HIGH; score >= 30 -> PriorityLevel.MEDIUM; else -> PriorityLevel.LOW },
                score = score,
                reason = reasons.take(2).joinToString(" + "),
                due = due,
                accuracy = accuracy,
                unresolvedMistakes = unresolved
            )
        }.sortedByDescending { it.score }.take(limit)
    }

    fun dueCount(): Int = signals(Int.MAX_VALUE).count { it.due }

    fun highPriority(limit: Int = 5): List<RevisionSignal> =
        signals(Int.MAX_VALUE).filter { it.priority == PriorityLevel.HIGH }.take(limit)
}
