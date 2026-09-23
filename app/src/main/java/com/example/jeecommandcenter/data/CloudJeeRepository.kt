package com.example.jeecommandcenter.data

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class CloudSubject(
    val id: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int
)

@Serializable
data class CloudChapter(
    val id: String,
    @SerialName("subject_id") val subjectId: String,
    val number: Int,
    val name: String,
    val difficulty: Int,
    @SerialName("estimated_minutes") val estimatedMinutes: Int
)

@Serializable
data class CloudTopic(
    val id: String,
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("sort_order") val sortOrder: Int,
    val title: String
)

@Serializable
data class CloudQuestion(
    val id: String,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("topic_id") val topicId: String? = null,
    @SerialName("chapter_name") val chapterName: String,
    val topic: String,
    val prompt: String,
    val options: List<String>,
    @SerialName("correct_index") val correctIndex: Int,
    val explanation: String,
    val difficulty: Int,
    val source: String,
    val year: Int? = null,
    val marks: Int = 4,
    @SerialName("negative_marks") val negativeMarks: Float = 1f
)

@Serializable
data class CloudChapterProgress(
    @SerialName("user_id") val userId: String,
    @SerialName("chapter_id") val chapterId: String,
    val progress: Float = 0f,
    val confidence: Int = 0,
    @SerialName("last_studied_at") val lastStudiedAt: String? = null
)

@Serializable
data class CloudTopicProgress(
    @SerialName("user_id") val userId: String,
    @SerialName("topic_id") val topicId: String,
    val completed: Boolean = false
)

class CloudJeeRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client
) {
    private val db = client.postgrest

    suspend fun ensureSession() = run {
        client.auth.loadFromStorage(autoRefresh = true)
        client.auth.currentSessionOrNull()?.user?.let { return@run it }
        client.auth.signInAnonymously()
        client.auth.currentSessionOrNull()?.user
            ?: error("Supabase session could not be created.")
    }

    suspend fun currentUser() = client.auth.currentSessionOrNull()?.user

    suspend fun subjects(): List<CloudSubject> =
        db["subjects"].select().decodeList<CloudSubject>().sortedBy { it.sortOrder }

    suspend fun chapters(subjectId: String): List<CloudChapter> =
        db["chapters"].select {
            filter { CloudChapter::subjectId eq subjectId }
        }.decodeList<CloudChapter>().sortedBy { it.number }

    suspend fun topics(chapterId: String): List<CloudTopic> =
        db["topics"].select {
            filter { CloudTopic::chapterId eq chapterId }
        }.decodeList<CloudTopic>().sortedBy { it.sortOrder }

    suspend fun questions(chapterId: String): List<CloudQuestion> =
        db["questions"].select {
            filter { CloudQuestion::chapterId eq chapterId }
        }.decodeList()

    suspend fun chapterProgressForUser(userId: String): List<CloudChapterProgress> =
        db["chapter_progress"].select {
            filter { CloudChapterProgress::userId eq userId }
        }.decodeList()

    suspend fun topicProgressForUser(userId: String): List<CloudTopicProgress> =
        db["topic_progress"].select {
            filter { CloudTopicProgress::userId eq userId }
        }.decodeList()

    suspend fun setTopicCompleted(userId: String, topicId: String, completed: Boolean) {
        db["topic_progress"].upsert(
            CloudTopicProgress(userId, topicId, completed),
            onConflict = "user_id,topic_id"
        )
    }

    suspend fun setChapterProgress(
        userId: String,
        chapterId: String,
        progress: Float,
        confidence: Int = 0
    ) {
        db["chapter_progress"].upsert(
            CloudChapterProgress(
                userId = userId,
                chapterId = chapterId,
                progress = progress.coerceIn(0f, 1f),
                confidence = confidence.coerceIn(0, 5)
            ),
            onConflict = "user_id,chapter_id"
        )
    }

    suspend fun recordQuestionAttempt(
        userId: String,
        question: CloudQuestion,
        selectedIndex: Int,
        responseTimeSec: Int
    ) {
        db["question_attempts"].insert(buildJsonObject {
            put("user_id", userId)
            put("question_id", question.id)
            put("chapter_id", question.chapterId)
            put("topic_id", question.topicId)
            put("selected_index", selectedIndex)
            put("correct", selectedIndex == question.correctIndex)
            put("response_time_sec", responseTimeSec.coerceAtLeast(0))
        })
    }

    suspend fun signUp(email: String, password: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun migrateLocalProgress(context: Context) {
        val prefs = context.getSharedPreferences("sigma_cloud_migration", Context.MODE_PRIVATE)
        if (prefs.getBoolean("topic_progress_v1", false)) return

        val user = ensureSession()
        val localRepo = JeeRepository(context)
        val topicsRepo = TopicRepository(context)

        val topicRows = mutableListOf<CloudTopicProgress>()
        val chapterRows = mutableListOf<CloudChapterProgress>()

        JeeCatalog.chapters.forEach { chapter ->
            topicsRepo.topicsFor(chapter)
                .filter { it.completed }
                .forEach { topicRows += CloudTopicProgress(user.id, it.id, true) }

            val state = localRepo.getChapterState(chapter.id)
            if (state.progress > 0f || state.confidence > 0) {
                chapterRows += CloudChapterProgress(
                    userId = user.id,
                    chapterId = chapter.id,
                    progress = state.progress,
                    confidence = state.confidence
                )
            }
        }

        if (topicRows.isNotEmpty()) {
            db["topic_progress"].upsert(
                topicRows,
                onConflict = "user_id,topic_id"
            )
        }

        if (chapterRows.isNotEmpty()) {
            db["chapter_progress"].upsert(
                chapterRows,
                onConflict = "user_id,chapter_id"
            )
        }

        prefs.edit().putBoolean("topic_progress_v1", true).apply()
    }
}
