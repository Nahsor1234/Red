package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class RevisionNote(
    val id: String,
    val title: String,
    val subject: String,
    val chapter: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

/** Local-first storage for personal notes in the Notes & Revision library. */
class RevisionNotesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("jee_revision_notes", Context.MODE_PRIVATE)

    fun getNotes(): List<RevisionNote> {
        val raw = prefs.getString("notes", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                RevisionNote(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    subject = obj.optString("subject", "General"),
                    chapter = obj.optString("chapter", ""),
                    content = obj.optString("content", ""),
                    createdAt = obj.optLong("createdAt", 0L),
                    updatedAt = obj.optLong("updatedAt", obj.optLong("createdAt", 0L))
                )
            }.sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    fun saveNote(note: RevisionNote): RevisionNote {
        val now = System.currentTimeMillis()
        val existing = getNotes().firstOrNull { it.id == note.id }
        val saved = note.copy(
            id = note.id.ifBlank { UUID.randomUUID().toString() },
            title = note.title.trim(),
            subject = note.subject.ifBlank { "General" },
            chapter = note.chapter.trim(),
            content = note.content,
            createdAt = existing?.createdAt?.takeIf { it > 0L } ?: note.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now
        )
        val all = (getNotes().filterNot { it.id == saved.id } + saved)
            .sortedByDescending { it.updatedAt }
            .take(500)
        val array = JSONArray()
        all.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("subject", item.subject)
                put("chapter", item.chapter)
                put("content", item.content)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
            })
        }
        prefs.edit().putString("notes", array.toString()).apply()
        return saved
    }

    fun deleteNote(id: String) {
        val array = JSONArray()
        getNotes().filterNot { it.id == id }.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("subject", item.subject)
                put("chapter", item.chapter)
                put("content", item.content)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
            })
        }
        prefs.edit().putString("notes", array.toString()).apply()
    }
}
