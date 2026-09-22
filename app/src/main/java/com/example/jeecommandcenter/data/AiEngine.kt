package com.example.jeecommandcenter.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class AiProvider {
    GEMINI,
    NVIDIA
}

data class AiConfig(
    val provider: AiProvider = AiProvider.GEMINI,
    val model: String = "gemini-3.8-flash"
)

data class AiResult(
    val success: Boolean,
    val text: String = "",
    val error: String? = null
)

class AiSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("jee_ai_settings", Context.MODE_PRIVATE)
    private val keyStore = SecureKeyStore(context)

    fun getConfig(): AiConfig {
        val provider = runCatching {
            AiProvider.valueOf(prefs.getString("provider", AiProvider.GEMINI.name) ?: AiProvider.GEMINI.name)
        }.getOrDefault(AiProvider.GEMINI)
        return AiConfig(
            provider = provider,
            model = prefs.getString("model", defaultModel(provider)) ?: defaultModel(provider)
        )
    }

    fun saveConfig(provider: AiProvider, model: String) {
        prefs.edit().putString("provider", provider.name).putString("model", model).apply()
    }

    fun saveApiKey(value: String) {
        keyStore.save(value.trim())
    }

    fun clearApiKey() {
        keyStore.clear()
    }

    fun hasApiKey(): Boolean = keyStore.exists()

    fun defaultModel(provider: AiProvider): String = when (provider) {
        AiProvider.GEMINI -> "gemini-3.8-flash"
        AiProvider.NVIDIA -> "openai/gpt-oss-20b"
    }

    internal fun apiKey(): String? = keyStore.read()
}

class AiEngine(private val settings: AiSettingsRepository) {

    suspend fun ask(prompt: String, systemInstruction: String? = null): AiResult =
        withContext(Dispatchers.IO) {
            val key = settings.apiKey()
                ?: return@withContext AiResult(false, error = "Add an API key in Settings → AI Hub.")
            val config = settings.getConfig()
            runCatching {
                when (config.provider) {
                    AiProvider.GEMINI -> callGemini(key, config.model, prompt, systemInstruction)
                    AiProvider.NVIDIA -> callNvidia(key, config.model, prompt, systemInstruction)
                }
            }.getOrElse { error ->
                AiResult(false, error = error.message ?: "AI request failed.")
            }
        }

    private fun callGemini(
        key: String,
        model: String,
        prompt: String,
        systemInstruction: String?
    ): AiResult {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
        val body = JSONObject().apply {
            if (!systemInstruction.isNullOrBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
            }
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
        }
        return postJson(url, mapOf("x-goog-api-key" to key), body) { response ->
            response.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Gemini returned no text.")
        }
    }

    private fun callNvidia(
        key: String,
        model: String,
        prompt: String,
        systemInstruction: String?
    ): AiResult {
        val messages = JSONArray()
        if (!systemInstruction.isNullOrBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemInstruction))
        }
        messages.put(JSONObject().put("role", "user").put("content", prompt))
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.2)
            .put("max_tokens", 1400)
        return postJson(
            URL("https://integrate.api.nvidia.com/v1/chat/completions"),
            mapOf("Authorization" to "Bearer $key"),
            body
        ) { response ->
            response.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("NVIDIA returned no text.")
        }
    }

    private fun postJson(
        url: URL,
        headers: Map<String, String>,
        body: JSONObject,
        extractor: (JSONObject) -> String
    ): AiResult {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

        connection.outputStream.use { it.write(body.toString().toByteArray()) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()

        val response = runCatching { JSONObject(text) }.getOrElse {
            throw IllegalStateException("Provider returned an invalid response ($status).")
        }

        if (status !in 200..299) {
            val detail = response.optString("message").ifBlank {
                response.optString("error").ifBlank { "HTTP $status" }
            }
            throw IllegalStateException(detail)
        }
        return AiResult(true, text = extractor(response))
    }
}
