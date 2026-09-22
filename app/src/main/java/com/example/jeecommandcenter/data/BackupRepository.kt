package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(context: Context) {
    private val context = context.applicationContext
    private val sources = listOf(
        "jee_command_center",
        "jee_learning_engine",
        "jee_ai_settings"
    )

    fun exportJson(): String {
        val root = JSONObject().apply {
            put("format", "JeE backup")
            put("version", JeeRepository.CURRENT_SCHEMA_VERSION)
            put("createdAt", System.currentTimeMillis())
            put("includesApiKey", false)
            put("preferences", JSONObject())
        }
        val all = root.getJSONObject("preferences")
        sources.forEach { name ->
            all.put(name, encode(context.getSharedPreferences(name, Context.MODE_PRIVATE).all))
        }
        return root.toString(2)
    }

    fun importJson(json: String): Result<Unit> = runCatching {
        val root = JSONObject(json)
        require(root.optString("format") == "JeE backup") { "Invalid JeE backup file." }
        require(root.optInt("version", 0) in 1..JeeRepository.CURRENT_SCHEMA_VERSION) {
            "Backup version is not supported."
        }
        val all = root.optJSONObject("preferences")
            ?: error("Backup does not contain preference data.")

        sources.forEach { name ->
            val data = all.optJSONObject(name) ?: return@forEach
            restore(context.getSharedPreferences(name, Context.MODE_PRIVATE), data)
        }
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
                is Set<*> -> out.put(
                    key,
                    JSONObject().put("type", "stringSet")
                        .put("value", JSONArray(value.filterIsInstance<String>()))
                )
            }
        }
        return out
    }

    private fun restore(prefs: android.content.SharedPreferences, data: JSONObject) {
        val editor = prefs.edit().clear()
        data.keys().forEach { key ->
            val item = data.getJSONObject(key)
            when (item.optString("type")) {
                "string" -> editor.putString(key, item.optString("value"))
                "int" -> editor.putInt(key, item.optInt("value"))
                "long" -> editor.putLong(key, item.optLong("value"))
                "float" -> editor.putFloat(key, item.optDouble("value").toFloat())
                "boolean" -> editor.putBoolean(key, item.optBoolean("value"))
                "stringSet" -> {
                    val array = item.optJSONArray("value") ?: JSONArray()
                    val values = mutableSetOf<String>()
                    for (i in 0 until array.length()) values += array.getString(i)
                    editor.putStringSet(key, values)
                }
            }
        }
        editor.apply()
    }
}
