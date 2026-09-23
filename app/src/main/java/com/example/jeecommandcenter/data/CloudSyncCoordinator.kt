package com.example.jeecommandcenter.data

import android.content.Context

/**
 * Offline-first synchronization coordinator.
 * Cloud catalog is read-only; student progress/attempts are uploaded incrementally.
 */
class CloudSyncCoordinator(
    private val context: Context,
    private val cloud: CloudJeeRepository = CloudJeeRepository()
) {
    private val syncPrefs = context.getSharedPreferences("sigma_cloud_sync", Context.MODE_PRIVATE)

    suspend fun sync(): SyncResult {
        val user = cloud.ensureSession()
        // Move local syllabus progress into this specific account before incremental sync.
        cloud.migrateLocalProgress(context)

        val local = JeeRepository(context)
        val learning = LearningRepository(context)
        val topics = TopicRepository(context)

        var chapterCount = 0
        var topicCount = 0
        var attemptCount = 0

        JeeCatalog.chapters.forEach { chapter ->
            val state = local.getChapterState(chapter.id)
            if (state.progress > 0f || state.confidence > 0) {
                cloud.setChapterProgress(user.id, chapter.id, state.progress, state.confidence)
                chapterCount++
            }

            topics.topicsFor(chapter).forEach { topic ->
                if (topic.completed) {
                    cloud.setTopicCompleted(user.id, topic.id, true)
                    topicCount++
                }
            }
        }

        // Sync bookkeeping is scoped to the signed-in account. This prevents one
        // account's local sync history from suppressing uploads for another account.
        val attemptKey = "question_attempt_ids_${user.id}"
        val lastSyncKey = "last_sync_at_${user.id}"
        val syncedIds = syncPrefs.getStringSet(attemptKey, emptySet()).orEmpty().toMutableSet()

        learning.getQuestionAttempts().forEach { attempt ->
            if (attempt.id.toString() in syncedIds) return@forEach

            val question = cloud.questions(attempt.chapterId)
                .firstOrNull { it.id == attempt.questionId }
                ?: return@forEach

            cloud.recordQuestionAttempt(
                userId = user.id,
                question = question,
                selectedIndex = attempt.selectedIndex,
                responseTimeSec = attempt.responseTimeSec
            )
            syncedIds += attempt.id.toString()
            attemptCount++
        }

        syncPrefs.edit()
            .putStringSet(attemptKey, syncedIds)
            .putLong(lastSyncKey, System.currentTimeMillis())
            .apply()

        return SyncResult(
            chaptersUploaded = chapterCount,
            topicsUploaded = topicCount,
            attemptsUploaded = attemptCount
        )
    }

    suspend fun hasAccount(): Boolean = cloud.hasAuthenticatedSession()

    fun lastSyncAt(userId: String? = null): Long {
        if (userId == null) return 0L
        return syncPrefs.getLong("last_sync_at_$userId", 0L)
    }
}

data class SyncResult(
    val chaptersUploaded: Int,
    val topicsUploaded: Int,
    val attemptsUploaded: Int
)
