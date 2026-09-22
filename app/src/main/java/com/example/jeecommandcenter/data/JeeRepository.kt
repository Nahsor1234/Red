package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.ceil

data class AppTask(
    val id: Long,
    val title: String,
    val subject: String,
    val durationMin: Int,
    val dueDay: String,
    val done: Boolean = false
)

data class StudySession(
    val id: Long,
    val date: String,
    val minutes: Int,
    val subject: String,
    val chapter: String,
    val startedAt: Long = id
)

data class ChapterProgress(
    val subject: String,
    val number: Int,
    val name: String,
    val progress: Float
)

data class TimerState(
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val running: Boolean,
    val endAtMillis: Long
)

class JeeRepository(context: Context) {

    private val prefs = context.getSharedPreferences("jee_command_center", Context.MODE_PRIVATE)

    fun getTasks(): List<AppTask> {
        val raw = prefs.getString("tasks", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                AppTask(
                    id = obj.getLong("id"),
                    title = obj.getString("title"),
                    subject = obj.getString("subject"),
                    durationMin = obj.getInt("duration"),
                    dueDay = obj.getString("dueDay"),
                    done = obj.optBoolean("done")
                )
            }.sortedBy { it.id }
        }.getOrDefault(emptyList())
    }

    fun addTask(
        title: String,
        subject: String,
        durationMin: Int,
        dueDay: String = "Today"
    ): Boolean {
        val cleanTitle = title.trim()
        val cleanSubject = subject.trim().ifBlank { "General" }
        if (cleanTitle.isBlank() || durationMin !in 1..1440) return false

        val tasks = getTasks().toMutableList()
        var id = System.currentTimeMillis()
        while (tasks.any { it.id == id }) id++

        tasks.add(
            AppTask(
                id = id,
                title = cleanTitle,
                subject = cleanSubject,
                durationMin = durationMin,
                dueDay = if (dueDay == "Today") "Today" else "Upcoming"
            )
        )
        saveTasks(tasks)
        return true
    }

    fun toggleTask(id: Long) {
        saveTasks(getTasks().map { task ->
            if (task.id == id) task.copy(done = !task.done) else task
        })
    }

    fun deleteTask(id: Long) {
        saveTasks(getTasks().filterNot { it.id == id })
    }

    private fun saveTasks(tasks: List<AppTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("subject", task.subject)
                    put("duration", task.durationMin)
                    put("dueDay", task.dueDay)
                    put("done", task.done)
                }
            )
        }
        prefs.edit().putString("tasks", array.toString()).apply()
    }

    fun getSessions(): List<StudySession> {
        val raw = prefs.getString("sessions", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                StudySession(
                    id = obj.getLong("id"),
                    date = obj.getString("date"),
                    minutes = obj.getInt("minutes"),
                    subject = obj.getString("subject"),
                    chapter = obj.getString("chapter"),
                    startedAt = obj.optLong("startedAt", obj.getLong("id"))
                )
            }.sortedByDescending { it.startedAt }
        }.getOrDefault(emptyList())
    }

    fun addSession(
        minutes: Int,
        subject: String = "General",
        chapter: String = "Self study",
        startedAt: Long = System.currentTimeMillis()
    ) {
        if (minutes <= 0) return

        val sessions = getSessions().toMutableList()
        var id = startedAt
        while (sessions.any { it.id == id }) id++

        sessions.add(
            StudySession(
                id = id,
                date = LocalDate.now().toString(),
                minutes = minutes,
                subject = subject,
                chapter = chapter,
                startedAt = startedAt
            )
        )

        val array = JSONArray()
        sessions.sortedByDescending { it.startedAt }.take(500).forEach { session ->
            array.put(
                JSONObject().apply {
                    put("id", session.id)
                    put("date", session.date)
                    put("minutes", session.minutes)
                    put("subject", session.subject)
                    put("chapter", session.chapter)
                    put("startedAt", session.startedAt)
                }
            )
        }
        prefs.edit().putString("sessions", array.toString()).apply()
    }

    fun getTodayMinutes(): Int =
        getSessions()
            .filter { it.date == LocalDate.now().toString() }
            .sumOf { it.minutes }

    fun getDailyGoalMinutes(): Int = prefs.getInt("daily_goal", 360)

    fun setDailyGoalMinutes(value: Int) =
        prefs.edit().putInt("daily_goal", value.coerceIn(15, 1440)).apply()

    fun getChapterProgress(subject: String, number: Int): Float =
        prefs.getFloat("chapter_${subject}_$number", 0f)

    fun setChapterProgress(subject: String, number: Int, progress: Float) =
        prefs.edit()
            .putFloat("chapter_${subject}_$number", progress.coerceIn(0f, 1f))
            .apply()

    fun chapters(subject: String): List<ChapterProgress> =
        (syllabus[subject] ?: emptyList()).mapIndexed { index, name ->
            ChapterProgress(
                subject = subject,
                number = index + 1,
                name = name,
                progress = getChapterProgress(subject, index + 1)
            )
        }

    fun restoreTimerState(): TimerState {
        val total = prefs.getInt("timer_total_seconds", DEFAULT_TIMER_SECONDS)
            .coerceIn(MIN_TIMER_SECONDS, MAX_TIMER_SECONDS)
        val running = prefs.getBoolean("timer_running", false)
        val endAt = prefs.getLong("timer_end_at", 0L)
        val storedRemaining = prefs.getInt("timer_remaining_seconds", total)
            .coerceIn(0, total)

        if (!running || endAt <= 0L) {
            return TimerState(total, storedRemaining, false, 0L)
        }

        val remaining = ceil(
            (endAt - System.currentTimeMillis()).coerceAtLeast(0L) / 1000.0
        ).toInt().coerceIn(0, total)

        if (remaining == 0) {
            completeTimer(total)
            return TimerState(total, 0, false, 0L)
        }

        prefs.edit().putInt("timer_remaining_seconds", remaining).apply()
        return TimerState(total, remaining, true, endAt)
    }

    fun resetTimer(totalSeconds: Int) {
        val safeTotal = totalSeconds.coerceIn(MIN_TIMER_SECONDS, MAX_TIMER_SECONDS)
        prefs.edit()
            .putInt("timer_total_seconds", safeTotal)
            .putInt("timer_remaining_seconds", safeTotal)
            .putBoolean("timer_running", false)
            .remove("timer_end_at")
            .apply()
    }

    fun startTimer(totalSeconds: Int, remainingSeconds: Int) {
        val safeTotal = totalSeconds.coerceIn(MIN_TIMER_SECONDS, MAX_TIMER_SECONDS)
        val safeRemaining = remainingSeconds.coerceIn(1, safeTotal)
        prefs.edit()
            .putInt("timer_total_seconds", safeTotal)
            .putInt("timer_remaining_seconds", safeRemaining)
            .putBoolean("timer_running", true)
            .putLong("timer_end_at", System.currentTimeMillis() + safeRemaining * 1000L)
            .apply()
    }

    fun pauseTimer(totalSeconds: Int, remainingSeconds: Int) {
        val safeTotal = totalSeconds.coerceIn(MIN_TIMER_SECONDS, MAX_TIMER_SECONDS)
        val safeRemaining = remainingSeconds.coerceIn(0, safeTotal)
        prefs.edit()
            .putInt("timer_total_seconds", safeTotal)
            .putInt("timer_remaining_seconds", safeRemaining)
            .putBoolean("timer_running", false)
            .remove("timer_end_at")
            .apply()
    }

    fun completeTimer(totalSeconds: Int) {
        if (totalSeconds >= 60) {
            addSession(minutes = totalSeconds / 60)
        }
        prefs.edit()
            .remove("timer_total_seconds")
            .remove("timer_remaining_seconds")
            .remove("timer_running")
            .remove("timer_end_at")
            .apply()
    }

    companion object {
        private const val DEFAULT_TIMER_SECONDS = 25 * 60
        private const val MIN_TIMER_SECONDS = 60
        private const val MAX_TIMER_SECONDS = 24 * 60 * 60

        val syllabus = mapOf(
            "Physics" to listOf(
                "Units and measurements", "Motion in a straight line", "Motion in a plane",
                "Laws of motion", "Work, energy and power", "System of particles",
                "Rotational motion", "Gravitation", "Mechanical properties of solids",
                "Mechanical properties of fluids", "Thermal properties of matter",
                "Thermodynamics", "Kinetic theory", "Oscillations", "Waves",
                "Electric charges and fields", "Electrostatic potential and capacitance",
                "Current electricity", "Moving charges and magnetism", "Magnetism and matter",
                "Electromagnetic induction", "Alternating current", "Electromagnetic waves",
                "Ray optics", "Wave optics", "Dual nature of matter", "Atoms", "Nuclei",
                "Semiconductor electronics"
            ),
            "Chemistry" to listOf(
                "Some basic concepts of chemistry", "Structure of atom",
                "Classification of elements", "Chemical bonding", "Thermodynamics",
                "Equilibrium", "Redox reactions", "Organic chemistry basics", "Hydrocarbons",
                "Solutions", "Electrochemistry", "Chemical kinetics", "Surface chemistry",
                "p-Block elements", "d- and f-Block elements", "Coordination compounds",
                "Haloalkanes and haloarenes", "Alcohols, phenols and ethers",
                "Aldehydes, ketones and carboxylic acids", "Amines",
                "Biomolecules and polymers", "Practical chemistry"
            ),
            "Mathematics" to listOf(
                "Sets", "Relations and functions", "Trigonometric functions",
                "Complex numbers", "Quadratic equations", "Sequences and series",
                "Permutations and combinations", "Binomial theorem", "Straight lines",
                "Circles", "Conic sections", "Limits", "Continuity and differentiability",
                "Application of derivatives", "Integrals", "Application of integrals",
                "Differential equations", "Matrices", "Determinants", "Vector algebra",
                "Three dimensional geometry", "Statistics", "Probability",
                "Mathematical reasoning"
            )
        )
    }
}
