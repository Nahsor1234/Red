package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AiChatMessage(
    val id: Long,
    val role: String,
    val text: String,
    val createdAt: Long
)

data class AiConversation(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messages: List<AiChatMessage>
)

class AiChatHistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ai_chat_history", Context.MODE_PRIVATE)

    fun getConversations(): List<AiConversation> {
        val raw = prefs.getString(KEY_CONVERSATIONS, null)
        if (raw.isNullOrBlank()) migrateLegacyEntries()
        val current = prefs.getString(KEY_CONVERSATIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(current)
            List(array.length()) { index -> parseConversation(array.getJSONObject(index)) }
                .sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    fun getConversation(id: Long): AiConversation? = getConversations().firstOrNull { it.id == id }

    fun createConversation(title: String): AiConversation {
        val now = System.currentTimeMillis()
        val conversation = AiConversation(now, title.ifBlank { "Study coach" }.trim(), now, now, emptyList())
        writeConversations(listOf(conversation) + getConversations())
        setActiveConversationId(conversation.id)
        return conversation
    }

    fun appendMessage(conversationId: Long, role: String, text: String): AiConversation? {
        if (text.isBlank()) return getConversation(conversationId)
        val conversations = getConversations().toMutableList()
        val index = conversations.indexOfFirst { it.id == conversationId }
        if (index < 0) return null
        val now = System.currentTimeMillis()
        val conversation = conversations[index]
        val updated = conversation.copy(updatedAt = now, messages = conversation.messages + AiChatMessage(now, role, text.trim(), now))
        conversations[index] = updated
        writeConversations(conversations)
        return updated
    }

    fun setActiveConversationId(id: Long?) {
        prefs.edit().apply { if (id == null) remove(KEY_ACTIVE) else putLong(KEY_ACTIVE, id) }.apply()
    }

    fun getActiveConversationId(): Long? = if (prefs.contains(KEY_ACTIVE)) prefs.getLong(KEY_ACTIVE, 0L).takeIf { it != 0L } else null

    private fun migrateLegacyEntries() {
        val raw = prefs.getString(KEY_LEGACY_ENTRIES, null) ?: return
        runCatching {
            val array = JSONArray(raw)
            val migrated = List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                val id = obj.getLong("id")
                val created = obj.getLong("createdAt")
                AiConversation(
                    id = id,
                    title = obj.optString("title", "Study coach"),
                    createdAt = created,
                    updatedAt = created,
                    messages = listOf(
                        AiChatMessage(id * 2, "USER", obj.optString("prompt"), created),
                        AiChatMessage(id * 2 + 1, "COACH", obj.optString("response"), created)
                    ).filter { it.text.isNotBlank() }
                )
            }
            writeConversations(migrated)
            migrated.maxByOrNull { it.updatedAt }?.let { setActiveConversationId(it.id) }
        }
    }

    private fun writeConversations(conversations: List<AiConversation>) {
        val array = JSONArray()
        conversations.take(100).forEach { conversation ->
            array.put(JSONObject().apply {
                put("id", conversation.id)
                put("title", conversation.title)
                put("createdAt", conversation.createdAt)
                put("updatedAt", conversation.updatedAt)
                put("messages", JSONArray().apply {
                    conversation.messages.forEach { message ->
                        put(JSONObject().apply {
                            put("id", message.id)
                            put("role", message.role)
                            put("text", message.text)
                            put("createdAt", message.createdAt)
                        })
                    }
                })
            })
        }
        prefs.edit().putString(KEY_CONVERSATIONS, array.toString()).apply()
    }

    private fun parseConversation(obj: JSONObject): AiConversation {
        val messagesJson = obj.optJSONArray("messages") ?: JSONArray()
        val messages = List(messagesJson.length()) { index ->
            val item = messagesJson.getJSONObject(index)
            AiChatMessage(item.getLong("id"), item.optString("role", "COACH"), item.optString("text"), item.optLong("createdAt", obj.optLong("updatedAt")))
        }
        return AiConversation(obj.getLong("id"), obj.optString("title", "Study coach"), obj.optLong("createdAt", obj.optLong("updatedAt")), obj.optLong("updatedAt", obj.optLong("createdAt")), messages)
    }

    private companion object {
        const val KEY_CONVERSATIONS = "conversations"
        const val KEY_LEGACY_ENTRIES = "entries"
        const val KEY_ACTIVE = "active_conversation_id"
    }
}
