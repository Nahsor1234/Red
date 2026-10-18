package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CustomReminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val hour: Int,
    val minute: Int,
    val repeat: String = REPEAT_DAILY,
    val enabled: Boolean = true,
    val lastTriggered: Long = 0L
) {
    companion object {
        const val REPEAT_DAILY = "Daily"
        const val REPEAT_WEEKDAYS = "Weekdays"
        const val REPEAT_ONCE = "Once"
        val repeatOptions = listOf(REPEAT_DAILY, REPEAT_WEEKDAYS, REPEAT_ONCE)
    }
}

/** Local-first storage for user-created reminders. */
class CustomReminderRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(): List<CustomReminder> {
        val source = prefs.getString(KEY_REMINDERS, "[]") ?: "[]"
        return runCatching {
            val json = JSONArray(source)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.optJSONObject(index) ?: continue
                    add(
                        CustomReminder(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            title = item.optString("title").take(MAX_TITLE_LENGTH),
                            message = item.optString("message").take(MAX_MESSAGE_LENGTH),
                            hour = item.optInt("hour", 18).coerceIn(0, 23),
                            minute = item.optInt("minute", 0).coerceIn(0, 59),
                            repeat = item.optString("repeat", CustomReminder.REPEAT_DAILY).takeIf { it in CustomReminder.repeatOptions } ?: CustomReminder.REPEAT_DAILY,
                            enabled = item.optBoolean("enabled", true),
                            lastTriggered = item.optLong("lastTriggered", 0L)
                        )
                    )
                }
            }.sortedWith(compareBy<CustomReminder> { it.hour }.thenBy { it.minute }.thenBy { it.title.lowercase() })
        }.getOrDefault(emptyList())
    }

    fun find(id: String): CustomReminder? = all().firstOrNull { it.id == id }

    fun save(reminder: CustomReminder) {
        val sanitized = reminder.copy(
            title = reminder.title.trim().take(MAX_TITLE_LENGTH),
            message = reminder.message.trim().take(MAX_MESSAGE_LENGTH),
            hour = reminder.hour.coerceIn(0, 23),
            minute = reminder.minute.coerceIn(0, 59),
            repeat = reminder.repeat.takeIf { it in CustomReminder.repeatOptions } ?: CustomReminder.REPEAT_DAILY
        )
        val updated = all().filterNot { it.id == sanitized.id } + sanitized
        persist(updated)
    }

    fun delete(id: String) {
        persist(all().filterNot { it.id == id })
    }

    fun setEnabled(id: String, enabled: Boolean): CustomReminder? {
        val reminder = find(id) ?: return null
        return reminder.copy(enabled = enabled).also(::save)
    }

    fun markTriggered(id: String, timeMillis: Long): CustomReminder? {
        val reminder = find(id) ?: return null
        return reminder.copy(
            lastTriggered = timeMillis,
            enabled = if (reminder.repeat == CustomReminder.REPEAT_ONCE) false else reminder.enabled
        ).also(::save)
    }

    private fun persist(reminders: List<CustomReminder>) {
        val json = JSONArray()
        reminders.forEach { reminder ->
            json.put(
                JSONObject()
                    .put("id", reminder.id)
                    .put("title", reminder.title)
                    .put("message", reminder.message)
                    .put("hour", reminder.hour)
                    .put("minute", reminder.minute)
                    .put("repeat", reminder.repeat)
                    .put("enabled", reminder.enabled)
                    .put("lastTriggered", reminder.lastTriggered)
            )
        }
        prefs.edit().putString(KEY_REMINDERS, json.toString()).apply()
    }

    companion object {
        private const val PREFS = "custom_reminders_v1"
        private const val KEY_REMINDERS = "reminders"
        const val MAX_TITLE_LENGTH = 80
        const val MAX_MESSAGE_LENGTH = 240
    }
}
