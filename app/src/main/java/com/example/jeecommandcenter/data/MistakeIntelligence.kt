package com.example.jeecommandcenter.data

/** Connects wrong attempts to actionable mistake categories and recurring patterns. */
class MistakeIntelligence(private val learning: LearningRepository) {
    data class Pattern(
        val type: MistakeType,
        val count: Int,
        val chapters: List<String>,
        val action: String
    )

    fun classify(attempt: QuestionAttemptRecord, question: Question): MistakeType {
        if (attempt.correct) return MistakeType.UNCLASSIFIED
        if (attempt.responseTimeSec >= 120) return MistakeType.TIME_PRESSURE
        if (attempt.selectedIndex < 0) return MistakeType.TIME_PRESSURE
        if (attempt.selectedIndex == question.correctIndex) return MistakeType.UNCLASSIFIED
        return when (question.difficulty) {
            5 -> MistakeType.CONCEPT
            4 -> MistakeType.METHOD
            else -> MistakeType.UNCLASSIFIED
        }
    }

    fun recurringPatterns(): List<Pattern> {
        val mistakes = learning.getMistakes().filterNot { it.resolved }
        return mistakes.groupBy { it.mistakeType }
            .map { (type, rows) ->
                Pattern(
                    type = type,
                    count = rows.sumOf { it.count },
                    chapters = rows.map { it.chapterId }.distinct().take(5),
                    action = when (type) {
                        MistakeType.CONCEPT -> "Relearn the underlying concept before more practice."
                        MistakeType.FORMULA -> "Review the formula and solve a short recall set."
                        MistakeType.CALCULATION -> "Slow down and verify intermediate arithmetic."
                        MistakeType.SILLY -> "Add a final substitution/sign/unit check."
                        MistakeType.MISREAD -> "Underline constraints and units before solving."
                        MistakeType.TIME_PRESSURE -> "Practice timed sets and improve question selection."
                        MistakeType.GUESS -> "Reduce blind guesses and review elimination strategy."
                        MistakeType.METHOD -> "Compare the failed method with the worked solution."
                        MistakeType.UNCLASSIFIED -> "Review the explanation and classify the mistake."
                    }
                )
            }
            .sortedByDescending { it.count }
    }

    fun unresolvedForChapter(chapterId: String): List<MistakeRecord> =
        learning.getMistakes().filter {
            !it.resolved && JeeCatalog.normalizeChapterId(it.chapterId) == JeeCatalog.normalizeChapterId(chapterId)
        }
}
