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
            appendLine("7-day study: __DOLLAR__{analytics.studyMinutes7d} minutes")
            appendLine("Question accuracy: __DOLLAR__{(analytics.accuracy * 100).toInt()}%")
            appendLine("Tests: __DOLLAR__{analytics.testsCompleted}")
            appendLine("Average test score: __DOLLAR__{analytics.averageTestScore.toInt()}%")
            appendLine("Unresolved mistakes: __DOLLAR__{analytics.unresolvedMistakes}")
            appendLine("Repeated mistakes: __DOLLAR__{analytics.repeatedMistakes}")
            appendLine("Weekly revisions: __DOLLAR__{weekly.revisions}")
            appendLine("Weak chapters: __DOLLAR__{intelligence.weakChapters(5).joinToString { it.chapter.name }}")
            appendLine("Return 3 observations, 3 concrete actions, and one risk to watch.")
        }.replace("__DOLLAR__", "$")
        return AiEngine(settings).ask(prompt)
    }

    suspend fun buildDailyStudyPlan(): AiResult {
        val priorities = JeeIntelligence(jee, learning).dailyPriorities(5)
        val prompt = buildString {
            appendLine("Turn this deterministic priority list into a practical JEE study plan.")
            priorities.forEachIndexed { index, item ->
                appendLine("__DOLLAR__{index + 1}. __DOLLAR__{item.title} — __DOLLAR__{item.durationMin} min — __DOLLAR__{item.reason}")
            }
            appendLine("Do not invent chapters or student data.")
        }.replace("__DOLLAR__", "$")
        return AiEngine(settings).ask(prompt)
    }

    suspend fun explainMistakes(): AiResult {
        val mistakes = learning.getMistakes().filterNot { it.resolved }.take(8)
        val prompt = buildString {
            appendLine("Analyze these recurring JEE mistakes.")
            mistakes.forEach {
                appendLine(
                    "- __DOLLAR__{it.subject} / __DOLLAR__{it.questionPrompt}: " +
                        "type=__DOLLAR__{it.mistakeType}, repeats=__DOLLAR__{it.count}, correction=__DOLLAR__{it.correction}"
                )
            }
            appendLine("Return recurring patterns, root causes, and the next practice action.")
        }.replace("__DOLLAR__", "$")
        return AiEngine(settings).ask(prompt)
    }
}
