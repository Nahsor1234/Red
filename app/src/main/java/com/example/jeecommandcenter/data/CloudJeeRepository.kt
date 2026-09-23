package com.example.jeecommandcenter.data

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.OtpType
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
        client.auth.currentSessionOrNull()?.user
            ?: error("Sign in to a Sigma JE account before using cloud sync.")
    }

    suspend fun currentUser() = run {
        client.auth.loadFromStorage(autoRefresh = true)
        client.auth.currentSessionOrNull()?.user
    }

    suspend fun hasAuthenticatedSession(): Boolean = currentUser() != null

    suspend fun subjects(): List<CloudSubject> =
        db["subjects"].select().decodeList<CloudSubject>().sortedBy { it.sortOrder }

    suspend fun chapters(subjectId: String): List<CloudChapter> =
        db["chapters"].select {
            filter { CloudChapter::subjectId eq subjectId.lowercase() }
        }.decodeList<CloudChapter>().sortedBy { it.number }

    suspend fun topics(chapterId: String): List<CloudTopic> =
        db["topics"].select {
            filter { CloudTopic::chapterId eq JeeCatalog.normalizeChapterId(chapterId) }
        }.decodeList<CloudTopic>().sortedBy { it.sortOrder }

    suspend fun questions(chapterId: String): List<CloudQuestion> {
        val canonicalId = JeeCatalog.normalizeChapterId(chapterId)
        val remote = runCatching {
            db["questions"].select {
                filter { CloudQuestion::chapterId eq canonicalId }
            }.decodeList<CloudQuestion>()
        }.getOrDefault(emptyList())

        if (remote.isNotEmpty()) return remote

        return (StarterQuestionBank.fallback + CuratedQuestionBank.questions)
            .filter { JeeCatalog.normalizeChapterId(it.chapterId) == canonicalId }
            .map { question ->
                CloudQuestion(
                    id = question.id,
                    subjectId = question.subject.lowercase(),
                    chapterId = canonicalId,
                    topicId = null,
                    chapterName = question.chapter,
                    topic = question.topic,
                    prompt = question.prompt,
                    options = question.options,
                    correctIndex = question.correctIndex,
                    explanation = question.explanation,
                    difficulty = question.difficulty,
                    source = question.source,
                    year = question.year,
                    marks = question.marks,
                    negativeMarks = question.negativeMarks
                )
            }
    }

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
            buildJsonObject {
                put("user_id", userId)
                put("topic_id", topicId)
                put("completed", completed)
            },
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
            buildJsonObject {
                put("user_id", userId)
                put("chapter_id", JeeCatalog.normalizeChapterId(chapterId))
                put("progress", progress.coerceIn(0f, 1f).toDouble())
                put("confidence", confidence.coerceIn(0, 5))
            },
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
            put("chapter_id", JeeCatalog.normalizeChapterId(question.chapterId))
            put("topic_id", question.topicId)
            put("selected_index", selectedIndex)
            put("correct", selectedIndex == question.correctIndex)
            put("response_time_sec", responseTimeSec.coerceAtLeast(0))
        })
    }

    suspend fun signUp(email: String, password: String) {
        client.auth.signUpWith(Email, redirectUrl = SupabaseClientProvider.AUTH_REDIRECT_URL) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun resendConfirmation(email: String) {
        client.auth.resendEmail(
            type = OtpType.Email.SIGNUP,
            email = email.trim()
        )
    }

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun migrateLocalProgress(context: Context) {
        val user = ensureSession()
        val prefs = context.getSharedPreferences("sigma_cloud_migration", Context.MODE_PRIVATE)
        val migrationKey = "topic_progress_v2_${user.id}"
        if (prefs.getBoolean(migrationKey, false)) return

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

        topicRows.forEach { row ->
            db["topic_progress"].upsert(
                buildJsonObject {
                    put("user_id", row.userId)
                    put("topic_id", row.topicId)
                    put("completed", row.completed)
                },
                onConflict = "user_id,topic_id"
            )
        }
        chapterRows.forEach { row ->
            db["chapter_progress"].upsert(
                buildJsonObject {
                    put("user_id", row.userId)
                    put("chapter_id", row.chapterId)
                    put("progress", row.progress.coerceIn(0f, 1f).toDouble())
                    put("confidence", row.confidence.coerceIn(0, 5))
                },
                onConflict = "user_id,chapter_id"
            )
        }

        prefs.edit().putBoolean(migrationKey, true).apply()
    }
}
