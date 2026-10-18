package com.example.jeecommandcenter.data

import android.content.Context
import java.time.LocalDate
import java.time.YearMonth

data class DailyLogEntry(
    val date: LocalDate,
    val completed: Boolean,
    val notes: String
)

/**
 * Small local-first journal. Editing is deliberately restricted to the device's current local date,
 * even if a caller accidentally tries to write a past or future entry.
 */
class DailyLogRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("daily_log_v1", Context.MODE_PRIVATE)

    fun getEntry(date: LocalDate): DailyLogEntry = DailyLogEntry(
        date = date,
        completed = prefs.getBoolean(completedKey(date), false),
        notes = prefs.getString(notesKey(date), "").orEmpty()
    )

    fun updateTodayCompletion(completed: Boolean): Boolean {
        val today = LocalDate.now()
        prefs.edit().putBoolean(completedKey(today), completed).apply()
        return true
    }

    fun updateTodayNotes(notes: String): Boolean {
        val today = LocalDate.now()
        prefs.edit().putString(notesKey(today), notes.take(MAX_NOTE_LENGTH)).apply()
        return true
    }

    fun completedCount(month: YearMonth): Int =
        (1..month.lengthOfMonth()).count { day ->
            prefs.getBoolean(completedKey(month.atDay(day)), false)
        }

    fun entriesFor(month: YearMonth): List<DailyLogEntry> =
        (1..month.lengthOfMonth()).map { getEntry(month.atDay(it)) }

    private fun completedKey(date: LocalDate) = "completed_${date}"
    private fun notesKey(date: LocalDate) = "notes_${date}"

    companion object {
        const val MAX_NOTE_LENGTH = 5000
    }
}
