package com.zionchat.app.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

class ChatApiClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun listModels(
        provider: ProviderConfig,
        extraHeaders: List<HttpHeader> = emptyList()
    ): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            when (provider.type.trim().lowercase()) {
                "codex" -> runCatching { listCodexModels(provider) }
                "antigravity" -> runCatching { listAntigravityModels(provider) }
                else ->
                    runCatching {
                        val url = provider.apiUrl.trimEnd('/') + "/models"
                        val requestBuilder =
                            Request.Builder()
                                .url(url)
                                .get()
                                .addHeader("Accept", "application/json")
                                .addHeader("Authorization", "Bearer ${provider.apiKey}")
                        if (isIFlow(provider)) {
                            requestBuilder.addHeader("User-Agent", IFLOW_USER_AGENT)
                        }
                        extraHeaders
                            .filter { it.key.isNotBlank() }
                            .forEach { header -> requestBuilder.addHeader(header.key.trim(), header.value) }

                        client.newCall(requestBuilder.build()).execute().use { response ->
                            val raw = response.body?.string().orEmpty()
                            if (!response.isSuccessful) {
                                error("HTTP ${response.code}: $raw")
                            }
                            val parsed = gson.fromJson(raw, OpenAIModelsResponse::class.java)
                            parsed.data?.mapNotNull { it.id?.trim()?.takeIf { id -> id.isNotBlank() } }.orEmpty()
                        }
                    }
            }
        }
    }

    private fun listCodexModels(provider: ProviderConfig): List<String> {
        val base = provider.apiUrl.trimEnd('/')
        val endpoints =
            listOf(
                "$base/models",
                "$base/v1/models"
            )

        for (url in endpoints) {
            val requestBuilder =
                Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer ${provider.apiKey}")
                    .addHeader("Openai-Beta", "responses=experimental")
                    .addHeader("Version", "0.21.0")
                    .addHeader("Session_id", UUID.randomUUID().toString())
                    .addHeader("User-Agent", CODEX_USER_AGENT)
                    .addHeader("Connection", "Keep-Alive")

            val isOAuthToken =
                provider.oauthProvider?.trim()?.equals("codex", ignoreCase = true) == true ||
                    !provider.oauthAccessToken.isNullOrBlank() ||
                    !provider.oauthRefreshToken.isNullOrBlank() ||
                    !provider.oauthIdToken.isNullOrBlank()
            if (isOAuthToken) {
                requestBuilder.addHeader("Originator", "codex_cli_rs")
                provider.oauthAccountId?.trim()?.takeIf { it.isNotBlank() }?.let { accountId ->
                    requestBuilder.addHeader("Chatgpt-Account-Id", accountId)
                }
            }

            val request = requestBuilder.build()
            val ids =
                runCatching {
                    client.newCall(request).execute().use { response ->
                        val raw = response.body?.string().orEmpty()
                        if (!response.isSuccessful) return@use emptyList<String>()
                        parseModelIdsFromJson(raw)
                    }
                }.getOrElse { emptyList() }

            if (ids.isNotEmpty()) return ids
        }

        return CODEX_DEFAULT_MODELS
    }

    private fun parseModelIdsFromJson(raw: String): List<String> {
        val element = runCatching { JsonParser.parseString(raw) }.getOrNull() ?: return emptyList()
        if (element.isJsonArray) {
            return element.asJsonArray.mapNotNull { el ->
                when {
                    el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString.trim()
                    el.isJsonObject -> el.asJsonObject.get("id")?.asString?.trim()
                    else -> null
                }?.takeIf { it.isNotBlank() }
            }.distinct()
        }

        if (!element.isJsonObject) return emptyList()
        val obj = element.asJsonObject

        // OpenAI style: {"object":"list","data":[{"id":"..."}, ...]}
        obj.getAsJsonArray("data")?.let { arr ->
            val ids =
                arr.mapNotNull { el ->
                    if (!el.isJsonObject) return@mapNotNull null
                    el.asJsonObject.get("id")?.asString?.trim()?.takeIf { it.isNotBlank() }
                }
            if (ids.isNotEmpty()) return ids.distinct()
        }

        // Other common shapes: {"models":[...]} or {"items":[...]}
        listOf("models", "items").forEach { key ->
            obj.getAsJsonArray(key)?.let { arr ->
                val ids =
                    arr.mapNotNull { el ->
                        when {
                            el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString.trim()
                            el.isJsonObject -> el.asJsonObject.get("id")?.asString?.trim()
                            else -> null
                        }?.takeIf { it.isNotBlank() }
                    }
                if (ids.isNotEmpty()) return ids.distinct()
            }
        }

        return emptyList()
    }

    suspend fun chatCompletions(
        provider: ProviderConfig,
        modelId: String,
        messages: List<Message>,
        extraHeaders: List<HttpHeader> = emptyList()
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = provider.apiUrl.trimEnd('/') + "/chat/completions"
                val body = gson.toJson(
                    mapOf(
                        "model" to modelId,
                        "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) }
                    )
                )
                val requestBuilder = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer ${provider.apiKey}")
                if (isIFlow(provider)) {
                    requestBuilder.addHeader("User-Agent", IFLOW_USER_AGENT)
                }
                extraHeaders
                    .filter { it.key.isNotBlank() }
                    .forEach { header -> requestBuilder.addHeader(header.key.trim(), header.value) }
                val request = requestBuilder
                    .post(body.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code}: $raw")
                    }

                    val parsed = gson.fromJson(raw, OpenAIChatCompletionsResponse::class.java)
                    parsed.choices?.firstOrNull()?.message?.content?.trim().orEmpty()
                }
            }
        }
    }

    /**
     * 流式聊天完成 - 返回 Flow，每次发射一个增量内容片段
     */
    fun chatCompletionsStream(
        provider: ProviderConfig,
        modelId: String,
        messages: List<Message>,
        extraHeaders: List<HttpHeader> = emptyList()
    ): Flow<ChatStreamDelta> {
        return when (provider.type.trim().lowercase()) {
            "codex" -> codexResponsesStream(provider, modelId, messages, extraHeaders)
            "antigravity" -> antigravityStream(provider, modelId, messages, extraHeaders)
            else -> openAIChatCompletionsStream(provider, modelId, messages, extraHeaders)
        }
    }

    private fun openAIChatCompletionsStream(
        provider: ProviderConfig,
        modelId: String,
        messages: List<Message>,
        extraHeaders: List<HttpHeader>
    ): Flow<ChatStreamDelta> = flow {
        val url = provider.apiUrl.trimEnd('/') + "/chat/completions"
        val body = gson.toJson(
            mapOf(
                "model" to modelId,
                "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
                "stream" to true
            )
        )
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${provider.apiKey}")
            .addHeader("Accept", "text/event-stream")
        if (isIFlow(provider)) {
            requestBuilder.addHeader("User-Agent", IFLOW_USER_AGENT)
        }
        extraHeaders
            .filter { it.key.isNotBlank() }
            .forEach { header -> requestBuilder.addHeader(header.key.trim(), header.value) }
        val request = requestBuilder
            .post(body.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                throw IllegalStateException("HTTP ${response.code}: $errorBody")
            }

            val source = response.body?.source()
                ?: throw IllegalStateException("Response body is null")

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (line.startsWith("data:")) {
                    val data = line.removePrefix("data:").trimStart()
                    if (data == "[DONE]") break

                    try {
                        val chunk = gson.fromJson(data, OpenAIStreamChunk::class.java)
                        val delta = chunk.choices?.firstOrNull()?.delta
                        val content = delta?.content
                        val reasoning = delta?.reasoning_content ?: delta?.reasoning ?: delta?.thinking
                        if (!content.isNullOrEmpty() || !reasoning.isNullOrEmpty()) {
                            emit(ChatStreamDelta(content = content, reasoning = reasoning))
                        }
                    } catch (_: Exception) {
                        // 忽略解析错误，继续处理下一行
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun codexResponsesStream(
        provider: ProviderConfig,
        modelId: String,
        messages: List<Message>,
        extraHeaders: List<HttpHeader>
    ): Flow<ChatStreamDelta> = flow {
        val url = provider.apiUrl.trimEnd('/') + "/responses"

        val input =
            messages.map { message ->
                val role = if (message.role == "system") "developer" else message.role
                val partType = if (message.role == "assistant") "output_text" else "input_text"
                mapOf(
                    "type" to "message",
                    "role" to role,
                    "content" to listOf(mapOf("type" to partType, "text" to message.content))
                )
            }

        val body =
            gson.toJson(
                mapOf(
                    "model" to modelId,
                    "stream" to true,
                    "store" to false,
                    "instructions" to "",
                    "input" to input,
                    "parallel_tool_calls" to true,
                    "reasoning" to mapOf("effort" to "medium", "summary" to "auto"),
                    "include" to listOf("reasoning.encrypted_content")
                )
            )

        val requestBuilder =
            Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${provider.apiKey}")
                .addHeader("Accept", "text/event-stream")
                .addHeader("Openai-Beta", "responses=experimental")
                .addHeader("Version", "0.21.0")
                .addHeader("Session_id", UUID.randomUUID().toString())
                .addHeader("User-Agent", CODEX_USER_AGENT)
                .addHeader("Connection", "Keep-Alive")

        val isOAuthToken =
            provider.oauthProvider?.trim()?.equals("codex", ignoreCase = true) == true ||
                !provider.oauthAccessToken.isNullOrBlank() ||
                !provider.oauthRefreshToken.isNullOrBlank() ||
                !provider.oauthIdToken.isNullOrBlank()
        if (isOAuthToken) {
            requestBuilder.addHeader("Originator", "codex_cli_rs")
            provider.oauthAccountId?.trim()?.takeIf { it.isNotBlank() }?.let { accountId ->
                requestBuilder.addHeader("Chatgpt-Account-Id", accountId)
            }
        }

        extraHeaders
            .filter { it.key.isNotBlank() }
            .forEach { header -> requestBuilder.addHeader(header.key.trim(), header.value) }

        val request = requestBuilder.post(body.toRequestBody(jsonMediaType)).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                throw IllegalStateException("HTTP ${response.code}: $errorBody")
            }

            val source = response.body?.source() ?: throw IllegalStateException("Response body is null")
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break

                val json = runCatching { JsonParser.parseString(data).asJsonObject }.getOrNull() ?: continue
                val type = json.get("type")?.asString?.trim().orEmpty()
                when (type) {
                    "response.output_text.delta" -> {
                        val delta = json.get("delta")?.asString ?: ""
                        if (delta.isNotEmpty()) emit(ChatStreamDelta(content = delta))
                    }
                    "response.reasoning_summary_text.delta" -> {
                        val delta = json.get("delta")?.asString ?: ""
                        if (delta.isNotEmpty()) emit(ChatStreamDelta(reasoning = delta))
                    }
                    "response.reasoning_summary_text.done" -> emit(ChatStreamDelta(reasoning = "\n\n"))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun antigravityStream(
        provider: ProviderConfig,
        modelId: String,
        messages: List<Message>,
        extraHeaders: List<HttpHeader>
    ): Flow<ChatStreamDelta> = flow {
        val project = provider.oauthProjectId?.trim().takeIf { !it.isNullOrBlank() } ?: generateAntigravityProjectId()

        val systemText =
            messages.filter { it.role == "system" || it.role == "developer" }
                .joinToString("\n\n") { it.content }
                .trim()

        val contents =
            messages.filterNot { it.role == "system" || it.role == "developer" }
                .mapNotNull { message ->
                    val role =
                        when (message.role) {
                            "assistant" -> "model"
                            "user" -> "user"
                            else -> null
                        } ?: return@mapNotNull null
                    mapOf("role" to role, "parts" to listOf(mapOf("text" to message.content)))
                }

        val requestPayload =
            mutableMapOf<String, Any>(
                "contents" to contents,
                "sessionId" to generateAntigravitySessionId(messages)
            )

        if (systemText.isNotBlank()) {
            requestPayload["systemInstruction"] = mapOf("role" to "user", "parts" to listOf(mapOf("text" to systemText)))
        }

        val body =
            gson.toJson(
                mapOf(
                    "project" to project,
                    "request" to requestPayload,
                    "model" to modelId,
                    "userAgent" to "antigravity",
                    "requestType" to "agent",
                    "requestId" to "agent-${UUID.randomUUID()}"
                )
            )

        val baseUrls = antigravityBaseUrlFallbackOrder(provider)
        var lastError: String? = null

        for ((index, baseUrl) in baseUrls.withIndex()) {
            var completed = false
            val url = baseUrl.trimEnd('/') + "/v1internal:streamGenerateContent?alt=sse"
            val requestBuilder =
                Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer ${provider.apiKey}")
                    .addHeader("Accept", "text/event-stream")
                    .addHeader("User-Agent", ANTIGRAVITY_USER_AGENT)

            extraHeaders
                .filter { it.key.isNotBlank() }
                .forEach { header -> requestBuilder.addHeader(header.key.trim(), header.value) }

            val request = requestBuilder.post(body.toRequestBody(jsonMediaType)).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string().orEmpty()
                    lastError = "HTTP ${response.code}: $errorBody"
                    if (index + 1 < baseUrls.size) return@use
                    throw IllegalStateException(lastError ?: "HTTP ${response.code}")
                }

                completed = true
                val source = response.body?.source() ?: throw IllegalStateException("Response body is null")
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break

                    val json = runCatching { JsonParser.parseString(data).asJsonObject }.getOrNull() ?: continue
                    val responseObj = json.getAsJsonObject("response") ?: json
                    val candidates = responseObj.getAsJsonArray("candidates") ?: continue
                    if (candidates.size() == 0) continue
                    val first = candidates[0].asJsonObject
                    val contentObj = first.getAsJsonObject("content") ?: continue
                    val parts = contentObj.getAsJsonArray("parts") ?: continue
                    parts.forEach { partEl ->
                        val part = runCatching { partEl.asJsonObject }.getOrNull() ?: return@forEach
                        val text = part.get("text")?.asString ?: return@forEach
                        if (text.isEmpty()) return@forEach
                        val isThought = part.get("thought")?.asBoolean ?: false
                        if (isThought) emit(ChatStreamDelta(reasoning = text)) else emit(ChatStreamDelta(content = text))
                    }
                }
            }

            if (completed) return@flow
        }
    }.flowOn(Dispatchers.IO)

    private fun listAntigravityModels(provider: ProviderConfig): List<String> {
        val baseUrls = antigravityBaseUrlFallbackOrder(provider)
        var lastError: String? = null

        baseUrls.forEachIndexed { index, baseUrl ->
            val url = baseUrl.trimEnd('/') + "/v1internal:fetchAvailableModels"
            val request =
                Request.Builder()
                    .url(url)
                    .post("{}".toRequestBody(jsonMediaType))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer ${provider.apiKey}")
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", ANTIGRAVITY_USER_AGENT)
                    .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    lastError = "HTTP ${response.code}: $raw"
                    if (index + 1 < baseUrls.size) return@use
                    error(lastError ?: "HTTP ${response.code}")
                }

                val json = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull() ?: return emptyList()
                val models = json.getAsJsonObject("models") ?: return emptyList()
                return models.entrySet().mapNotNull { entry ->
                    entry.key?.trim()?.takeIf { it.isNotBlank() }
                }.sorted()
            }
        }

        if (!lastError.isNullOrBlank()) error(lastError!!)
        return emptyList()
    }

    /**
     * 图片生成 - 使用 DALL-E 或其他文生图模型
     */
    suspend fun generateImage(
        provider: ProviderConfig,
        modelId: String,
        prompt: String,
        extraHeaders: List<HttpHeader> = emptyList(),
        size: String = "1024x1024",
        quality: String = "standard",
        n: Int = 1
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = provider.apiUrl.trimEnd('/') + "/images/generations"
                val body = gson.toJson(
                    mapOf(
                        "model" to modelId,
                        "prompt" to prompt,
                        "n" to n,
                        "size" to size,
                        "quality" to quality,
                        "response_format" to "url"
                    )
                )
                val requestBuilder = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer ${provider.apiKey}")
                extraHeaders
                    .filter { it.key.isNotBlank() }
                    .forEach { header -> requestBuilder.addHeader(header.key.trim(), header.value) }
                val request = requestBuilder
                    .post(body.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code}: $raw")
                    }

                    val parsed = gson.fromJson(raw, ImageGenerationResponse::class.java)
                    val url = parsed.data?.firstOrNull()?.url
                    if (!url.isNullOrBlank()) return@use url

                    val b64 = parsed.data?.firstOrNull()?.b64_json
                    if (!b64.isNullOrBlank()) return@use "data:image/png;base64,$b64"

                    error("No image URL in response")
                }
            }
        }
    }

    companion object {
        private const val CODEX_USER_AGENT = "codex_cli_rs/0.50.0 (Mac OS 26.0.1; arm64) Apple_Terminal/464"
        private const val ANTIGRAVITY_USER_AGENT = "antigravity/1.104.0 darwin/arm64"
        private const val ANTIGRAVITY_BASE_DAILY = "https://daily-cloudcode-pa.googleapis.com"
        private const val ANTIGRAVITY_BASE_SANDBOX = "https://daily-cloudcode-pa.sandbox.googleapis.com"
        private const val IFLOW_USER_AGENT = "iFlow-Cli"

        private val CODEX_DEFAULT_MODELS =
            listOf(
                "gpt-5.1-codex",
                "gpt-5.1-codex-mini",
                "gpt-5.1-codex-max",
                "gpt-5-codex",
                "gpt-5-codex-mini",
                "gpt-5.2-codex"
            )
    }

    private fun isIFlow(provider: ProviderConfig): Boolean {
        if (provider.oauthProvider?.trim()?.equals("iflow", ignoreCase = true) == true) return true
        if (provider.presetId?.trim()?.equals("iflow", ignoreCase = true) == true) return true
        return provider.apiUrl.contains("iflow", ignoreCase = true)
    }

    private fun generateAntigravityProjectId(): String {
        val adjectives = listOf("useful", "bright", "swift", "calm", "bold")
        val nouns = listOf("fuze", "wave", "spark", "flow", "core")
        val adj = adjectives.random()
        val noun = nouns.random()
        val randomPart = UUID.randomUUID().toString().lowercase().take(5)
        return "$adj-$noun-$randomPart"
    }

    private fun generateAntigravitySessionId(messages: List<Message>): String {
        val firstUserText = messages.firstOrNull { it.role == "user" }?.content?.trim().orEmpty()
        if (firstUserText.isNotBlank()) {
            val digest = MessageDigest.getInstance("SHA-256").digest(firstUserText.toByteArray(Charsets.UTF_8))
            val n = abs(ByteBuffer.wrap(digest.copyOfRange(0, 8)).long)
            return "-$n"
        }
        val n = Random.nextLong(0, 9_000_000_000_000_000_000L)
        return "-$n"
    }

    private fun antigravityBaseUrlFallbackOrder(provider: ProviderConfig): List<String> {
        val custom = provider.apiUrl.trim().trimEnd('/')
        val isProd = custom.contains("cloudcode-pa.googleapis.com", ignoreCase = true) &&
            !custom.contains("daily-cloudcode-pa.googleapis.com", ignoreCase = true)

        val candidates =
            when {
                isProd -> listOf(ANTIGRAVITY_BASE_DAILY, ANTIGRAVITY_BASE_SANDBOX, custom)
                custom.contains("daily-cloudcode-pa.googleapis.com", ignoreCase = true) ->
                    listOf(custom, ANTIGRAVITY_BASE_SANDBOX)
                custom.contains("daily-cloudcode-pa.sandbox.googleapis.com", ignoreCase = true) ->
                    listOf(custom, ANTIGRAVITY_BASE_DAILY)
                else -> listOf(custom)
            }

        return candidates
            .map { it.trim().trimEnd('/') }
            .filter { it.isNotBlank() }
            .distinct()
    }
}

data class OpenAIChatCompletionsResponse(
    val choices: List<OpenAIChoice>?
)

data class OpenAIChoice(
    val message: OpenAIMessage?
)

data class OpenAIMessage(
    val content: String?
)

data class OpenAIModelsResponse(
    val data: List<OpenAIModel>?
)

data class OpenAIModel(
    val id: String?
)

// 流式响应数据类
data class OpenAIStreamChunk(
    val choices: List<OpenAIStreamChoice>?
)

data class OpenAIStreamChoice(
    val delta: OpenAIStreamDelta?
)

data class OpenAIStreamDelta(
    val content: String?,
    val reasoning_content: String? = null,
    val reasoning: String? = null,
    val thinking: String? = null
)

data class ChatStreamDelta(
    val content: String? = null,
    val reasoning: String? = null
)

// 图片生成相关数据类
data class ImageGenerationResponse(
    val data: List<ImageData>?
)

data class ImageData(
    val url: String?,
    val b64_json: String?
)
