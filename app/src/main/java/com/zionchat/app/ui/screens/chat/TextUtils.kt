package com.zionchat.app.ui.screens.chat

import com.google.gson.JsonParser
import com.zionchat.app.data.Message

/**
 * Text processing utilities for ChatScreen
 */

/**
 * Append a text section to a StringBuilder with proper spacing
 */
internal fun appendTextSection(target: StringBuilder, text: String) {
    val cleaned = text.trim()
    if (cleaned.isBlank()) return
    if (target.isNotEmpty()) target.append("\n\n")
    target.append(cleaned)
}

/**
 * Merge two text sections with proper spacing
 */
internal fun mergeTextSections(existing: String, incoming: String): String {
    val left = existing.trim()
    val right = incoming.trim()
    return when {
        left.isBlank() -> right
        right.isBlank() -> left
        else -> "$left\n\n$right"
    }
}

/**
 * Strip markdown code fences from text
 */
internal fun stripMarkdownCodeFences(text: String): String {
    var t = text.trim()
    if (t.startsWith("```")) {
        t = t.substringAfter('\n', t).trim()
    }
    if (t.endsWith("```")) {
        t = t.dropLast(3).trim()
    }
    return t.trim()
}

/**
 * Parse a JSON string array from text
 */
internal fun parseJsonStringArray(text: String): List<String> {
    val cleaned = stripMarkdownCodeFences(text).trim()
    if (cleaned.isBlank()) return emptyList()

    val candidate = run {
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
    }

    val element = runCatching { JsonParser.parseString(candidate) }.getOrNull() ?: return emptyList()
    val array =
        when {
            element.isJsonArray -> element.asJsonArray
            element.isJsonObject -> element.asJsonObject.getAsJsonArray("memories")
            else -> null
        } ?: return emptyList()

    return array.mapNotNull { el ->
        runCatching { el.asString }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }
}

/**
 * Build a conversation transcript from messages
 */
internal fun buildConversationTranscript(
    messages: List<Message>,
    maxMessages: Int,
    maxCharsPerMessage: Int
): String {
    if (messages.isEmpty()) return ""
    val tail = messages.takeLast(maxMessages)
    return buildString {
        tail.forEach { msg ->
            val prefix =
                when (msg.role) {
                    "user" -> "User"
                    "assistant" -> "Assistant"
                    else -> msg.role.replaceFirstChar { it.uppercase() }
                }
            append(prefix)
            append(": ")
            val cleanContent = stripMcpTagMarkers(msg.content)
            append(cleanContent.replace('\n', ' ').trim().take(maxCharsPerMessage))
            append('\n')
        }
    }.trim()
}

/**
 * Extract the first JSON candidate (object or array) from text
 */
internal fun extractFirstJsonCandidate(text: String): String? {
    val raw = text.trim()
    if (raw.isBlank()) return null

    val idxObj = raw.indexOf('{')
    val idxArr = raw.indexOf('[')
    val start =
        listOf(idxObj, idxArr)
            .filter { it >= 0 }
            .minOrNull()
            ?: return null

    val open = raw[start]
    val close = if (open == '{') '}' else ']'
    val end = raw.lastIndexOf(close)
    if (end <= start) return null

    return raw.substring(start, end + 1)
}

/**
 * Extract a quoted field from text by key names
 */
internal fun extractQuotedField(text: String, keys: List<String>): String? {
    keys.forEach { key ->
        val escapedKey = Regex.escape(key)
        val quotedRegex = Regex("\"$escapedKey\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", setOf(RegexOption.IGNORE_CASE))
        val looseQuotedRegex = Regex("\\b$escapedKey\\b\\s*[:=]\\s*\"((?:\\\\.|[^\"\\\\])*)\"", setOf(RegexOption.IGNORE_CASE))
        val looseSingleQuotedRegex = Regex("\\b$escapedKey\\b\\s*[:=]\\s*'([^']*)'", setOf(RegexOption.IGNORE_CASE))
        val bareRegex = Regex("\\b$escapedKey\\b\\s*[:=]\\s*([^,\\n\\r}\\]]+)", setOf(RegexOption.IGNORE_CASE))

        val value =
            quotedRegex.find(text)?.groupValues?.getOrNull(1)
                ?: looseQuotedRegex.find(text)?.groupValues?.getOrNull(1)
                ?: looseSingleQuotedRegex.find(text)?.groupValues?.getOrNull(1)
                ?: bareRegex.find(text)?.groupValues?.getOrNull(1)

        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return@forEach

        val normalized = raw.removePrefix("\"").removeSuffix("\"").removePrefix("'").removeSuffix("'").trim()
        if (normalized.isBlank()) return@forEach

        val decoded = runCatching { JsonParser.parseString("\"$normalized\"").asString }.getOrDefault(normalized)
        return decoded.trim()
    }
    return null
}

/**
 * Find the matching closing brace for an opening brace at the given position
 */
internal fun findMatchingBraceEnd(text: String, start: Int): Int {
    var depth = 1
    var i = start
    while (i < text.length && depth > 0) {
        val c = text[i]
        when (c) {
            '{' -> depth++
            '}' -> depth--
        }
        i++
    }
    return if (depth == 0) i - 1 else -1
}

/**
 * Extract an object field from text by key names
 */
internal fun extractObjectField(text: String, keys: List<String>): String? {
    keys.forEach { key ->
        val escapedKey = Regex.escape(key)
        val markerRegex = Regex("(?:\"$escapedKey\"|\\b$escapedKey\\b)\\s*:\\s*\\{", setOf(RegexOption.IGNORE_CASE))
        val marker = markerRegex.find(text) ?: return@forEach
        val start = marker.range.last
        val end = findMatchingBraceEnd(text, start)
        if (start >= 0 && end >= start && end < text.length) {
            return text.substring(start, end + 1)
        }
    }
    return null
}
