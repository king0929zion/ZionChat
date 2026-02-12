package com.zionchat.app.ui.screens.chat

import com.zionchat.app.data.ChatApiClient
import com.zionchat.app.data.HttpHeader
import com.zionchat.app.data.Message
import com.zionchat.app.data.ProviderConfig
import kotlinx.coroutines.flow.first

/**
 * Stream collection and MCP tag content utilities
 */

/**
 * Collect stream content from chat API
 */
internal suspend fun collectStreamContent(
    chatApiClient: ChatApiClient,
    provider: ProviderConfig,
    modelId: String,
    messages: List<Message>,
    extraHeaders: List<HttpHeader>,
    onChunk: ((String) -> Unit)? = null
): String {
    val sb = StringBuilder()
    chatApiClient.chatCompletionsStream(
        provider = provider,
        modelId = modelId,
        messages = messages,
        extraHeaders = extraHeaders
    ).first { delta ->
        delta.content?.let { chunk ->
            sb.append(chunk)
            onChunk?.invoke(chunk)
        }
        false // Continue collecting
    }
    return sb.toString().trim()
}

/**
 * Build MCP tag detail content for display
 */
internal fun buildMcpTagDetailContent(
    round: Int,
    serverName: String?,
    toolName: String,
    argumentsJson: String,
    statusText: String,
    attempts: Int,
    elapsedMs: Long?,
    resultText: String?,
    errorText: String?
): String {
    return buildString {
        append("Round: ")
        appendLine(round)
        if (!serverName.isNullOrBlank()) {
            append("Server: ")
            appendLine(serverName)
        }
        append("Tool: ")
        appendLine(toolName)
        if (argumentsJson.isNotBlank()) {
            appendLine("Arguments:")
            appendLine(argumentsJson.take(800))
        }
        appendLine()
        append("Status: ")
        appendLine(statusText)
        if (attempts > 1) {
            append("Attempts: ")
            appendLine(attempts)
        }
        if (elapsedMs != null) {
            append("Elapsed: ")
            appendLine(formatElapsedDuration(elapsedMs))
        }
        if (!resultText.isNullOrBlank()) {
            appendLine()
            appendLine("Result:")
            appendLine(resultText.take(1200))
        }
        if (!errorText.isNullOrBlank()) {
            appendLine()
            appendLine("Error:")
            appendLine(errorText.take(600))
        }
    }
}

/**
 * Collect stream content with flow (returns flow instead of collecting all)
 */
internal fun collectStreamContentFlow(
    chatApiClient: ChatApiClient,
    provider: ProviderConfig,
    modelId: String,
    messages: List<Message>,
    extraHeaders: List<HttpHeader>
) = chatApiClient.chatCompletionsStream(
    provider = provider,
    modelId = modelId,
    messages = messages,
    extraHeaders = extraHeaders
)
