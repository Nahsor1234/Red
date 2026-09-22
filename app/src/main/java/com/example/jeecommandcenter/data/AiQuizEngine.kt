package com.example.jeecommandcenter.data

import java.util.UUID
import org.json.JSONArray

class AiQuizEngine(private val engine: AiEngine) {
    suspend fun generate(subject: String, difficulty: String, count: Int): AiResultWithQuestions {
        val prompt = """
Create a JEE practice quiz for $subject.
Difficulty: $difficulty.
Number of questions: $count.
Return ONLY a JSON array, no markdown fences and no commentary.
Each item must have: question, options (exactly 4 strings), correctIndex (0-3), explanation, chapter, topic.
Questions must be self-contained, mathematically/scientifically coherent, and appropriate for JEE Main practice.
""".trimIndent()
        val result = engine.ask(prompt, "You generate reliable JEE multiple-choice questions. Never omit the correct answer or explanation.")
        if (!result.success) return AiResultWithQuestions(false, error = result.error)
        return runCatching { AiResultWithQuestions(true, questions = parse(result.text, subject, difficulty)) }
            .getOrElse { AiResultWithQuestions(false, error = "AI returned an invalid quiz. Please try again.") }
    }

    private fun parse(raw: String, subject: String, difficulty: String): List<Question> {
        val cleaned = raw.replace("```json", "", ignoreCase = true).replace("```", "").trim()
        val array = JSONArray(cleaned)
        require(array.length() > 0)
        return List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            val chapterName = obj.optString("chapter", "General")
            val chapter = JeeCatalog.forSubject(subject).firstOrNull { it.name.equals(chapterName, true) }
                ?: JeeCatalog.forSubject(subject).firstOrNull()
                ?: JeeChapter("$subject-0", subject, 0, chapterName)
            val optionsJson = obj.getJSONArray("options")
            require(optionsJson.length() == 4)
            Question(
                id = "ai_${UUID.randomUUID()}", subject = subject, chapterId = chapter.id, chapter = chapter.name,
                topic = obj.optString("topic", "Mixed practice"), prompt = obj.getString("question"),
                options = List(4) { optionsJson.getString(it) }, correctIndex = obj.getInt("correctIndex").coerceIn(0, 3),
                explanation = obj.optString("explanation", "Review the underlying concept."),
                difficulty = when (difficulty.lowercase()) { "easy" -> 2; "hard" -> 4; else -> 3 }, source = "AI-generated practice"
            )
        }
    }
}

data class AiResultWithQuestions(val success: Boolean, val questions: List<Question> = emptyList(), val error: String? = null)
