package com.example.jeecommandcenter.data

import android.content.Context
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject

@Serializable
private data class CloudQuestionAttemptRestore(
    val id: String,
    @SerialName("question_id") val questionId: String,
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("selected_index") val selectedIndex: Int,
    val correct: Boolean,
    @SerialName("response_time_sec") val responseTimeSec: Int = 0
)

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
                    .putFloat("chapter_${chapter.subject}_${chapter.number}", mergedProgress)
                    .apply()
            }
        }

        var topicsRestored = 0
        val topicPrefs = context.getSharedPreferences("jee_topics", Context.MODE_PRIVATE)
        topics.filter { it.completed }.groupBy { topic ->
            JeeCatalog.chapters.firstOrNull { chapter -> topic.topicId.startsWith("${chapter.id}_") }?.id
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

        val attempts = runCatching {
            db["question_attempts"].select {
                filter { eq("user_id", userId) }
            }.decodeList<CloudQuestionAttemptRestore>()
        }.getOrElse { emptyList() }

        val learningPrefs = context.getSharedPreferences("jee_learning_engine", Context.MODE_PRIVATE)
        val localAttempts = readLocalAttempts(learningPrefs)
        val restoredLocalIds = mutableSetOf<String>()
        var attemptsRestored = 0

        attempts.forEach { remote ->
            val localId = stableLocalId(remote.id)
            if (!localAttempts.containsKey(localId)) {
                localAttempts[localId] = JSONObject().apply {
                    put("id", localId)
                    put("testId", "cloud_${remote.id}")
                    put("questionId", remote.questionId)
                    put("selectedIndex", remote.selectedIndex)
                    put("correct", remote.correct)
                    put("responseTimeSec", remote.responseTimeSec.coerceAtLeast(0))
                    put("subject", JeeCatalog.find(remote.chapterId)?.subject ?: "General")
                    put("chapterId", JeeCatalog.normalizeChapterId(remote.chapterId))
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
