package com.example.jeecommandcenter.data

import android.content.Context
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject

data class CloudRestoreResult(
    val chaptersRestored: Int,
    val topicsRestored: Int,
    val attemptsRestored: Int,
    val restoredAttemptLocalIds: Set<String>
)

/** Restores authenticated cloud state into the existing offline-first stores. */
class CloudSyncRestore(
    private val context: Context,
    private val cloud: CloudJeeRepository = CloudJeeRepository()
) {
    private val db = SupabaseClientProvider.client.postgrest

    suspend fun restore(userId: String): CloudRestoreResult {
        val chapters = cloud.chapterProgressForUser(userId)
        val topics = cloud.topicProgressForUser(userId)

        var chaptersRestored = 0
        val chapterPrefs = context.getSharedPreferences("jee_command_center", Context.MODE_PRIVATE)
        val localRepo = JeeRepository(context)
        chapters.forEach { remote ->
            val chapterId = JeeCatalog.normalizeChapterId(remote.chapterId)
            val local = localRepo.getChapterState(chapterId)
            val mergedProgress = maxOf(local.progress, remote.progress)
            val mergedConfidence = maxOf(local.confidence, remote.confidence)
            if (mergedProgress != local.progress || mergedConfidence != local.confidence) {
                localRepo.setChapterState(chapterId, mergedProgress, mergedConfidence, local.lastStudiedAt)
                chaptersRestored++
            }
            JeeCatalog.find(chapterId)?.let { chapter ->
                chapterPrefs.edit()
                    .putFloat("chapter_".plus(chapter.subject).plus("_").plus(chapter.number), mergedProgress)
                    .apply()
            }
        }

        var topicsRestored = 0
        val topicPrefs = context.getSharedPreferences("jee_topics", Context.MODE_PRIVATE)
        topics.filter { it.completed }.groupBy { topic ->
            JeeCatalog.chapters.firstOrNull { chapter -> topic.topicId.startsWith(chapter.id + "_") }?.id
        }.forEach { (chapterId, remoteTopics) ->
            if (chapterId == null) return@forEach
            val key = "chapter_$chapterId"
            val existing = readTopics(topicPrefs, key).toMutableMap()
            remoteTopics.forEach { remote ->
                val current = existing[remote.topicId]
                if (current == null || !current.second) {
                    existing[remote.topicId] = (current?.first ?: topicTitle(remote.topicId)) to true
                    topicsRestored++
                }
            }
            writeTopics(topicPrefs, key, existing)
        }

        // Decode the row as JSON instead of assuming a Kotlin scalar type. This
        // handles the current numeric column and also remains tolerant if the
        // backend represents the identifier as a JSON string.
        val attempts = db["question_attempts"].select {
            filter { eq("user_id", userId) }
        }.decodeList<JsonObject>()

        val learningPrefs = context.getSharedPreferences("jee_learning_engine", Context.MODE_PRIVATE)
        val localAttempts = readLocalAttempts(learningPrefs)
        val restoredLocalIds = mutableSetOf<String>()
        var attemptsRestored = 0

        attempts.forEach { remote ->
            val cloudId = remote["id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val questionId = remote["question_id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val chapterId = remote["chapter_id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val selectedIndex = remote["selected_index"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@forEach
            val correct = parseBoolean(remote["correct"]?.jsonPrimitive?.contentOrNull)
            val responseTimeSec = remote["response_time_sec"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceAtLeast(0) ?: 0

            val localId = stableLocalId(cloudId)
            if (!localAttempts.containsKey(localId)) {
                localAttempts[localId] = JSONObject().apply {
                    put("id", localId)
                    put("testId", "cloud_$cloudId")
                    put("questionId", questionId)
                    put("selectedIndex", selectedIndex)
                    put("correct", correct)
                    put("responseTimeSec", responseTimeSec)
                    put("subject", JeeCatalog.find(chapterId)?.subject ?: "General")
                    put("chapterId", JeeCatalog.normalizeChapterId(chapterId))
                    put("mistakeType", "")
                    put("createdAt", System.currentTimeMillis())
                }
                attemptsRestored++
            }
            restoredLocalIds += localId.toString()
        }

        if (attempts.isNotEmpty()) writeLocalAttempts(learningPrefs, localAttempts.values.toList())

        return CloudRestoreResult(
            chaptersRestored = chaptersRestored,
            topicsRestored = topicsRestored,
            attemptsRestored = attemptsRestored,
            restoredAttemptLocalIds = restoredLocalIds
        )
    }

    private fun parseBoolean(value: String?): Boolean =
        value.equals("true", ignoreCase = true) || value == "1"

    private fun topicTitle(topicId: String): String =
        topicId.substringAfterLast('_').replace('_', ' ')

    private fun readTopics(
        prefs: android.content.SharedPreferences,
        key: String
    ): MutableMap<String, Pair<String, Boolean>> {
        val raw = prefs.getString(key, null) ?: return mutableMapOf()
        return runCatching {
            val array = JSONArray(raw)
            MutableList(array.length()) { i ->
                val obj = array.getJSONObject(i)
                obj.getString("id") to (obj.getString("title") to obj.optBoolean("completed"))
            }.toMap().toMutableMap()
        }.getOrDefault(mutableMapOf())
    }

    private fun writeTopics(
        prefs: android.content.SharedPreferences,
        key: String,
        topics: Map<String, Pair<String, Boolean>>
    ) {
        val array = JSONArray()
        topics.forEach { (id, titleCompleted) ->
            array.put(
                JSONObject()
                    .put("id", id)
                    .put("title", titleCompleted.first)
                    .put("completed", titleCompleted.second)
            )
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun stableLocalId(cloudId: String): Long =
        (cloudId.hashCode().toLong() shl 32) or
            (cloudId.reversed().hashCode().toLong() and 0xffffffffL)

    private fun readLocalAttempts(
        prefs: android.content.SharedPreferences
    ): MutableMap<Long, JSONObject> {
        val raw = prefs.getString("question_attempts", null) ?: return mutableMapOf()
        return runCatching {
            val array = JSONArray(raw)
            MutableList(array.length()) { i -> array.getJSONObject(i) }
                .associateBy { it.getLong("id") }
                .toMutableMap()
        }.getOrDefault(mutableMapOf())
    }

    private fun writeLocalAttempts(
        prefs: android.content.SharedPreferences,
        attempts: List<JSONObject>
    ) {
        val array = JSONArray()
        attempts.sortedBy { it.optLong("createdAt", it.optLong("id")) }
            .takeLast(1000)
            .forEach { array.put(it) }
        prefs.edit().putString("question_attempts", array.toString()).apply()
    }
}
