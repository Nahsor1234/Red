package com.example.jeecommandcenter.data

class AiOrchestrator(context: android.content.Context) {
    private val appContext = context.applicationContext
    private val jee = JeeRepository(appContext)
    private val learning = LearningRepository(appContext)
    private val settings = AiSettingsRepository(appContext)

    suspend fun analyzePerformance(): AiResult {
        val intelligence = JeeIntelligence(jee, learning)
        val analytics = learning.analytics(jee)
        val weekly = intelligence.weeklyReview()
        val prompt = buildString {
            appendLine("Analyze this JEE student's preparation using only the supplied data.")
            appendLine("7-day study: ${analytics.studyMinutes7d} minutes")
            appendLine("Question accuracy: ${(analytics.accuracy * 100).toInt()}%")
            appendLine("Tests: ${analytics.testsCompleted}")
            appendLine("Average test score: ${analytics.averageTestScore.toInt()}%")
            appendLine("Unresolved mistakes: ${analytics.unresolvedMistakes}")
            appendLine("Repeated mistakes: ${analytics.repeatedMistakes}")
            appendLine("Weekly revisions: ${weekly.revisions}")
            appendLine("Weak chapters: ${intelligence.weakChapters(5).joinToString { it.chapter.name }}")
            appendLine("Return 3 observations, 3 concrete actions, and one risk to watch.")
        }
        return AiEngine(settings).ask(prompt)
    }

    suspend fun buildDailyStudyPlan(): AiResult {
        val priorities = JeeIntelligence(jee, learning).dailyPriorities(5)
        val prompt = buildString {
            appendLine("Turn this deterministic priority list into a practical JEE study plan.")
            priorities.forEachIndexed { index, item ->
                appendLine("${index + 1}. ${item.title} — ${item.durationMin} min — ${item.reason}")
            }
            appendLine("Do not invent chapters or student data.")
        }
        return AiEngine(settings).ask(prompt)
    }

    suspend fun explainMistakes(): AiResult {
        val mistakes = learning.getMistakes().filterNot { it.resolved }.take(8)
        val prompt = buildString {
            appendLine("Analyze these recurring JEE mistakes.")
            mistakes.forEach {
                appendLine(
                    "- ${it.subject} / ${it.questionPrompt}: " +
                        "type=${it.mistakeType}, repeats=${it.count}, correction=${it.correction}"
                )
            }
            appendLine("Return recurring patterns, root causes, and the next practice action.")
        }
        return AiEngine(settings).ask(prompt)
    }
}
