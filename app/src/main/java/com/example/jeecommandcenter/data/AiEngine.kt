package com.example.jeecommandcenter.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class AiProvider(val label: String) {
    GEMINI("Gemini"),
    NVIDIA("NVIDIA NIM"),
    OPENAI("OpenAI"),
    OPENROUTER("OpenRouter"),
    GROQ("Groq"),
    TOGETHER("Together AI"),
    DEEPSEEK("DeepSeek"),
    CUSTOM_OPENAI_COMPATIBLE("Custom / OpenAI-compatible")
}

data class AiConfig(
    val provider: AiProvider = AiProvider.GEMINI,
    val model: String = "gemini-3.8-flash",
    val endpoint: String = ""
)

data class AiResult(
    val success: Boolean,
    val text: String = "",
    val error: String? = null
)

data class AiPromptMessage(
    val role: String,
    val content: String
)

class AiSettingsRepository(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("jee_ai_settings", android.content.Context.MODE_PRIVATE)
    private val keyStore = SecureKeyStore(context)

    fun getConfig(): AiConfig {
        val provider = runCatching {
            AiProvider.valueOf(
                prefs.getString("provider", AiProvider.GEMINI.name) ?: AiProvider.GEMINI.name
            )
        }.getOrDefault(AiProvider.GEMINI)

        return AiConfig(
            provider,
            prefs.getString("model", defaultModel(provider)) ?: defaultModel(provider),
            prefs.getString("endpoint", null)?.trim()?.takeIf { it.isNotBlank() }
                ?: defaultEndpoint(provider)
        )
    }

    fun saveConfig(
        provider: AiProvider,
        model: String,
        endpoint: String = defaultEndpoint(provider)
    ) {
        prefs.edit()
            .putString("provider", provider.name)
            .putString("model", model.trim())
            .putString("endpoint", endpoint.trim())
            .apply()
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

    internal fun apiKey(): String? = keyStore.read()
}

class AiEngine(private val settings: AiSettingsRepository) {

    suspend fun ask(
        prompt: String,
        systemInstruction: String? = null
    ): AiResult = askConversation(
        messages = listOf(AiPromptMessage("user", prompt)),
        systemInstruction = systemInstruction
    )

    suspend fun askConversation(
        messages: List<AiPromptMessage>,
        systemInstruction: String? = null
    ): AiResult = withContext(Dispatchers.IO) {
        val key = settings.apiKey()
            ?: return@withContext AiResult(false, error = "Add an API key in Settings → AI Hub.")
        val config = settings.getConfig()

        runCatching {
            when (config.provider) {
                AiProvider.GEMINI ->
                    callGemini(key, config.model, messages, systemInstruction)
                else ->
                    callOpenAiCompatible(
                        key,
                        config.endpoint.ifBlank { settings.defaultEndpoint(config.provider) },
                        config.model,
                        messages,
                        systemInstruction
                    )
            }
        }.getOrElse {
            AiResult(false, error = it.message ?: "AI request failed.")
        }
    }

    suspend fun stream(
        prompt: String,
        systemInstruction: String? = null,
        onChunk: suspend (String) -> Unit
    ): AiResult = streamConversation(
        messages = listOf(AiPromptMessage("user", prompt)),
        systemInstruction = systemInstruction,
        onChunk = onChunk
    )

    suspend fun streamConversation(
        messages: List<AiPromptMessage>,
        systemInstruction: String? = null,
        onChunk: suspend (String) -> Unit
    ): AiResult = withContext(Dispatchers.IO) {
        val key = settings.apiKey()
            ?: return@withContext AiResult(false, error = "Add an API key in Settings → AI Hub.")
        val config = settings.getConfig()

        runCatching {
            val text = when (config.provider) {
                AiProvider.GEMINI ->
                    streamGemini(key, config.model, messages, systemInstruction, onChunk)
                else ->
                    streamOpenAiCompatible(
                        key,
                        config.endpoint.ifBlank { settings.defaultEndpoint(config.provider) },
                        config.model,
                        messages,
                        systemInstruction,
                        onChunk
                    )
            }
            AiResult(true, text)
        }.getOrElse {
            AiResult(false, error = it.message ?: "AI request failed.")
        }
    }

    suspend fun generateQuiz(
        subject: String,
        chapter: String?,
        difficulty: String,
        count: Int
    ): List<Question> {
        val prompt =
            "Create exactly $count JEE practice MCQs for $subject" +
                (chapter?.let { " chapter '$it'" } ?: "") +
                ". Difficulty: $difficulty. Return ONLY valid JSON array. " +
                "Each object: {\"prompt\":string,\"options\":[4 strings],\"correctIndex\":0-3," +
                "\"explanation\":string,\"topic\":string}. No markdown."

        val result = ask(
            prompt,
            "You generate accurate educational multiple-choice questions for JEE. " +
                "Exactly one option must be correct. Never invent exam claims."
        )

        if (!result.success) return emptyList()
        return parseQuiz(result.text, subject, chapter ?: "General", difficulty)
    }

    private fun parseQuiz(
        raw: String,
        subject: String,
        chapter: String,
        difficulty: String
    ): List<Question> {
        val clean = raw.substringAfter("[").let {
            "[" + it.substringBeforeLast("]") + "]"
        }

        return runCatching {
            val array = JSONArray(clean)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                val options = o.optJSONArray("options") ?: JSONArray()
                require(options.length() == 4)

                val d = when (difficulty.lowercase()) {
                    "easy" -> 1
                    "hard" -> 5
                    else -> 3
                }

                Question(
                    "ai_${System.currentTimeMillis()}_$i",
                    subject,
                    "ai_${subject.lowercase()}",
                    chapter,
                    o.optString("topic", "Core concepts"),
                    o.getString("prompt"),
                    List(4) { options.getString(it) },
                    o.getInt("correctIndex").coerceIn(0, 3),
                    o.optString("explanation", "Review the underlying concept."),
                    d,
                    "AI generated"
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun callGemini(
        key: String,
        model: String,
        messages: List<AiPromptMessage>,
        systemInstruction: String?
    ): AiResult {
        val contents = JSONArray()
        messages.filter { it.content.isNotBlank() }.forEach { message ->
            contents.put(
                JSONObject()
                    .put("role", if (message.role == "assistant") "model" else "user")
                    .put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", message.content))
                    )
            )
        }

        val body = JSONObject().apply {
            if (!systemInstruction.isNullOrBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemInstruction))
                    )
                )
            }
            put("contents", contents)
        }

        return postJson(
            URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"),
            mapOf("x-goog-api-key" to key),
            body
        ) { response ->
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

    private fun callOpenAiCompatible(
        key: String,
        endpoint: String,
        model: String,
        messages: List<AiPromptMessage>,
        systemInstruction: String?
    ): AiResult {
        require(endpoint.startsWith("https://")) { "API endpoint must start with https://" }
        require(model.isNotBlank()) { "Model ID cannot be blank." }

        val requestMessages = JSONArray()
        if (!systemInstruction.isNullOrBlank()) {
            requestMessages.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", systemInstruction)
            )
        }

        messages.filter { it.content.isNotBlank() }.forEach { message ->
            requestMessages.put(
                JSONObject()
                    .put(
                        "role",
                        if (message.role == "assistant") "assistant" else "user"
                    )
                    .put("content", message.content)
            )
        }

        return postJson(
            URL(endpoint),
            mapOf("Authorization" to "Bearer $key"),
            JSONObject()
                .put("model", model.trim())
                .put("messages", requestMessages)
        ) { response ->
            response.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Provider returned no text.")
        }
    }

    private suspend fun streamOpenAiCompatible(
        key: String,
        endpoint: String,
        model: String,
        messages: List<AiPromptMessage>,
        systemInstruction: String?,
        onChunk: suspend (String) -> Unit
    ): String {
        require(endpoint.startsWith("https://")) { "API endpoint must start with https://" }
        require(model.isNotBlank()) { "Model ID cannot be blank." }

        val requestMessages = JSONArray()
        if (!systemInstruction.isNullOrBlank()) {
            requestMessages.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", systemInstruction)
            )
        }

        messages.filter { it.content.isNotBlank() }.forEach { message ->
            requestMessages.put(
                JSONObject()
                    .put(
                        "role",
                        if (message.role == "assistant") "assistant" else "user"
                    )
                    .put("content", message.content)
            )
        }

        val connection = open(
            URL(endpoint),
            mapOf(
                "Authorization" to "Bearer $key",
                "Accept" to "text/event-stream"
            )
        )

        connection.outputStream.use {
            it.write(
                JSONObject()
                    .put("model", model)
                    .put("messages", requestMessages)
                    .put("stream", true)
                    .toString()
                    .toByteArray()
            )
        }

        check(connection.responseCode in 200..299) {
            "AI provider HTTP ${connection.responseCode}"
        }

        val out = StringBuilder()
        connection.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue

                val data = line.removePrefix("data:").trim()
                if (data.isBlank() || data == "[DONE]") continue

                val delta = runCatching {
                    JSONObject(data)
                        .optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?.optString("content")
                        .orEmpty()
                }.getOrDefault("")

                if (delta.isNotEmpty()) {
                    out.append(delta)
                    onChunk(delta)
                }
            }
        }

        connection.disconnect()
        return out.toString()
    }

    private suspend fun streamGemini(
        key: String,
        model: String,
        messages: List<AiPromptMessage>,
        systemInstruction: String?,
        onChunk: suspend (String) -> Unit
    ): String {
        val contents = JSONArray()
        messages.filter { it.content.isNotBlank() }.forEach { message ->
            contents.put(
                JSONObject()
                    .put("role", if (message.role == "assistant") "model" else "user")
                    .put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", message.content))
                    )
            )
        }

        val body = JSONObject().apply {
            if (!systemInstruction.isNullOrBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemInstruction))
                    )
                )
            }
            put("contents", contents)
        }

        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse"
        )
        val connection = open(
            url,
            mapOf(
                "x-goog-api-key" to key,
                "Accept" to "text/event-stream"
            )
        )

        connection.outputStream.use { it.write(body.toString().toByteArray()) }
        check(connection.responseCode in 200..299) {
            "Gemini HTTP ${connection.responseCode}"
        }

        val out = StringBuilder()
        connection.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue

                val data = line.removePrefix("data:").trim()
                if (data.isBlank()) continue

                val delta = runCatching {
                    JSONObject(data)
                        .optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        .orEmpty()
                }.getOrDefault("")

                if (delta.isNotEmpty()) {
                    out.append(delta)
                    onChunk(delta)
                }
            }
        }

        connection.disconnect()
        return out.toString()
    }

    private fun open(
        url: URL,
        headers: Map<String, String>
    ): HttpURLConnection =
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

    private fun postJson(
        url: URL,
        headers: Map<String, String>,
        body: JSONObject,
        extractor: (JSONObject) -> String
    ): AiResult {
        val connection = open(url, headers)
        connection.outputStream.use { it.write(body.toString().toByteArray()) }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()

        val response = runCatching { JSONObject(text) }
            .getOrElse {
                throw IllegalStateException("Provider returned an invalid response ($status).")
            }

        if (status !in 200..299) {
            throw IllegalStateException(
                response.optString("message")
                    .ifBlank { response.optString("error").ifBlank { "HTTP $status" } }
            )
        }

        return AiResult(true, text = extractor(response))
    }
}
