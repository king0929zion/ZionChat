package com.zionchat.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.http.append
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.modelcontextprotocol.kotlin.sdk.types.AudioContent
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.ContentBlock
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import com.zionchat.app.data.mcp.transport.SseClientTransport
import com.zionchat.app.data.mcp.transport.StreamableHttpClientTransport

class McpClient {
    private val okHttpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(120, TimeUnit.SECONDS)
            .followSslRedirects(true)
            .followRedirects(true)
            .build()

    private val ktorClient: HttpClient =
        HttpClient(OkHttp) {
            engine {
                preconfigured = okHttpClient
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        explicitNulls = false
                        classDiscriminatorMode = ClassDiscriminatorMode.NONE
                    }
                )
            }
            install(SSE)
        }

    suspend fun fetchTools(config: McpConfig): Result<List<McpTool>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                withConnectedClient(config) { client ->
                    val tools = client.listTools()?.tools.orEmpty()
                    tools.map { tool -> tool.toUiTool() }
                }
            }
        }
    }

    suspend fun callTool(config: McpConfig, toolCall: McpToolCall): Result<McpToolResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                withConnectedClient(config) { client ->
                    val argsJson = toolCall.arguments.toJsonObject()
                    val result =
                        client.callTool(
                            request = CallToolRequest(
                                params = CallToolRequestParams(
                                    name = toolCall.toolName,
                                    arguments = argsJson,
                                ),
                            ),
                            options = RequestOptions(timeout = 120.seconds),
                        )

                    val isError = result.isError == true
                    val text = result.content.toDisplayText().ifBlank {
                        result.content.toString()
                    }
                    McpToolResult(
                        success = !isError,
                        content = text,
                        error = if (isError) text else null
                    )
                }
            }
        }
    }

    suspend fun testConnection(config: McpConfig): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                withConnectedClient(config) { client ->
                    client.listTools()
                    "Connected"
                }
            }
        }
    }

    private suspend fun <T> withConnectedClient(config: McpConfig, block: suspend (Client) -> T): T {
        val transport = buildTransport(config)
        val client =
            Client(
                clientInfo = Implementation(
                    name = config.name.trim().ifBlank { "ZionChat" },
                    version = "1.0"
                )
            )

        client.connect(transport)
        return try {
            block(client)
        } finally {
            runCatching { transport.close() }
        }
    }

    private fun buildTransport(config: McpConfig): AbstractTransport {
        val requestBuilder: HttpRequestBuilder.() -> Unit = {
            headers {
                config.headers.forEach { header ->
                    val key = header.key.trim()
                    if (key.isBlank()) return@forEach
                    append(key, header.value)
                }
            }
        }

        return when (config.protocol) {
            McpProtocol.SSE ->
                SseClientTransport(
                    client = ktorClient,
                    urlString = config.url.trim(),
                    requestBuilder = requestBuilder
                )

            McpProtocol.HTTP ->
                StreamableHttpClientTransport(
                    client = ktorClient,
                    url = config.url.trim(),
                    requestBuilder = requestBuilder
                )
        }
    }

    private fun Tool.toUiTool(): McpTool {
        val parameters = parseToolSchema(inputSchema)
        return McpTool(
            name = name,
            description = description?.trim().orEmpty(),
            parameters = parameters
        )
    }

    private fun parseToolSchema(schema: ToolSchema): List<McpToolParameter> {
        val properties = schema.properties ?: return emptyList()
        val requiredSet = schema.required?.mapNotNull { it.trim().takeIf(String::isNotBlank) }?.toSet().orEmpty()
        return properties.entries.mapNotNull { (name, el) ->
            val key = name.trim()
            if (key.isBlank()) return@mapNotNull null

            val obj = el as? JsonObject
            val type = obj?.get("type").toSchemaTypeString()
            val description = (obj?.get("description") as? JsonPrimitive)?.content?.trim().orEmpty()
            McpToolParameter(
                name = key,
                type = type,
                required = requiredSet.contains(key),
                description = description
            )
        }
    }

    private fun JsonElement?.toSchemaTypeString(): String {
        return when (this) {
            null -> "string"
            is JsonNull -> "string"
            is JsonPrimitive -> content.trim().takeIf { it.isNotBlank() } ?: "string"
            is JsonArray -> joinToString("|") { it.toSchemaTypeString() }.ifBlank { "string" }
            is JsonObject -> (this["type"] as? JsonPrimitive)?.content?.trim()?.takeIf { it.isNotBlank() } ?: "object"
        }
    }

    private fun List<ContentBlock>.toDisplayText(): String {
        return buildString {
            this@toDisplayText.forEachIndexed { index, block ->
                if (index > 0) append("\n\n")
                when (block) {
                    is TextContent -> append(block.text)
                    is ImageContent -> append("[image: ${block.mimeType}]")
                    is AudioContent -> append("[audio: ${block.mimeType}]")
                    else -> append("[${block.type.value}]")
                }
            }
        }.trim()
    }

    private fun Map<String, Any>.toJsonObject(): JsonObject {
        val entries =
            entries.mapNotNull { (rawKey, value) ->
                val key = rawKey.trim()
                if (key.isBlank()) return@mapNotNull null
                key to value.toJsonElement()
            }.toMap()
        return JsonObject(entries)
    }

    private fun Any?.toJsonElement(): JsonElement {
        return when (this) {
            null -> JsonNull
            is JsonElement -> this
            is String -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            is Int -> JsonPrimitive(this)
            is Long -> JsonPrimitive(this)
            is Float -> JsonPrimitive(this)
            is Double -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this.toDouble())
            is Map<*, *> -> {
                val obj =
                    this.entries.mapNotNull { (k, v) ->
                        val key = k?.toString()?.trim().orEmpty()
                        if (key.isBlank()) return@mapNotNull null
                        key to v.toJsonElement()
                    }.toMap()
                JsonObject(obj)
            }

            is List<*> -> JsonArray(this.map { it.toJsonElement() })
            else -> JsonPrimitive(this.toString())
        }
    }
}
