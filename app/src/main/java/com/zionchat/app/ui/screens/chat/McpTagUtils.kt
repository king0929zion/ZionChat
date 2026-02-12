package com.zionchat.app.ui.screens.chat

/**
 * MCP Tag marker utilities for handling tool call tags in message content
 */

internal val mcpTagMarkerRegex =
    Regex("(?is)<!--\\s*mcp_tag:([A-Za-z0-9_-]+)\\s*-->")

/**
 * Build an MCP tag marker comment for insertion into content
 */
internal fun buildMcpTagMarker(tagId: String): String {
    return "<!--mcp_tag:${tagId.trim()}-->"
}

/**
 * Insert MCP tag markers into visible content at specified anchor positions
 */
internal fun insertMcpTagMarkers(visibleContent: String, anchors: List<McpTagAnchor>): String {
    if (anchors.isEmpty()) return visibleContent
    val content = visibleContent
    val sortedAnchors = anchors.sortedBy { it.charIndex }
    val sb = StringBuilder()
    var cursor = 0

    sortedAnchors.forEach { anchor ->
        val safeId = anchor.tagId.trim()
        if (safeId.isBlank()) return@forEach
        val index = anchor.charIndex.coerceIn(0, content.length)
        if (index > cursor) {
            sb.append(content.substring(cursor, index))
        }
        if (sb.isNotEmpty() && sb.last() != '\n') {
            sb.append("\n\n")
        } else if (sb.isNotEmpty()) {
            sb.append('\n')
        }
        sb.append(buildMcpTagMarker(safeId))
        sb.append("\n\n")
        cursor = index
    }

    if (cursor < content.length) {
        sb.append(content.substring(cursor))
    }
    return sb.toString()
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

/**
 * Split content by MCP tag markers into segments
 */
internal fun splitContentByMcpTagMarkers(content: String): List<MessageInlineSegment> {
    if (!mcpTagMarkerRegex.containsMatchIn(content)) return emptyList()
    val result = mutableListOf<MessageInlineSegment>()
    var cursor = 0

    mcpTagMarkerRegex.findAll(content).forEach { match ->
        if (match.range.first > cursor) {
            val text = content.substring(cursor, match.range.first).trim()
            if (text.isNotBlank()) {
                result += MessageInlineSegment(text = text)
            }
        }
        val tagId = match.groupValues.getOrNull(1)?.trim().orEmpty()
        if (tagId.isNotBlank()) {
            result += MessageInlineSegment(tagId = tagId)
        }
        cursor = match.range.last + 1
    }

    if (cursor < content.length) {
        val tail = content.substring(cursor).trim()
        if (tail.isNotBlank()) {
            result += MessageInlineSegment(text = tail)
        }
    }
    return result
}

/**
 * Remove MCP tag markers from content
 */
internal fun stripMcpTagMarkers(content: String): String {
    if (content.isBlank()) return ""
    return content
        .replace(mcpTagMarkerRegex, " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

/**
 * Extract inline MCP call blocks from content
 */
internal fun extractInlineMcpCallBlocks(content: String): InlineMcpCallExtraction {
    val raw = content.trim()
    if (raw.isBlank()) {
        return InlineMcpCallExtraction(visibleText = "", blocks = emptyList())
    }

    val blockRegex = Regex(
        "(?is)<(?:mcp_call|tool_call)\\b[^>]*>(.*?)(?:</(?:mcp_call|tool_call)>|$)"
    )
    val blocks =
        blockRegex.findAll(raw)
            .mapNotNull { match ->
                match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
            }
            .toList()

    val visible =
        raw.replace(blockRegex, " ")
            .replace(Regex("(?is)</(?:mcp_call|tool_call)>"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    return InlineMcpCallExtraction(visibleText = visible, blocks = blocks)
}

/**
 * Split thinking blocks from visible content
 */
internal fun splitThinkingFromContent(content: String): ThinkingSplitResult {
    val raw = content.trim()
    if (raw.isBlank()) return ThinkingSplitResult(visible = "", thinking = "")

    val blockRegex = Regex("(?is)<(?:think|thinking)>(.*?)</(?:think|thinking)>")
    val thinkingBlocks =
        blockRegex.findAll(raw)
            .mapNotNull { match ->
                match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
            }
            .toList()

    val visible =
        raw.replace(blockRegex, " ")
            .replace(Regex("(?i)</?(?:think|thinking)>"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    return ThinkingSplitResult(
        visible = visible,
        thinking = thinkingBlocks.joinToString("\n\n").trim()
    )
}
