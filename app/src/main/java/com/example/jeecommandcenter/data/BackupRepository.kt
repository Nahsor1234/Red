package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(context: Context) {
    private val context = context.applicationContext
    private val sources = listOf("jee_command_center", "jee_learning_engine", "jee_ai_settings")
    private val recoveryPrefs = "jee_recovery"
    private val recoveryKey = "last_known_good_backup"

    fun exportJson(): String = buildBackupJson()

    fun importJson(json: String): Result<Unit> = runCatching {
        val root = validateBackup(json)
        val all = root.getJSONObject("preferences")
        val decoded = sources.mapNotNull { name ->
            all.optJSONObject(name)?.let { name to decode(it) }
        }

        // Keep a local recovery point before replacing any user data.
        context.getSharedPreferences(recoveryPrefs, Context.MODE_PRIVATE)
            .edit().putString(recoveryKey, buildBackupJson()).apply()

        decoded.forEach { (name, values) ->
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            values.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
            check(editor.commit()) { "Could not restore $name" }
        }
    }

    fun recoverLastKnownGood(): Result<Unit> = runCatching {
        val json = context.getSharedPreferences(recoveryPrefs, Context.MODE_PRIVATE).getString(recoveryKey, null)
            ?: error("No recovery point is available.")
        importJsonWithoutCreatingRecovery(json)
    }

    fun hasRecoveryPoint(): Boolean = context.getSharedPreferences(recoveryPrefs, Context.MODE_PRIVATE).contains(recoveryKey)

    private fun importJsonWithoutCreatingRecovery(json: String) {
        val root = validateBackup(json)
        val all = root.getJSONObject("preferences")
        sources.forEach { name ->
            val data = all.optJSONObject(name) ?: return@forEach
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            decode(data).forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
            check(editor.commit()) { "Could not recover $name" }
        }
    }

    private fun validateBackup(json: String): JSONObject {
        val root = JSONObject(json)
        require(root.optString("format") == "JeE backup") { "Invalid JeE backup file." }
        require(root.optInt("version", 0) in 1..JeeRepository.CURRENT_SCHEMA_VERSION) { "Backup version is not supported." }
        require(root.optJSONObject("preferences") != null) { "Backup does not contain preference data." }
        require(root.optBoolean("includesApiKey", true).not()) { "Unsafe backup: API keys must not be imported." }
        return root
    }

    private fun buildBackupJson(): String {
        val root = JSONObject().apply {
            put("format", "JeE backup")
            put("version", JeeRepository.CURRENT_SCHEMA_VERSION)
            put("createdAt", System.currentTimeMillis())
            put("includesApiKey", false)
            put("preferences", JSONObject())
        }
        val all = root.getJSONObject("preferences")
        sources.forEach { name -> all.put(name, encode(context.getSharedPreferences(name, Context.MODE_PRIVATE).all)) }
        return root.toString(2)
    }

    private fun encode(values: Map<String, *>): JSONObject {
        val out = JSONObject()
        values.forEach { (key, value) ->
            when (value) {
                is String -> out.put(key, JSONObject().put("type", "string").put("value", value))
                is Int -> out.put(key, JSONObject().put("type", "int").put("value", value))
                is Long -> out.put(key, JSONObject().put("type", "long").put("value", value))
                is Float -> out.put(key, JSONObject().put("type", "float").put("value", value.toDouble()))
                is Boolean -> out.put(key, JSONObject().put("type", "boolean").put("value", value))
                is Set<*> -> out.put(key, JSONObject().put("type", "stringSet").put("value", JSONArray(value.filterIsInstance<String>())))
            }
        }
        return out
    }

    private fun decode(data: JSONObject): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        data.keys().forEach { key ->
            val item = data.optJSONObject(key) ?: error("Malformed backup entry: $key")
            when (item.optString("type")) {
                "string" -> result[key] = item.optString("value")
                "int" -> result[key] = item.optInt("value")
                "long" -> result[key] = item.optLong("value")
                "float" -> result[key] = item.optDouble("value").toFloat()
                "boolean" -> result[key] = item.optBoolean("value")
                "stringSet" -> {
                    val array = item.optJSONArray("value") ?: JSONArray()
                    result[key] = buildSet { for (i in 0 until array.length()) add(array.getString(i)) }
                }
                else -> error("Unsupported backup value type for $key")
            }
        }
        return result
    }
}
