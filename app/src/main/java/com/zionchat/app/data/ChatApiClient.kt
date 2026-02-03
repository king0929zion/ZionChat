package com.zionchat.app.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ChatApiClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun listModels(
        provider: ProviderConfig,
        extraHeaders: List<HttpHeader> = emptyList()
    ): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = provider.apiUrl.trimEnd('/') + "/models"
                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer ${provider.apiKey}")
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
    ): Flow<String> = flow {
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
                if (line.startsWith("data: ")) {
                    val data = line.substring(6)
                    if (data == "[DONE]") break

                    try {
                        val chunk = gson.fromJson(data, OpenAIStreamChunk::class.java)
                        val content = chunk.choices?.firstOrNull()?.delta?.content
                        if (!content.isNullOrEmpty()) {
                            emit(content)
                        }
                    } catch (_: Exception) {
                        // 忽略解析错误，继续处理下一行
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

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
                    parsed.data?.firstOrNull()?.url
                        ?: parsed.data?.firstOrNull()?.b64_json
                        ?: error("No image URL in response")
                }
            }
        }
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
    val content: String?
)

// 图片生成相关数据类
data class ImageGenerationResponse(
    val data: List<ImageData>?
)

data class ImageData(
    val url: String?,
    val b64_json: String?
)
