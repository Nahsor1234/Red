package com.example.jeecommandcenter.data

/**
 * Deterministic daily command center. It ranks actionable work from the existing
 * revision, performance, mistake, test and task signals without creating new state.
 */
class DailyCommandCenter(
    private val jee: JeeRepository,
    private val learning: LearningRepository
) {
    data class Action(
        val title: String,
        val subject: String,
        val chapterId: String?,
        val durationMin: Int,
        val priority: PriorityLevel,
        val reason: String,
        val source: Source
    )

    enum class Source { REVISION, WEAKNESS, MISTAKE, TEST, TASK }

    private val finalIntelligence = FinalIntelligence(jee, learning)

    fun actions(limit: Int = 5): List<Action> {
        val result = mutableListOf<Action>()

        finalIntelligence.revisionSignals(3).forEach { signal ->
            result += Action(
                title = "Revise ${signal.chapter.name}",
                subject = signal.chapter.subject,
                chapterId = signal.chapter.id,
                durationMin = 30,
                priority = signal.priority,
                reason = signal.reason,
                source = Source.REVISION
            )
        }

        finalIntelligence.weakChapters(3).forEach { item ->
            if (result.any { it.chapterId == item.chapter.id }) return@forEach
            result += Action(
                title = "Practice ${item.chapter.name}",
                subject = item.chapter.subject,
                chapterId = item.chapter.id,
                durationMin = 35,
                priority = if (item.strengthScore < 35) PriorityLevel.HIGH else PriorityLevel.MEDIUM,
                reason = buildList {
                    if (item.accuracy != null) add("${(item.accuracy * 100).toInt()}% accuracy")
                    if (item.unresolvedMistakes > 0) add("${item.unresolvedMistakes} unresolved mistakes")
                }.ifEmpty { listOf("limited recent evidence") }.joinToString(" + "),
                source = Source.WEAKNESS
            )
        }

        finalIntelligence.topMistakePatterns(2).forEach { pattern ->
            result += Action(
                title = "Review ${pattern.type.name.lowercase().replace('_', ' ')} mistakes",
                subject = "All subjects",
                chapterId = pattern.chapters.firstOrNull(),
                durationMin = 20,
                priority = if (pattern.count >= 3) PriorityLevel.HIGH else PriorityLevel.MEDIUM,
                reason = pattern.action,
                source = Source.MISTAKE
            )
        }

        finalIntelligence.testFocus()?.let { focus ->
            result += Action(
                title = focus.title,
                subject = focus.subject,
                chapterId = focus.chapterId,
                durationMin = focus.durationMin,
                priority = focus.priority,
                reason = focus.reason,
                source = Source.TEST
            )
        }

        jee.getTasks().filter { !it.done && it.dueDay == "Today" }.take(3).forEach { task ->
            result += Action(
                title = task.title,
                subject = task.subject,
                chapterId = task.chapterId,
                durationMin = task.durationMin.coerceAtLeast(10),
                priority = PriorityLevel.MEDIUM,
                reason = "unfinished task due today",
                source = Source.TASK
            )
        }

        return result
            .distinctBy { it.chapterId ?: it.title }
            .sortedWith(compareByDescending<Action> {
                when (it.priority) { PriorityLevel.HIGH -> 3; PriorityLevel.MEDIUM -> 2; PriorityLevel.LOW -> 1 }
            }.thenByDescending { it.source.ordinal })
            .take(limit)
    }
}
