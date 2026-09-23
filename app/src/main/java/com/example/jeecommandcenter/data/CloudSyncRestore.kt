package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

/**
 * Restores authenticated user data from Supabase into the existing offline-first stores.
 * This deliberately merges rather than replacing local state:
 * - chapter progress/confidence never move backwards
 * - completed topics remain completed
 * - question attempts are unioned by stable cloud UUID-derived local id
 */
class CloudSyncRestore(
    private val context: Context,
    private val cloud: CloudJeeRepository = CloudJeeRepository()
) {
    private val db = SupabaseClientProvider.client.postgrest

    suspend fun restore(userId: String): CloudRestoreResult {
        val chapters = cloud.chapterProgressForUser(userId)
        val topics = cloud.topicProgressForUser(userId)
        val topicChapterMap = buildTopicChapterMap()

        val chapterPrefs = context.getSharedPreferences("jee_command_center", Context.MODE_PRIVATE)
        var chaptersRestored = 0
        chapters.forEach { remote ->
            val chapterId = JeeCatalog.normalizeChapterId(remote.chapterId)
            val localRepo = JeeRepository(context)
            val local = localRepo.getChapterState(chapterId)
            val mergedProgress = maxOf(local.progress, remote.progress)
            val mergedConfidence = maxOf(local.confidence, remote.confidence)
            if (mergedProgress != local.progress || mergedConfidence != local.confidence) {
                localRepo.setChapterState(
                    chapterId = chapterId,
                    progress = mergedProgress,
                    confidence = mergedConfidence,
                    studiedAt = local.lastStudiedAt
                )
                chaptersRestored++
            }
            // Keep the legacy chapter-progress key populated for older readers/screens.
            JeeCatalog.find(chapterId)?.let { chapter ->
                chapterPrefs.edit()
                    .putFloat("chapter_${chapter.subject}_${chapter.number}", mergedProgress)
                    .apply()
            }
        }

        val topicPrefs = context.getSharedPreferences("jee_topics", Context.MODE_PRIVATE)
        val groupedTopics = topics.filter { it.completed }
            .mapNotNull { progress ->
                val chapterId = topicChapterMap[progress.topicId] ?: return@mapNotNull null
                chapterId to progress
            }
            .groupBy({ it.first }, { it.second })

        var topicsRestored = 0
        groupedTopics.forEach { (chapterId, remoteTopics) ->
            val chapter = JeeCatalog.find(chapterId) ?: return@forEach
            val key = "chapter_${chapter.id}"
            val existing = readTopics(topicPrefs, key).associateBy { it.first }.toMutableMap()
            remoteTopics.forEach { remote ->
                val topic = existing[remote.topicId]
                if (topic == null || !topic.second) {
                    val title = topicChapterMap[remote.topicId]?.let { mappedChapterId ->
                        defaultTopicTitle(mappedChapterId, remote.topicId)
                    } ?: remote.topicId.substringAfterLast('_').replace('_', ' ')
                    existing[remote.topicId] = title to true
                    topicsRestored++
                }
            }
            writeTopics(topicPrefs, key, existing)
        }

        val attempts = runCatching {
            db["question_attempts"].select {
                filter { CloudQuestionAttemptRestore::id.isNotNull() }
                filter { CloudQuestionAttemptRestore::userId eq userId }
            }.decodeList<CloudQuestionAttemptRestore>()
        }.getOrElse {
            // The property-based user filter above is unavailable on some older
            // supabase-kt versions; use the raw column filter supported by PostgREST.
            db["question_attempts"].select {
                filter { eq("user_id", userId) }
            }.decodeList<CloudQuestionAttemptRestore>()
        }

        val learningPrefs = context.getSharedPreferences("jee_learning_engine", Context.MODE_PRIVATE)
        val localAttempts = readLocalAttempts(learningPrefs).associateBy { it.id }.toMutableMap()
        val restoredLocalIds = mutableSetOf<String>()
        attempts.forEach { remote ->
            val localId = stableLocalId(remote.id)
            val existing = localAttempts[localId]
            if (existing == null) {
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
                attemptsRestoredCount.incrementAndGet()
            }
            restoredLocalIds += localId.toString()
        }

        if (attempts.isNotEmpty()) writeLocalAttempts(learningPrefs, localAttempts.values.toList())

        return CloudRestoreResult(
            chaptersRestored = chaptersRestored,
            topicsRestored = topicsRestored,
            attemptsRestored = attemptsRestoredCount.get(),
            restoredAttemptLocalIds = restoredLocalIds
        )
    }

    private val attemptsRestoredCount = java.util.concurrent.atomic.AtomicInteger(0)

    private suspend fun buildTopicChapterMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        JeeCatalog.chapters.forEach { chapter ->
            runCatching { cloud.topics(chapter.id) }
                .getOrDefault(emptyList())
                .forEach { topic -> map[topic.id] = JeeCatalog.normalizeChapterId(topic.chapterId) }
        }
        return map
    }

    private fun defaultTopicTitle(chapterId: String, topicId: String): String =
        topicId.substringAfterLast('_').replace('_', ' ')

    private fun readTopics(prefs: android.content.SharedPreferences, key: String): List<Pair<String, Boolean>> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                obj.getString("id") to obj.optBoolean("completed")
            }
        }.getOrDefault(emptyList())
    }

    private fun writeTopics(
        prefs: android.content.SharedPreferences,
        key: String,
        topics: Map<String, Pair<String, Boolean>>
    ) {
        val array = JSONArray()
        topics.forEach { (id, titleAndCompleted) ->
            array.put(
                JSONObject()
                    .put("id", id)
                    .put("title", titleAndCompleted.first)
                    .put("completed", titleAndCompleted.second)
            )
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun stableLocalId(cloudId: String): Long =
        cloudId.hashCode().toLong() shl 32 or (cloudId.reversed().hashCode().toLong() and 0xffffffffL)

    private fun readLocalAttempts(prefs: android.content.SharedPreferences): MutableMap<Long, JSONObject> {
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
