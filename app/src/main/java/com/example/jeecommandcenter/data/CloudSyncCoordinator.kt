package com.example.jeecommandcenter.data

import android.content.Context

/**
 * Offline-first synchronization coordinator.
 * Cloud catalog is read-only; authenticated student progress/attempts are merged both ways.
 */
class CloudSyncCoordinator(
    private val context: Context,
    private val cloud: CloudJeeRepository = CloudJeeRepository()
) {
    private val syncPrefs = context.getSharedPreferences("sigma_cloud_sync", Context.MODE_PRIVATE)

    suspend fun sync(): SyncResult {
        val user = cloud.ensureSession()

        // Pull first so a fresh install can recover the account's existing cloud state.
        // The restore is monotonic: progress/confidence use the stronger value, completed
        // topics stay completed, and attempts are unioned by their cloud UUID.
        val restore = CloudSyncRestore(context, cloud).restore(user.id)

        // Move any local state into this account after cloud state has been merged.
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

        // Sync bookkeeping is scoped to the signed-in account and includes attempts
        // restored from cloud, preventing a restored attempt from being uploaded again.
        val attemptKey = "question_attempt_ids_${user.id}"
        val lastSyncKey = "last_sync_at_${user.id}"
        val syncedIds = syncPrefs.getStringSet(attemptKey, emptySet()).orEmpty().toMutableSet()
        syncedIds += restore.restoredAttemptLocalIds

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
            attemptsUploaded = attemptCount,
            chaptersRestored = restore.chaptersRestored,
            topicsRestored = restore.topicsRestored,
            attemptsRestored = restore.attemptsRestored
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
    val attemptsUploaded: Int,
    val chaptersRestored: Int = 0,
    val topicsRestored: Int = 0,
    val attemptsRestored: Int = 0
)
