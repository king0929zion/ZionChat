package com.zionchat.app.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ChatApiClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

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
