package com.example.jeecommandcenter.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class AiProvider(val label: String) {
    GEMINI("Gemini"), NVIDIA("NVIDIA NIM"), OPENAI("OpenAI"), OPENROUTER("OpenRouter"), GROQ("Groq"), TOGETHER("Together AI"), DEEPSEEK("DeepSeek"), CUSTOM_OPENAI_COMPATIBLE("Custom / OpenAI-compatible")
}

data class AiConfig(val provider: AiProvider = AiProvider.GEMINI, val model: String = "gemini-3.8-flash", val endpoint: String = "")
data class AiResult(val success: Boolean, val text: String = "", val error: String? = null)

class AiSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("jee_ai_settings", Context.MODE_PRIVATE)
    private val keyStore = SecureKeyStore(context)
    fun getConfig(): AiConfig {
        val provider = runCatching { AiProvider.valueOf(prefs.getString("provider", AiProvider.GEMINI.name) ?: AiProvider.GEMINI.name) }.getOrDefault(AiProvider.GEMINI)
        return AiConfig(provider, prefs.getString("model", defaultModel(provider)) ?: defaultModel(provider), prefs.getString("endpoint", null)?.trim()?.takeIf { it.isNotBlank() } ?: defaultEndpoint(provider))
    }
    fun saveConfig(provider: AiProvider, model: String, endpoint: String = defaultEndpoint(provider)) { prefs.edit().putString("provider", provider.name).putString("model", model.trim()).putString("endpoint", endpoint.trim()).apply() }
    fun saveApiKey(value: String) = keyStore.save(value.trim())
    fun clearApiKey() = keyStore.clear()
    fun hasApiKey(): Boolean = keyStore.exists()
    internal fun apiKey(): String? = keyStore.read()
    fun defaultModel(provider: AiProvider): String = when (provider) {
        AiProvider.GEMINI -> "gemini-3.8-flash"
        AiProvider.NVIDIA -> "openai/gpt-oss-20b"
        AiProvider.OPENAI -> "gpt-5"
        AiProvider.OPENROUTER -> "openai/gpt-5"
        AiProvider.GROQ -> "openai/gpt-oss-20b"
        AiProvider.TOGETHER -> "meta-llama/Llama-3.3-70B-Instruct-Turbo"
        AiProvider.DEEPSEEK -> "deepseek-flash"
        AiProvider.CUSTOM_OPENAI_COMPATIBLE -> "your-model-id"
    }
    fun defaultEndpoint(provider: AiProvider): String = when (provider) {
        AiProvider.GEMINI -> ""
        AiProvider.NVIDIA -> "https://integrate.api.nvidia.com/v1/chat/completions"
        AiProvider.OPENAI -> "https://api.openai.com/v1/chat/completions"
        AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
        AiProvider.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
        AiProvider.TOGETHER -> "https://api.together.xyz/v1/chat/completions"
        AiProvider.DEEPSEEK -> "https://api.deepseek.com/chat/completions"
        AiProvider.CUSTOM_OPENAI_COMPATIBLE -> ""
    }
}

class AiEngine(private val settings: AiSettingsRepository) {
    suspend fun ask(prompt: String, systemInstruction: String? = null): AiResult = withContext(Dispatchers.IO) {
        val key = settings.apiKey() ?: return@withContext AiResult(false, error = "Add an API key in Settings → AI Hub.")
        val config = settings.getConfig()
        runCatching { when (config.provider) {
            AiProvider.GEMINI -> callGemini(key, config.model, prompt, systemInstruction)
            else -> callOpenAiCompatible(key, config.endpoint.ifBlank { settings.defaultEndpoint(config.provider) }, config.model, prompt, systemInstruction)
        } }.getOrElse { AiResult(false, error = it.message ?: "AI request failed.") }
    }

    suspend fun askStreaming(prompt: String, systemInstruction: String? = null, onChunk: suspend (String) -> Unit): AiResult = withContext(Dispatchers.IO) {
        val key = settings.apiKey() ?: return@withContext AiResult(false, error = "Add an API key in Settings → AI Hub.")
        val config = settings.getConfig()
        runCatching {
            val text = when (config.provider) {
                AiProvider.GEMINI -> streamGemini(key, config.model, prompt, systemInstruction, onChunk)
                else -> streamOpenAiCompatible(key, config.endpoint.ifBlank { settings.defaultEndpoint(config.provider) }, config.model, prompt, systemInstruction, onChunk)
            }
            AiResult(true, text = text)
        }.getOrElse { error ->
            val fallback = runCatching { ask(prompt, systemInstruction) }.getOrNull()
            if (fallback?.success == true && fallback.text.isNotBlank()) { onChunk(fallback.text); fallback }
            else AiResult(false, error = error.message ?: fallback?.error ?: "AI request failed.")
        }
    }

    private fun callGemini(key: String, model: String, prompt: String, systemInstruction: String?): AiResult = postJson(
        URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"), mapOf("x-goog-api-key" to key), geminiBody(prompt, systemInstruction)
    ) { response -> response.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")?.takeIf { it.isNotBlank() } ?: throw IllegalStateException("Gemini returned no text.") }

    private suspend fun streamGemini(key: String, model: String, prompt: String, systemInstruction: String?, onChunk: suspend (String) -> Unit): String = streamJson(
        URL("https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse"), mapOf("x-goog-api-key" to key), geminiBody(prompt, systemInstruction), onChunk
    ) { json -> json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "" }

    private fun callOpenAiCompatible(key: String, endpoint: String, model: String, prompt: String, systemInstruction: String?): AiResult {
        require(endpoint.startsWith("https://")) { "API endpoint must start with https://" }
        require(model.isNotBlank()) { "Model ID cannot be blank." }
        return postJson(URL(endpoint), mapOf("Authorization" to "Bearer $key"), openAiBody(model, prompt, systemInstruction, false)) { response -> response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.takeIf { it.isNotBlank() } ?: throw IllegalStateException("Provider returned no text.") }
    }

    private suspend fun streamOpenAiCompatible(key: String, endpoint: String, model: String, prompt: String, systemInstruction: String?, onChunk: suspend (String) -> Unit): String {
        require(endpoint.startsWith("https://")) { "API endpoint must start with https://" }
        require(model.isNotBlank()) { "Model ID cannot be blank." }
        return streamJson(URL(endpoint), mapOf("Authorization" to "Bearer $key"), openAiBody(model, prompt, systemInstruction, true), onChunk) { json -> json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.optString("content") ?: "" }
    }

    private fun geminiBody(prompt: String, systemInstruction: String?): JSONObject = JSONObject().apply {
        if (!systemInstruction.isNullOrBlank()) put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
        put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
    }

    private fun openAiBody(model: String, prompt: String, systemInstruction: String?, stream: Boolean): JSONObject {
        val messages = JSONArray()
        if (!systemInstruction.isNullOrBlank()) messages.put(JSONObject().put("role", "system").put("content", systemInstruction))
        messages.put(JSONObject().put("role", "user").put("content", prompt))
        return JSONObject().put("model", model.trim()).put("messages", messages).put("stream", stream)
    }

    private suspend fun streamJson(url: URL, headers: Map<String, String>, body: JSONObject, onChunk: suspend (String) -> Unit, extractor: (JSONObject) -> String): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 15_000; readTimeout = 120_000; doOutput = true
            setRequestProperty("Content-Type", "application/json")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        connection.outputStream.use { it.write(body.toString().toByteArray()) }
        val status = connection.responseCode
        if (status !in 200..299) {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty(); connection.disconnect(); throw IllegalStateException(parseError(error, status))
        }
        val reader = connection.inputStream.bufferedReader(); val collected = StringBuilder()
        try {
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isBlank() || payload == "[DONE]") continue
                val chunk = runCatching { extractor(JSONObject(payload)) }.getOrDefault("")
                if (chunk.isNotEmpty()) { collected.append(chunk); onChunk(chunk) }
            }
        } finally { reader.close(); connection.disconnect() }
        if (collected.isEmpty()) throw IllegalStateException("Provider returned no streamed text.")
        return collected.toString()
    }

    private fun postJson(url: URL, headers: Map<String, String>, body: JSONObject, extractor: (JSONObject) -> String): AiResult {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 15_000; readTimeout = 60_000; doOutput = true
            setRequestProperty("Content-Type", "application/json")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        connection.outputStream.use { it.write(body.toString().toByteArray()) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty(); connection.disconnect()
        val response = runCatching { JSONObject(text) }.getOrElse { throw IllegalStateException("Provider returned an invalid response ($status).") }
        if (status !in 200..299) throw IllegalStateException(parseError(text, status))
        return AiResult(true, text = extractor(response))
    }

    private fun parseError(text: String, status: Int): String = runCatching { val obj = JSONObject(text); obj.optString("message").ifBlank { obj.optString("error").ifBlank { "HTTP $status" } } }.getOrDefault("HTTP $status")
}
