package com.zionchat.app.ui.screens.chat

import com.google.gson.JsonElement
import com.google.gson.JsonParser

/**
 * MCP tool call parsing utilities
 */

/**
 * Convert a JsonElement to a Kotlin Any? value
 */
internal fun JsonElement.toKotlinAny(): Any? {
    return when {
        isJsonNull -> null
        isJsonPrimitive -> {
            val prim = asJsonPrimitive
            when {
                prim.isBoolean -> prim.asBoolean
                prim.isNumber -> prim.asNumber.toDouble().let { d ->
                    if (d == d.toLong().toDouble()) d.toLong() else d
                }
                else -> prim.asString
            }
        }
        isJsonArray -> asJsonArray.map { it.toKotlinAny() }
        isJsonObject -> asJsonObject.entrySet().associate { it.key to it.value.toKotlinAny() }
        else -> toString()
    }
}

/**
 * Parse MCP tool calls payload from text
 */
internal fun parseMcpToolCallsPayload(text: String): List<PlannedMcpToolCall> {
    val cleaned = stripMarkdownCodeFences(text).trim()
    if (cleaned.isBlank()) return emptyList()

    val candidate = extractFirstJsonCandidate(cleaned)
    val root = candidate?.let { raw -> runCatching { JsonParser.parseString(raw) }.getOrNull() }
    if (root == null) {
        return parseLooseMcpToolCall(cleaned)?.let { listOf(it) } ?: emptyList()
    }

    val callsElement =
        when {
            root.isJsonObject && root.asJsonObject.has("calls") -> root.asJsonObject.get("calls")
            root.isJsonObject && root.asJsonObject.has("mcp_call") -> root.asJsonObject.get("mcp_call")
            root.isJsonObject && root.asJsonObject.has("tool_call") -> root.asJsonObject.get("tool_call")
            root.isJsonObject && root.asJsonObject.has("call") -> root.asJsonObject.get("call")
            root.isJsonObject && root.asJsonObject.has("toolCall") -> root.asJsonObject.get("toolCall")
            root.isJsonObject && root.asJsonObject.has("mcpCall") -> root.asJsonObject.get("mcpCall")
            else -> root
        }

    val callsArray =
        when {
            callsElement.isJsonArray -> callsElement.asJsonArray
            callsElement.isJsonObject -> com.google.gson.JsonArray().apply { add(callsElement) }
            else -> return emptyList()
        }

    fun getString(obj: com.google.gson.JsonObject, vararg keys: String): String? {
        return keys.asSequence().mapNotNull { key ->
            val el = runCatching { obj.get(key) }.getOrNull() ?: return@mapNotNull null
            if (!el.isJsonPrimitive) return@mapNotNull null
            val value = runCatching { el.asString }.getOrNull()?.trim().orEmpty()
            value.takeIf { it.isNotBlank() }
        }.firstOrNull()
    }

    fun getArgsObject(obj: com.google.gson.JsonObject): com.google.gson.JsonObject {
        fun fromKey(key: String): com.google.gson.JsonObject? {
            val el = runCatching { obj.get(key) }.getOrNull() ?: return null
            if (el.isJsonObject) return el.asJsonObject
            if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
                val raw = el.asString.trim()
                if (raw.isBlank()) return null
                return runCatching {
                    JsonParser.parseString(raw)
                        .takeIf { it.isJsonObject }
                        ?.asJsonObject
                }.getOrNull()
            }
            return null
        }
        return fromKey("arguments")
            ?: fromKey("args")
            ?: fromKey("input")
            ?: fromKey("params")
            ?: fromKey("parameters")
            ?: com.google.gson.JsonObject()
    }

    fun normalizeCallObject(obj: com.google.gson.JsonObject): com.google.gson.JsonObject {
        val nested =
            listOf("mcp_call", "tool_call", "call", "toolCall", "mcpCall")
                .asSequence()
                .mapNotNull { key ->
                    runCatching { obj.get(key) }.getOrNull()
                        ?.takeIf { it.isJsonObject }
                        ?.asJsonObject
                }
                .firstOrNull()
        return nested ?: obj
    }

    val strictParsed = callsArray.mapNotNull { el ->
        val rawObj = runCatching { el.asJsonObject }.getOrNull() ?: return@mapNotNull null
        val obj = normalizeCallObject(rawObj)
        val toolName = getString(obj, "toolName", "tool_name", "tool", "name").orEmpty()
        if (toolName.isBlank()) return@mapNotNull null

        val serverId = getString(obj, "serverId", "server_id", "server", "mcpId", "mcp_id", "id").orEmpty()
        val args = getArgsObject(obj).entrySet().associate { entry ->
            entry.key to entry.value.toKotlinAny()
        }

        PlannedMcpToolCall(serverId = serverId, toolName = toolName, arguments = args)
    }
    if (strictParsed.isNotEmpty()) return strictParsed
    return parseLooseMcpToolCall(cleaned)?.let { listOf(it) } ?: emptyList()
}

/**
 * Parse a loose MCP tool call from text (fallback parsing)
 */
internal fun parseLooseMcpToolCall(text: String): PlannedMcpToolCall? {
    val toolName =
        extractQuotedField(text, listOf("toolName", "tool_name", "tool", "name"))
            .orEmpty()
            .trim()
    if (toolName.isBlank()) return null

    val serverId =
        extractQuotedField(text, listOf("serverId", "server_id", "server", "mcpId", "mcp_id", "id"))
            .orEmpty()
            .trim()

    val argsFromJson =
        extractObjectField(text, listOf("arguments", "args", "input", "params", "parameters"))
            ?.let { raw ->
                runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull()
            }
            ?.entrySet()
            ?.associate { entry -> entry.key to entry.value.toKotlinAny() }
            .orEmpty()
            .toMutableMap()

    if (argsFromJson.isEmpty()) {
        extractQuotedField(text, listOf("arguments", "args", "input", "params", "parameters"))
            ?.takeIf { it.startsWith("{") && it.endsWith("}") }
            ?.let { raw ->
                runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull()
            }
            ?.entrySet()
            ?.forEach { entry -> argsFromJson[entry.key] = entry.value.toKotlinAny() }
    }

    if (argsFromJson.isEmpty()) {
        extractQuotedField(text, listOf("value"))?.takeIf { it.isNotBlank() }?.let { value ->
            argsFromJson["value"] = value
        }
    }

    return PlannedMcpToolCall(
        serverId = serverId,
        toolName = toolName,
        arguments = argsFromJson
    )
}

/**
 * Build a unique signature for an MCP call (for deduplication)
 */
internal fun buildMcpCallSignature(call: PlannedMcpToolCall): String {
    return buildString {
        append(call.serverId.trim())
        append("|")
        append(call.toolName.trim())
        append("|")
        append(call.arguments.keys.sorted().joinToString(","))
    }
}

/**
 * Extract explicit API error from response
 */
internal fun extractExplicitApiError(text: String): String? {
    if (text.isBlank()) return null
    val patterns = listOf(
        Regex("(?i)error[:\\s]+(.{8,200})", setOf(RegexOption.MULTILINE)),
        Regex("(?i)failed[:\\s]+(.{8,200})", setOf(RegexOption.MULTILINE)),
        Regex("(?i)exception[:\\s]+(.{8,200})", setOf(RegexOption.MULTILINE))
    )
    for (pattern in patterns) {
        val match = pattern.find(text) ?: continue
        val captured = match.groupValues.getOrNull(1)?.trim() ?: continue
        if (captured.isNotBlank()) return captured.take(180)
    }
    return null
}

/**
 * Convert throwable to MCP failure result
 */
internal fun toMcpFailureResult(e: Throwable): McpCallResult {
    return McpCallResult(
        success = false,
        content = "",
        error = e.message?.trim().orEmpty().ifBlank { e.toString() }
    )
}

/**
 * Result of an MCP tool call
 */
internal data class McpCallResult(
    val success: Boolean,
    val content: String,
    val error: String? = null
)

/**
 * Check if a tag is in running state
 */
internal fun isTagRunning(tag: com.zionchat.app.data.MessageTag): Boolean {
    return tag.status == "running" || tag.status == "pending"
}
