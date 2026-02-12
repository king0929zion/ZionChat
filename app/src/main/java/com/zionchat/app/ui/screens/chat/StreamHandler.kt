package com.zionchat.app.ui.screens.chat

import com.zionchat.app.data.ChatApiClient
import com.zionchat.app.data.HttpHeader
import com.zionchat.app.data.Message
import com.zionchat.app.data.ProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Sealed class representing a stream chunk from chat API
 */
sealed class StreamChunk {
    data class Content(val text: String) : StreamChunk()
    data class Reasoning(val text: String) : StreamChunk()
    object Done : StreamChunk()
    data class Error(val message: String, val exception: Throwable? = null) : StreamChunk()
}

/**
 * Result of a complete stream
 */
data class StreamResult(
    val content: String,
    val reasoning: String? = null,
    val success: Boolean,
    val error: String? = null
)

/**
 * Interface for handling streaming responses
 */
interface StreamHandler {
    /**
     * Process a stream and emit chunks
     */
    fun processStream(
        chatApiClient: ChatApiClient,
        provider: ProviderConfig,
        modelId: String,
        messages: List<Message>,
        extraHeaders: List<HttpHeader> = emptyList(),
        reasoningEffort: String? = null,
        conversationId: String? = null
    ): Flow<StreamChunk>

    /**
     * Collect a complete stream and return the result
     */
    suspend fun collectCompleteStream(
        chatApiClient: ChatApiClient,
        provider: ProviderConfig,
        modelId: String,
        messages: List<Message>,
        extraHeaders: List<HttpHeader> = emptyList()
    ): StreamResult
}

/**
 * Base implementation of StreamHandler with common functionality
 */
class BaseStreamHandler : StreamHandler {

    override fun processStream(
        chatApiClient: ChatApiClient,
        provider: ProviderConfig,
        modelId: String,
        messages: List<Message>,
        extraHeaders: List<HttpHeader>,
        reasoningEffort: String?,
        conversationId: String?
    ): Flow<StreamChunk> {
        return chatApiClient.chatCompletionsStream(
            provider = provider,
            modelId = modelId,
            messages = messages,
            extraHeaders = extraHeaders,
            reasoningEffort = reasoningEffort,
            conversationId = conversationId
        ).map { delta ->
            when {
                delta.content != null -> StreamChunk.Content(delta.content)
                delta.reasoning != null -> StreamChunk.Reasoning(delta.reasoning)
                else -> StreamChunk.Done
            }
        }
    }

    override suspend fun collectCompleteStream(
        chatApiClient: ChatApiClient,
        provider: ProviderConfig,
        modelId: String,
        messages: List<Message>,
        extraHeaders: List<HttpHeader>
    ): StreamResult {
        val contentBuilder = StringBuilder()
        val reasoningBuilder = StringBuilder()
        var error: String? = null

        try {
            chatApiClient.chatCompletionsStream(
                provider = provider,
                modelId = modelId,
                messages = messages,
                extraHeaders = extraHeaders
            ).collect { delta ->
                delta.content?.let { contentBuilder.append(it) }
                delta.reasoning?.let { reasoningBuilder.append(it) }
            }
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
        }

        val content = contentBuilder.toString().trim()
        val reasoning = reasoningBuilder.toString().trim().ifBlank { null }

        return StreamResult(
            content = content,
            reasoning = reasoning,
            success = error == null && content.isNotBlank(),
            error = error
        )
    }
}

/**
 * Helper class for extracting thinking content from streaming responses
 */
class ThinkingExtractor {
    private var inThink = false
    private var remainder = ""
    private val keepTail = 12

    /**
     * Process a chunk and extract visible vs thinking content
     */
    fun processChunk(input: String): ProcessedChunk {
        if (input.isEmpty()) return ProcessedChunk()

        var source = remainder + input
        remainder = ""
        val visibleBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()
        var cursor = 0

        while (cursor < source.length) {
            if (!inThink) {
                val idxThink = source.indexOf(" HS", cursor, ignoreCase = true)
                val idxThinking = source.indexOf("<thinking>", cursor, ignoreCase = true)
                val next = when {
                    idxThink < 0 -> idxThinking
                    idxThinking < 0 -> idxThink
                    else -> minOf(idxThink, idxThinking)
                }

                if (next < 0) {
                    val safeEnd = maxOf(cursor, source.length - keepTail)
                    if (safeEnd > cursor) {
                        visibleBuilder.append(source.substring(cursor, safeEnd))
                    }
                    remainder = source.substring(safeEnd)
                    return ProcessedChunk(visibleBuilder.toString(), "")
                }

                if (next > cursor) {
                    visibleBuilder.append(source.substring(cursor, next))
                }

                inThink = true
                cursor = if (idxThinking == next) {
                    next + "<thinking>".length
                } else {
                    next + " HS".length
                }
            } else {
                val idxEndThink = source.indexOf(" SSA", cursor, ignoreCase = true)
                val idxEndThinking = source.indexOf("</thinking>", cursor, ignoreCase = true)
                val next = when {
                    idxEndThink < 0 -> idxEndThinking
                    idxEndThinking < 0 -> idxEndThink
                    else -> minOf(idxEndThink, idxEndThinking)
                }

                if (next < 0) {
                    val safeEnd = maxOf(cursor, source.length - keepTail)
                    if (safeEnd > cursor) {
                        thinkingBuilder.append(source.substring(cursor, safeEnd))
                    }
                    remainder = source.substring(safeEnd)
                    return ProcessedChunk(visibleBuilder.toString(), thinkingBuilder.toString())
                }

                if (next > cursor) {
                    thinkingBuilder.append(source.substring(cursor, next))
                }

                inThink = false
                cursor = if (idxEndThinking == next) {
                    next + "</thinking>".length
                } else {
                    next + " SSA".length
                }
            }
        }

        return ProcessedChunk(visibleBuilder.toString(), thinkingBuilder.toString())
    }

    /**
     * Flush any remaining content
     */
    fun flush(): ProcessedChunk {
        val result = ProcessedChunk(
            visible = if (!inThink) remainder else "",
            thinking = if (inThink) remainder else ""
        )
        remainder = ""
        return result
    }

    /**
     * Reset the extractor state
     */
    fun reset() {
        inThink = false
        remainder = ""
    }

    /**
     * Check if currently in thinking mode
     */
    fun isInThink(): Boolean = inThink
}

/**
 * Result of processing a chunk
 */
data class ProcessedChunk(
    val visible: String = "",
    val thinking: String = ""
)

/**
 * Helper for extracting inline MCP call blocks from streaming content
 */
class InlineCallExtractor {
    private val inlineCallRegex = Regex("(?is)<(?:mcp_call|tool_call)\\b[^>]*>(.*?)</(?:mcp_call|tool_call)>")
    private val inlineCallStartRegex = Regex("(?is)<(?:mcp_call|tool_call)\\b")
    private var tail = ""
    private val safeTailLen = 24

    /**
     * Process a chunk and extract visible text vs call blocks
     */
    fun processChunk(input: String, flush: Boolean = false): ExtractionResult {
        if (input.isEmpty() && !flush) return ExtractionResult()

        var source = tail + input
        tail = ""
        if (source.isEmpty()) return ExtractionResult()

        val visibleBuilder = StringBuilder()
        val blocks = mutableListOf<String>()
        var cursor = 0

        inlineCallRegex.findAll(source).forEach { match ->
            if (match.range.first > cursor) {
                visibleBuilder.append(source.substring(cursor, match.range.first))
            }
            val payload = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (payload.isNotBlank()) {
                blocks.add(payload)
            }
            cursor = match.range.last + 1
        }

        if (cursor >= source.length) return ExtractionResult(visibleBuilder.toString(), blocks)

        val remaining = source.substring(cursor)
        val openIdx = inlineCallStartRegex.find(remaining)?.range?.first ?: -1

        if (openIdx >= 0) {
            if (openIdx > 0) {
                visibleBuilder.append(remaining.substring(0, openIdx))
            }
            val tailCandidate = remaining.substring(openIdx)
            if (flush) {
                visibleBuilder.append(tailCandidate)
            } else {
                tail = tailCandidate
            }
        } else if (flush) {
            visibleBuilder.append(remaining)
        } else {
            if (remaining.length > safeTailLen) {
                visibleBuilder.append(remaining.dropLast(safeTailLen))
                tail = remaining.takeLast(safeTailLen)
            } else {
                tail = remaining
            }
        }

        return ExtractionResult(visibleBuilder.toString(), blocks)
    }

    /**
     * Reset the extractor state
     */
    fun reset() {
        tail = ""
    }
}

/**
 * Result of inline call extraction
 */
data class ExtractionResult(
    val visibleText: String = "",
    val callBlocks: List<String> = emptyList()
)
