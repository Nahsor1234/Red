package com.example.jeecommandcenter.data

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.contentOrNull
import io.github.jan.supabase.postgrest.postgrest

@Serializable
data class CloudPreferenceRecord(
    @SerialName("user_id") val userId: String,
    val namespace: String,
    val key: String,
    val payload: JsonObject = buildJsonObject { },
    val deleted: Boolean = false,
    @SerialName("updated_at") val updatedAt: Long
)

data class PreferenceSyncResult(
    val uploaded: Int = 0,
    val restored: Int = 0
)

/**
 * Syncs persistent SharedPreferences state that is not already represented by the
 * typed chapter/topic/question cloud tables. This deliberately works at key level,
 * so existing repositories remain the local source of truth and future preference-
 * backed user data is covered without creating duplicate domain models.
 */
class CloudPreferenceSync(
    private val context: Context,
    private val cloud: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client
) {
    private val db = cloud.postgrest
    private val state = context.getSharedPreferences("sigma_cloud_preference_sync", Context.MODE_PRIVATE)

    private val namespaces = listOf(
        "jee_command_center",
        "jee_learning_engine",
        "jee_ai_settings",
        "ai_chat_history"
    )

    suspend fun sync(userId: String): PreferenceSyncResult {
        val remote = db["user_cloud_preference_records"].select {
            filter { CloudPreferenceRecord::userId eq userId }
        }.decodeList<CloudPreferenceRecord>()
        val remoteByKey = remote.associateBy { key(it.namespace, it.key) }
        val local = currentLocalEntries()
        val allKeys = (local.keys + remoteByKey.keys).toSortedSet()

        val uploads = mutableListOf<CloudPreferenceRecord>()
        val restored = mutableListOf<CloudPreferenceRecord>()
        var uploadedCount = 0
        var restoredCount = 0

        for (compoundKey in allKeys) {
            val parts = compoundKey.split(KEY_SEPARATOR, limit = 2)
            val namespace = parts[0]
            val prefKey = parts[1]
            val localEntry = local[compoundKey]
            val remoteEntry = remoteByKey[compoundKey]
            val lastHash = state.getString(hashKey(namespace, prefKey), null)
            val localHash = localEntry?.let { hash(it) }
            val remoteHash = remoteEntry?.takeUnless { it.deleted }?.let { hash(it.payload) }

            when {
                localEntry == null && remoteEntry == null -> Unit

                localEntry == null -> {
                    if (remoteEntry!!.deleted) {
                        state.edit().putString(hashKey(namespace, prefKey), tombstoneHash(remoteEntry)).apply()
                    } else if (lastHash == null || lastHash == remoteHash) {
                        restore(namespace, prefKey, remoteEntry.payload)
                        state.edit().putString(hashKey(namespace, prefKey), remoteHash).apply()
                        restored += remoteEntry
                        restoredCount++
                    } else {
                        uploads += tombstone(userId, namespace, prefKey)
                        uploadedCount++
                    }
                }

                remoteEntry == null -> {
                    val record = CloudPreferenceRecord(
                        userId = userId,
                        namespace = namespace,
                        key = prefKey,
                        payload = localEntry,
                        deleted = false,
                        updatedAt = System.currentTimeMillis()
                    )
                    uploads += record
                    state.edit().putString(hashKey(namespace, prefKey), localHash).apply()
                    uploadedCount++
                }

                remoteEntry.deleted -> {
                    when {
                        localHash == lastHash -> {
                            removeLocal(namespace, prefKey)
                            state.edit().putString(hashKey(namespace, prefKey), tombstoneHash(remoteEntry)).apply()
                            restored += remoteEntry
                            restoredCount++
                        }
                        localHash == tombstoneHash(remoteEntry) -> Unit
                        else -> {
                            uploads += CloudPreferenceRecord(
                                userId = userId,
                                namespace = namespace,
                                key = prefKey,
                                payload = localEntry,
                                deleted = false,
                                updatedAt = System.currentTimeMillis()
                            )
                            state.edit().putString(hashKey(namespace, prefKey), localHash).apply()
                            uploadedCount++
                        }
                    }
                }

                localHash == remoteHash -> {
                    state.edit().putString(hashKey(namespace, prefKey), localHash).apply()
                }

                localHash == lastHash -> {
                    restore(namespace, prefKey, remoteEntry.payload)
                    state.edit().putString(hashKey(namespace, prefKey), remoteHash).apply()
                    restored += remoteEntry
                    restoredCount++
                }

                remoteHash == lastHash || lastHash == null -> {
                    uploads += CloudPreferenceRecord(
                        userId = userId,
                        namespace = namespace,
                        key = prefKey,
                        payload = localEntry,
                        deleted = false,
                        updatedAt = System.currentTimeMillis()
                    )
                    state.edit().putString(hashKey(namespace, prefKey), localHash).apply()
                    uploadedCount++
                }

                else -> {
                    // Both sides changed since the last common state. Keep the current
                    // device's user-visible state and publish it; never silently discard it.
                    uploads += CloudPreferenceRecord(
                        userId = userId,
                        namespace = namespace,
                        key = prefKey,
                        payload = localEntry,
                        deleted = false,
                        updatedAt = System.currentTimeMillis()
                    )
                    state.edit().putString(hashKey(namespace, prefKey), localHash).apply()
                    uploadedCount++
                }
            }
        }

        if (uploads.isNotEmpty()) {
            db["user_cloud_preference_records"].upsert(
                uploads,
                onConflict = "user_id,namespace,key"
            )
        }

        return PreferenceSyncResult(uploaded = uploadedCount, restored = restoredCount)
    }

    private fun currentLocalEntries(): Map<String, JsonObject> {
        val result = mutableMapOf<String, JsonObject>()
        namespaces.forEach { namespace ->
            context.getSharedPreferences(namespace, Context.MODE_PRIVATE).all.forEach { (prefKey, value) ->
                if (shouldSync(namespace, prefKey)) {
                    encode(value)?.let { result[key(namespace, prefKey)] = it }
                }
            }
        }
        return result
    }

    private fun shouldSync(namespace: String, prefKey: String): Boolean = when (namespace) {
        "jee_command_center" -> !prefKey.startsWith("chapter_") &&
            !prefKey.startsWith("timer_") &&
            prefKey != "data_schema_version" &&
            prefKey != "daily_plan"
        "jee_learning_engine" -> prefKey != "question_attempts"
        else -> true
    }

    private fun restore(namespace: String, prefKey: String, payload: JsonObject) {
        val value = decode(payload) ?: return
        val editor = context.getSharedPreferences(namespace, Context.MODE_PRIVATE).edit()
        when (value) {
            is String -> editor.putString(prefKey, value)
            is Int -> editor.putInt(prefKey, value)
            is Long -> editor.putLong(prefKey, value)
            is Float -> editor.putFloat(prefKey, value)
            is Boolean -> editor.putBoolean(prefKey, value)
            is Set<*> -> editor.putStringSet(prefKey, value.filterIsInstance<String>().toSet())
        }
        check(editor.commit()) { "Could not restore $namespace/$prefKey" }
    }

    private fun removeLocal(namespace: String, prefKey: String) {
        check(
            context.getSharedPreferences(namespace, Context.MODE_PRIVATE)
                .edit()
                .remove(prefKey)
                .commit()
        ) { "Could not remove $namespace/$prefKey" }
    }

    private fun encode(value: Any?): JsonObject? = when (value) {
        is String -> buildJsonObject { put("type", "string"); put("value", value) }
        is Int -> buildJsonObject { put("type", "int"); put("value", value) }
        is Long -> buildJsonObject { put("type", "long"); put("value", value) }
        is Float -> buildJsonObject { put("type", "float"); put("value", value.toDouble()) }
        is Boolean -> buildJsonObject { put("type", "boolean"); put("value", value) }
        is Set<*> -> buildJsonObject {
            put("type", "stringSet")
            put("value", buildJsonArray { value.filterIsInstance<String>().forEach(::add) })
        }
        else -> null
    }

    private fun decode(payload: JsonObject): Any? {
        val type = payload["type"]?.jsonPrimitive?.contentOrNull ?: return null
        val value = payload["value"] ?: return null
        return when (type) {
            "string" -> value.jsonPrimitive.content
            "int" -> value.jsonPrimitive.content.toInt()
            "long" -> value.jsonPrimitive.content.toLong()
            "float" -> value.jsonPrimitive.content.toFloat()
            "boolean" -> value.jsonPrimitive.content.toBoolean()
            "stringSet" -> value.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }.toSet()
            else -> null
        }
    }

    private fun hash(payload: JsonObject): String = sha256(payload.toString())

    private fun tombstone(record: CloudPreferenceRecord): CloudPreferenceRecord =
        record.copy(payload = buildJsonObject { }, deleted = true, updatedAt = System.currentTimeMillis())

    private fun tombstoneHash(record: CloudPreferenceRecord): String =
        "deleted:" + record.updatedAt

    private fun hashKey(namespace: String, prefKey: String): String = "hash_$namespace$KEY_SEPARATOR$prefKey"

    private fun key(namespace: String, prefKey: String): String = namespace + KEY_SEPARATOR + prefKey

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val KEY_SEPARATOR = "::"
    }
}
