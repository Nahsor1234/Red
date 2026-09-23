package com.example.jeecommandcenter.data

import org.json.JSONObject

/** Safe recovery facade around the existing versioned backup repository. */
class BackupRecoveryManager(private val repository: BackupRepository) {
    data class Preview(
        val version: Int,
        val createdAt: Long,
        val includesApiKey: Boolean,
        val valid: Boolean,
        val error: String? = null
    )

    fun createBackup(): String = repository.exportJson()

    fun preview(json: String): Preview = runCatching {
        val root = JSONObject(json)
        val version = root.optInt("version", 0)
        val createdAt = root.optLong("createdAt", 0L)
        val includesApiKey = root.optBoolean("includesApiKey", true)
        require(root.optString("format") == "JeE backup") { "Invalid backup format" }
        require(version > 0) { "Missing backup version" }
        require(!includesApiKey) { "Backup contains an API key" }
        require(root.optJSONObject("preferences") != null) { "Missing backup data" }
        Preview(version, createdAt, includesApiKey, true)
    }.getOrElse { error ->
        Preview(0, 0L, false, false, error.message ?: "Invalid backup")
    }

    fun restore(json: String): Result<Unit> {
        val check = preview(json)
        if (!check.valid) return Result.failure(IllegalArgumentException(check.error))
        return repository.importJson(json)
    }

    fun recoverLastKnownGood(): Result<Unit> = repository.recoverLastKnownGood()

    fun hasRecoveryPoint(): Boolean = repository.hasRecoveryPoint()
}
