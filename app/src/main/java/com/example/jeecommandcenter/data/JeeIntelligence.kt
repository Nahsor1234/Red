package com.example.jeecommandcenter.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class PriorityLevel { HIGH, MEDIUM, LOW }

data class RevisionRecommendation(
    val chapter: JeeChapter,
    val level: PriorityLevel,
    val score: Int,
    val reason: String,
    val daysSinceRevision: Long,
    val confidence: Int
)

data class WeakChapter(
    val chapter: JeeChapter,
    val confidence: Int,
    val accuracy: Float?,
    val unresolvedMistakes: Int,
    val signals: List<String>
)

data class DailyPriority(
    val title: String,
    val subject: String,
    val chapterId: String? = null,
    val durationMin: Int,
    val level: PriorityLevel,
    val reason: String
)

data class WeeklyReview(
    val studyMinutes: Int,
    val sessions: Int,
    val revisions: Int,
    val tests: Int,
    val mistakes: Int,
    val averageAccuracy: Float,
    val weakChapters: List<String>,
    val improvementFocus: List<String>
)

class JeeIntelligence(
    private val jee: JeeRepository,
    private val learning: LearningRepository
) {
    private val zone = ZoneId.systemDefault()

    fun revisionRecommendations(limit: Int = 12): List<RevisionRecommendation> {
        val today = LocalDate.now()
        val queue = jee.getRevisionQueue().associateBy { it.chapter.id }
        return JeeCatalog.chapters.mapNotNull { chapter ->
            val state = jee.getChapterState(chapter.id)
            if (state.progress <= 0f) return@mapNotNull null
            val days = state.lastStudiedAt.let { studied ->
                val last = if (studied > 0L) jee.getRevisionHistory(chapter.id).maxByOrNull { it.reviewedAt }?.reviewedAt ?: 0L else 0L
                if (last <= 0L) 999L else ChronoUnit.DAYS.between(Instant.ofEpochMilli(last).atZone(zone).toLocalDate(), today)
            }
            val attempts = learning.getQuestionAttempts().filter { JeeCatalog.normalizeChapterId(it.chapterId) == chapter.id }
            val accuracy = if (attempts.size >= 3) attempts.count { it.correct }.toFloat() / attempts.size else null
            val mistakes = learning.getMistakes().count { JeeCatalog.normalizeChapterId(it.chapterId) == chapter.id && !it.resolved }
            var score = 0
            val reasons = mutableListOf<String>()
            val due = queue[chapter.id]
            if (due != null) {
                val overdue = ChronoUnit.DAYS.between(LocalDate.parse(due.revision.dueDate), today).coerceAtLeast(0)
                score += 25 + minOf(overdue, 14).toInt() * 3
                reasons += if (overdue > 0) "overdue by ${overdue}d" else "revision is due"
            } else if (days >= 7) { score += 20; reasons += "last revision was ${days}d ago" }
            if (state.confidence <= 2) { score += (3 - state.confidence) * 12; reasons += "confidence ${state.confidence}/5" }
            if (accuracy != null && accuracy < 0.65f) { score += ((0.65f - accuracy) * 70f).toInt(); reasons += "${(accuracy * 100).toInt()}% recent accuracy" }
            if (mistakes >= 2) { score += mistakes * 6; reasons += "$mistakes unresolved mistakes" }
            if (score == 0) return@mapNotNull null
            RevisionRecommendation(chapter, when { score >= 55 -> PriorityLevel.HIGH; score >= 30 -> PriorityLevel.MEDIUM; else -> PriorityLevel.LOW }, score, reasons.take(2).joinToString(" + "), days, state.confidence)
        }.sortedByDescending { it.score }.take(limit)
    }

    fun weakChapters(limit: Int = 8): List<WeakChapter> {
        val attempts = learning.getQuestionAttempts()
        val mistakes = learning.getMistakes()
        return JeeCatalog.chapters.mapNotNull { chapter ->
            val state = jee.getChapterState(chapter.id)
            val chapterAttempts = attempts.filter { JeeCatalog.normalizeChapterId(it.chapterId) == chapter.id }
            val accuracy = if (chapterAttempts.size >= 3) chapterAttempts.count { it.correct }.toFloat() / chapterAttempts.size else null
            val unresolved = mistakes.count { JeeCatalog.normalizeChapterId(it.chapterId) == chapter.id && !it.resolved }
            val gapDays = if (state.lastStudiedAt <= 0L) 999L else ChronoUnit.DAYS.between(Instant.ofEpochMilli(state.lastStudiedAt).atZone(zone).toLocalDate(), LocalDate.now())
            val signals = buildList {
                if (state.confidence <= 2) add("low confidence")
                if (unresolved >= 2) add("$unresolved unresolved mistakes")
                if (accuracy != null && accuracy < 0.65f) add("${(accuracy * 100).toInt()}% recent accuracy")
                if (gapDays >= 10) add("$gapDays days since study")
            }
            if (signals.size < 2) null else WeakChapter(chapter, state.confidence, accuracy, unresolved, signals.take(4))
        }.sortedWith(compareByDescending<WeakChapter> { it.signals.size }.thenBy { it.confidence }).take(limit)
    }

    /** Compatibility facade for existing UI; ranking is now delegated to the unified command center. */
    fun dailyPriorities(limit: Int = 5): List<DailyPriority> =
        DailyCommandCenter(jee, learning).actions(limit).map {
            DailyPriority(it.title, it.subject, it.chapterId, it.durationMin, it.priority, it.reason)
        }

    fun weeklyReview(): WeeklyReview {
        val cutoff = System.currentTimeMillis() - 7L * 86_400_000L
        val sessions = jee.getSessions().filter { it.startedAt >= cutoff }
        val revisions = jee.getRevisionHistory().count { it.reviewedAt >= cutoff }
        val tests = learning.getTestAttempts().count { it.completedAt >= cutoff }
        val mistakes = learning.getMistakes().count { it.lastSeenAt >= cutoff }
        val attempts = learning.getQuestionAttempts().filter { it.createdAt >= cutoff }
        val accuracy = if (attempts.isEmpty()) 0f else attempts.count { it.correct }.toFloat() / attempts.size
        val focus = buildList {
            if (sessions.isEmpty()) add("Establish a daily study baseline")
            if (revisions == 0) add("Clear the revision backlog")
            if (attempts.isNotEmpty() && accuracy < 0.65f) add("Improve question accuracy")
            if (tests == 0) add("Take one timed assessment")
            if (mistakes > 0) add("Resolve recurring mistakes")
        }
        return WeeklyReview(sessions.sumOf { it.minutes }, sessions.size, revisions, tests, mistakes, accuracy, weakChapters(5).map { it.chapter.name }, focus.take(4))
    }
}
