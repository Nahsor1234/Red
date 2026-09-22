package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

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
        prefs.getFloat("chapter_" + subject + "_" + number, 0f)

    fun setChapterProgress(subject: String, number: Int, progress: Float) {
        val chapter = JeeCatalog.forSubject(subject).firstOrNull { it.number == number } ?: return
        val current = getChapterState(chapter.id)
        setChapterState(chapter.id, progress, current.confidence)
        prefs.edit()
            .putFloat(
                "chapter_" + subject + "_" + number,
                progress.coerceIn(0f, 1f)
            )
            .apply()
        if (progress > 0f) ensureRevisionItem(chapter.id)
    }

    fun chapters(subject: String): List<ChapterProgress> =
        JeeCatalog.forSubject(subject).map { chapter ->
            val state = getChapterState(chapter.id)
            ChapterProgress(
                subject = chapter.subject,
                number = chapter.number,
                name = chapter.name,
                progress = state.progress,
                confidence = state.confidence,
                difficulty = chapter.difficulty,
                estimatedMinutes = chapter.estimatedMinutes
            )
        }

    fun getChapterState(chapterId: String): ChapterState {
        val raw = prefs.getString("chapter_state_" + chapterId, null)
        if (raw.isNullOrBlank()) {
            val chapter = JeeCatalog.find(chapterId)
            return ChapterState(
                chapterId = chapterId,
                progress = chapter?.let { getChapterProgress(it.subject, it.number) } ?: 0f
            )
        }
        return runCatching {
            val obj = JSONObject(raw)
            ChapterState(
                chapterId = chapterId,
                progress = obj.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f),
                confidence = obj.optInt("confidence", 0).coerceIn(0, 5),
                lastStudiedAt = obj.optLong("lastStudiedAt", 0L)
            )
        }.getOrDefault(ChapterState(chapterId, 0f))
    }

    fun setChapterState(
        chapterId: String,
        progress: Float,
        confidence: Int,
        studiedAt: Long = System.currentTimeMillis()
    ) {
        val safeProgress = progress.coerceIn(0f, 1f)
        val safeConfidence = confidence.coerceIn(0, 5)
        prefs.edit()
            .putString(
                "chapter_state_" + chapterId,
                JSONObject().apply {
                    put("progress", safeProgress)
                    put("confidence", safeConfidence)
                    put("lastStudiedAt", studiedAt)
                }.toString()
            )
            .apply()
    }

    fun flagChapterWeak(chapterId: String) {
        val chapter = JeeCatalog.find(chapterId) ?: return
        val state = getChapterState(chapter.id)
        setChapterState(chapter.id, state.progress, minOf(1, state.confidence))
        val items = getRevisionItems()
        val existing = items.firstOrNull { it.chapterId == chapter.id }
        val updated = existing?.copy(
            dueDate = LocalDate.now().toString(),
            lapses = existing.lapses + 1
        ) ?: RevisionItem(
            id = "rev_" + chapter.id,
            chapterId = chapter.id,
            dueDate = LocalDate.now().toString(),
            lapses = 1
        )
        saveRevisionItems(items.filterNot { it.id == updated.id } + updated)
    }

    fun getRevisionQueue(): List<RevisionQueueItem> {
        val today = LocalDate.now()
        return getRevisionItems()
            .mapNotNull { revision ->
                val chapter = JeeCatalog.find(revision.chapterId)
                    ?: return@mapNotNull null
                val state = getChapterState(chapter.id)
                val due = runCatching { LocalDate.parse(revision.dueDate) }.getOrNull()
                    ?: today
                if (due.isAfter(today)) return@mapNotNull null
                RevisionQueueItem(chapter, revision, state.progress, state.confidence)
            }
            .sortedWith(
                compareByDescending<RevisionQueueItem> {
                    ChronoUnit.DAYS.between(LocalDate.parse(it.revision.dueDate), today)
                }.thenByDescending { it.chapter.difficulty }
            )
    }

    fun reviewChapter(chapterId: String, rating: RevisionRating) {
        val current = getRevisionItems().firstOrNull { it.chapterId == chapterId }
            ?: ensureRevisionItem(chapterId)

        val nextInterval = when (rating) {
            RevisionRating.AGAIN -> 1
            RevisionRating.HARD -> maxOf(2, (current.intervalDays * 1.5f).roundToInt())
            RevisionRating.GOOD -> maxOf(3, (current.intervalDays * current.ease).roundToInt())
            RevisionRating.EASY -> maxOf(5, (current.intervalDays * (current.ease + 1f)).roundToInt())
        }
        val nextEase = when (rating) {
            RevisionRating.AGAIN -> maxOf(1.3f, current.ease - 0.2f)
            RevisionRating.HARD -> maxOf(1.3f, current.ease - 0.05f)
            RevisionRating.GOOD -> minOf(3.2f, current.ease + 0.05f)
            RevisionRating.EASY -> minOf(3.5f, current.ease + 0.15f)
        }
        val confidence = when (rating) {
            RevisionRating.AGAIN -> 1
            RevisionRating.HARD -> 2
            RevisionRating.GOOD -> 4
            RevisionRating.EASY -> 5
        }
        val now = System.currentTimeMillis()
        val state = getChapterState(chapterId)
        setChapterState(chapterId, state.progress, confidence, now)

        val updated = current.copy(
            dueDate = LocalDate.now().plusDays(nextInterval.toLong()).toString(),
            intervalDays = nextInterval,
            ease = nextEase,
            repetitions = current.repetitions + 1,
            lapses = current.lapses + if (rating == RevisionRating.AGAIN) 1 else 0,
            lastReviewedAt = now
        )
        saveRevisionItems(
            getRevisionItems().map { if (it.id == current.id) updated else it }
        )
    }

    fun getMasteredChapterCount(): Int =
        JeeCatalog.chapters.count { getChapterState(it.id).mastered }

    fun getActiveRevisionCount(): Int =
        getRevisionItems().count { getChapterState(it.chapterId).progress > 0f }

    fun getOrCreateDailyPlan(learning: LearningRepository? = null): DailyPlan {
        val today = LocalDate.now().toString()
        getDailyPlan()?.takeIf { it.date == today }?.let { return it }
        return regenerateDailyPlan(learning)
    }

    fun regenerateDailyPlan(learning: LearningRepository? = null): DailyPlan {
        val today = LocalDate.now().toString()
        val goal = getDailyGoalMinutes()
        val selected = mutableListOf<PlannerItem>()
        var remaining = goal

        getTasks()
            .asSequence()
            .filter { !it.done && it.dueDay == "Today" }
            .sortedByDescending { it.durationMin }
            .take(3)
            .forEach { task ->
                if (remaining <= 0) return@forEach
                val duration = minOf(task.durationMin, remaining)
                if (duration < 15) return@forEach
                selected += PlannerItem(
                    id = "task_" + task.id,
                    date = today,
                    type = PlannerItemType.PRACTICE,
                    title = task.title,
                    subject = task.subject,
                    durationMin = duration,
                    priority = 100
                )
                remaining -= duration
            }

        getRevisionQueue()
            .take(3)
            .forEach { due ->
                if (remaining <= 0) return@forEach
                val duration = minOf(30, remaining)
                if (duration < 15) return@forEach
                selected += PlannerItem(
                    id = "revplan_" + due.chapter.id,
                    date = today,
                    type = PlannerItemType.REVISION,
                    title = "Revise " + due.chapter.name,
                    subject = due.chapter.subject,
                    chapterId = due.chapter.id,
                    durationMin = duration,
                    priority = 90
                )
                remaining -= duration
            }

        val candidates = JeeCatalog.chapters
            .map { chapter ->
                val state = getChapterState(chapter.id)
                val daysSinceStudy = if (state.lastStudiedAt == 0L) 999
                else ((System.currentTimeMillis() - state.lastStudiedAt) / 86_400_000L).toInt()
                val repeatedMistakes = learning?.getMistakes()
                    ?.count { it.chapterId == chapter.id && !it.resolved } ?: 0
                val score =
                    (1f - state.progress) * 100f +
                        (5 - state.confidence) * 9f +
                        chapter.difficulty * 5f +
                        minOf(daysSinceStudy, 30) * 0.5f +
                        repeatedMistakes * 18f
                chapter to score
            }
            .sortedByDescending { it.second }

        val subjectCount = mutableMapOf<String, Int>()
        for ((chapter, _) in candidates) {
            if (remaining <= 0) break
            if (selected.any { it.chapterId == chapter.id }) continue
            val count = subjectCount[chapter.subject] ?: 0
            if (count >= 2 && subjectCount.size < 3) continue

            val state = getChapterState(chapter.id)
            val duration = minOf(
                remaining,
                when {
                    state.progress == 0f -> 50
                    state.progress < 1f -> 40
                    else -> 25
                }
            )
            if (duration < 15) break

            val type = if (state.progress >= 0.5f)
                PlannerItemType.PRACTICE
            else
                PlannerItemType.STUDY

            selected += PlannerItem(
                id = "plan_" + chapter.id,
                date = today,
                type = type,
                title = if (type == PlannerItemType.STUDY)
                    "Study " + chapter.name
                else
                    "Practice " + chapter.name,
                subject = chapter.subject,
                chapterId = chapter.id,
                durationMin = duration,
                priority = chapter.difficulty
            )
            subjectCount[chapter.subject] = count + 1
            remaining -= duration
        }

        val plan = DailyPlan(today, goal, selected)
        saveDailyPlan(plan)
        return plan
    }

    fun completePlannerItem(id: String) {
        val plan = getDailyPlan() ?: return
        val item = plan.items.firstOrNull { it.id == id } ?: return
        if (item.completed) return

        if (item.id.startsWith("task_")) {
            item.id.removePrefix("task_").toLongOrNull()?.let { taskId ->
                val task = getTasks().firstOrNull { it.id == taskId }
                if (task != null && !task.done) toggleTask(taskId)
            }
        } else if (item.type == PlannerItemType.REVISION && item.chapterId != null) {
            reviewChapter(item.chapterId, RevisionRating.GOOD)
        } else if (item.chapterId != null) {
            val state = getChapterState(item.chapterId)
            val increment = if (item.type == PlannerItemType.STUDY) 0.20f else 0.10f
            setChapterState(
                item.chapterId,
                (state.progress + increment).coerceAtMost(1f),
                state.confidence.coerceAtLeast(1)
            )
        }

        saveDailyPlan(
            plan.copy(
                items = plan.items.map { if (it.id == id) it.copy(completed = true) else it }
            )
        )
    }

    private fun getRevisionItems(): List<RevisionItem> {
        val raw = prefs.getString("revision_items", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                RevisionItem(
                    id = obj.getString("id"),
                    chapterId = obj.getString("chapterId"),
                    dueDate = obj.getString("dueDate"),
                    intervalDays = obj.optInt("intervalDays", 1),
                    ease = obj.optDouble("ease", 2.5).toFloat(),
                    repetitions = obj.optInt("repetitions", 0),
                    lapses = obj.optInt("lapses", 0),
                    lastReviewedAt = obj.optLong("lastReviewedAt", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveRevisionItems(items: List<RevisionItem>) {
        val array = JSONArray()
        items.distinctBy { it.id }.take(500).forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("chapterId", item.chapterId)
                    put("dueDate", item.dueDate)
                    put("intervalDays", item.intervalDays)
                    put("ease", item.ease)
                    put("repetitions", item.repetitions)
                    put("lapses", item.lapses)
                    put("lastReviewedAt", item.lastReviewedAt)
                }
            )
        }
        prefs.edit().putString("revision_items", array.toString()).apply()
    }

    private fun ensureRevisionItem(chapterId: String): RevisionItem {
        getRevisionItems().firstOrNull { it.chapterId == chapterId }?.let { return it }
        val item = RevisionItem(
            id = "rev_" + chapterId,
            chapterId = chapterId,
            dueDate = LocalDate.now().plusDays(1).toString()
        )
        saveRevisionItems(getRevisionItems() + item)
        return item
    }

    private fun getDailyPlan(): DailyPlan? {
        val raw = prefs.getString("daily_plan", null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val array = obj.optJSONArray("items") ?: JSONArray()
            val items = List(array.length()) { index ->
                val item = array.getJSONObject(index)
                PlannerItem(
                    id = item.getString("id"),
                    date = obj.getString("date"),
                    type = PlannerItemType.valueOf(item.getString("type")),
                    title = item.getString("title"),
                    subject = item.getString("subject"),
                    chapterId = item.optString("chapterId").ifBlank { null },
                    durationMin = item.getInt("durationMin"),
                    priority = item.optInt("priority", 0),
                    completed = item.optBoolean("completed")
                )
            }
            DailyPlan(
                date = obj.getString("date"),
                goalMinutes = obj.getInt("goalMinutes"),
                items = items
            )
        }.getOrNull()
    }

    private fun saveDailyPlan(plan: DailyPlan) {
        val array = JSONArray()
        plan.items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type.name)
                    put("title", item.title)
                    put("subject", item.subject)
                    put("chapterId", item.chapterId ?: "")
                    put("durationMin", item.durationMin)
                    put("priority", item.priority)
                    put("completed", item.completed)
                }
            )
        }
        prefs.edit()
            .putString(
                "daily_plan",
                JSONObject().apply {
                    put("date", plan.date)
                    put("goalMinutes", plan.goalMinutes)
                    put("items", array)
                }.toString()
            )
            .apply()
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
        val syllabus: Map<String, List<String>>
            get() = JeeCatalog.chapters
                .groupBy { it.subject }
                .mapValues { entry -> entry.value.sortedBy { it.number }.map { it.name } }

        private const val DEFAULT_TIMER_SECONDS = 25 * 60
        private const val MIN_TIMER_SECONDS = 60
        private const val MAX_TIMER_SECONDS = 24 * 60 * 60

    }
}
