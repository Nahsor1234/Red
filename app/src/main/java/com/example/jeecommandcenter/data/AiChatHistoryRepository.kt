package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AiHistoryEntry(
    val id: Long,
    val title: String,
    val prompt: String,
    val response: String,
    val createdAt: Long
)

class AiChatHistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ai_chat_history", Context.MODE_PRIVATE)

    fun getEntries(): List<AiHistoryEntry> {
        val raw = prefs.getString("entries", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                AiHistoryEntry(
                    id = obj.getLong("id"),
                    title = obj.getString("title"),
                    prompt = obj.getString("prompt"),
                    response = obj.getString("response"),
                    createdAt = obj.getLong("createdAt")
                )
            }.sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    fun save(title: String, prompt: String, response: String) {
        if (prompt.isBlank() || response.isBlank()) return
        val now = System.currentTimeMillis()
        val items = (listOf(
            AiHistoryEntry(now, title.ifBlank { "Study coach" }, prompt.trim(), response.trim(), now)
        ) + getEntries()).take(100)
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("prompt", item.prompt)
                put("response", item.response)
                put("createdAt", item.createdAt)
            })
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }
}
