package com.zionchat.app.ui.screens.chat

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.zionchat.app.data.SavedApp

/**
 * App Development utilities for ChatScreen
 */

/**
 * Check if a character is a Han (Chinese) character
 */
internal fun isHanChar(ch: Char): Boolean {
    return Character.UnicodeScript.of(ch.code) == Character.UnicodeScript.HAN
}

/**
 * Normalize app display name to use single language
 */
internal fun normalizeAppDisplayName(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return "App"

    val hasHan = trimmed.any(::isHanChar)
    val hasLatin = trimmed.any { it in 'A'..'Z' || it in 'a'..'z' }
    if (!hasHan || !hasLatin) {
        return trimmed.take(80)
    }

    val separators = listOf("/", "|", "｜", " - ", " — ", " – ", "·")
    separators.forEach { sep ->
        val parts = trimmed.split(sep).map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size < 2) return@forEach
        val pureHan = parts.firstOrNull { p -> p.any(::isHanChar) && p.none { it in 'A'..'Z' || it in 'a'..'z' } }
        val pureLatin = parts.firstOrNull { p -> p.any { it in 'A'..'Z' || it in 'a'..'z' } && p.none(::isHanChar) }
        val chosen = pureHan ?: pureLatin ?: parts.first()
        return chosen.take(80)
    }

    val hanCount = trimmed.count(::isHanChar)
    val latinCount = trimmed.count { it in 'A'..'Z' || it in 'a'..'z' }
    val keepHan = hanCount >= latinCount
    val filtered =
        buildString {
            trimmed.forEach { ch ->
                val keep =
                    if (keepHan) {
                        isHanChar(ch) || ch.isDigit() || ch.isWhitespace() || ch in setOf('-', '_')
                    } else {
                        (ch in 'A'..'Z') || (ch in 'a'..'z') || ch.isDigit() || ch.isWhitespace() || ch in setOf('-', '_')
                    }
                if (keep) append(ch)
            }
        }.trim()
    return filtered.ifBlank { trimmed }.take(80)
}

/**
 * Compact app description to a short form
 */
internal fun compactAppDescription(primary: String, fallback: String = "Developing app"): String {
    val raw =
        primary
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[\\r\\n]+"), " ")
            .trim()
    val simplified = raw.trimEnd('.', '。', '!', '！', '?', '？')
    if (simplified.isBlank()) {
        return fallback.trim().ifBlank { "Developing app" }.take(56)
    }
    return simplified.take(56)
}

/**
 * Check if prompt should enable app builder
 */
internal fun shouldEnableAppBuilderForPrompt(userPrompt: String): Boolean {
    val text = userPrompt.trim().lowercase()
    if (text.isBlank()) return false
    val patterns = listOf(
        Regex("(?i)\\b(create|build|develop|make)\\b.{0,24}\\b(app|website|web app|html app|landing page)\\b"),
        Regex("(?i)\\b(edit|update|revise|modify|refactor|improve)\\b.{0,24}\\b(app|website|web app|html app|page|saved app)\\b"),
        Regex("(?i)\\b(html app|web app|single page app|landing page)\\b"),
        Regex("创建.{0,10}(应用|app|网页|网站|页面)"),
        Regex("开发.{0,10}(应用|app|网页|网站|页面)"),
        Regex("做(一个|个)?.{0,8}(应用|app|网页|网站|页面)"),
        Regex("(修改|编辑|更新|优化|重构).{0,12}(应用|app|网页|网站|页面)")
    )
    return patterns.any { it.containsMatchIn(text) }
}

/**
 * Check if a call is the built-in app developer
 */
internal fun isBuiltInAppDeveloperCall(call: PlannedMcpToolCall): Boolean {
    val server = call.serverId.trim().lowercase()
    val tool = call.toolName.trim().lowercase()
    return tool in setOf(
        "app_developer",
        "app_builder",
        "build_html_app",
        "develop_html_app"
    ) || server in setOf(
        "builtin_app_developer",
        "app_builder",
        "internal_app_developer"
    )
}

/**
 * Parse app developer tool spec from arguments
 */
internal fun parseAppDevToolSpec(arguments: Map<String, Any?>): AppDevToolSpec? {
    fun anyString(vararg keys: String): String {
        return keys.asSequence()
            .mapNotNull { key ->
                val value = arguments.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value ?: return@mapNotNull null
                when (value) {
                    is String -> value.trim().takeIf { it.isNotBlank() }
                    else -> value?.toString()?.trim()?.takeIf { it.isNotBlank() }
                }
            }
            .firstOrNull()
            .orEmpty()
    }

    fun anyStringList(vararg keys: String): List<String> {
        val rawValue =
            keys.asSequence()
                .mapNotNull { key -> arguments.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value }
                .firstOrNull()
                ?: return emptyList()
        return when (rawValue) {
            is List<*> -> rawValue.mapNotNull { it?.toString()?.trim()?.takeIf { t -> t.isNotBlank() } }
            is String -> rawValue
                .split('\n', ',', ';', '|')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            else -> emptyList()
        }.distinct().take(12)
    }

    val modeRaw = anyString("mode", "operation", "intent", "action")
    val mode =
        when (modeRaw.trim().lowercase()) {
            "edit", "update", "revise", "modify", "refactor", "patch" -> "edit"
            else -> "create"
        }
    val name = normalizeAppDisplayName(anyString("name", "title", "appName", "app_name"))
    val description = anyString("description", "desc", "summary")
    val style = anyString("style", "theme", "visualStyle", "design")
    val targetAppId = anyString("targetAppId", "target_app_id", "appId", "existingAppId", "sourceAppId")
    val targetAppName = anyString("targetAppName", "target_app_name", "existingAppName", "appToEdit", "editAppName")
    val editRequest =
        anyString("editRequest", "request", "updateRequest", "changeRequest", "instruction", "prompt")
            .ifBlank { anyString("details", "detail", "scope", "requirement") }
    val features =
        anyStringList("features", "requirements", "specs", "functionalities")
            .ifEmpty {
                anyString("details", "detail", "scope", "requirement")
                    .takeIf { it.isNotBlank() }
                    ?.split('\n', ',', ';', '|')
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
            }

    if (mode == "edit") {
        if (editRequest.isBlank()) return null
        return AppDevToolSpec(
            mode = mode,
            name = name.take(80),
            description = description.take(260),
            style = style.take(120),
            features = features.take(10),
            targetAppId = targetAppId.takeIf { it.isNotBlank() }?.take(120),
            targetAppName = targetAppName.takeIf { it.isNotBlank() }?.take(80),
            editRequest = editRequest.take(800)
        )
    }

    if (name.isBlank() || description.isBlank() || style.isBlank() || features.isEmpty()) return null
    return AppDevToolSpec(
        mode = mode,
        name = normalizeAppDisplayName(name).take(80),
        description = description.take(260),
        style = style.take(120),
        features = features.take(10),
        targetAppId = targetAppId.takeIf { it.isNotBlank() }?.take(120),
        targetAppName = targetAppName.takeIf { it.isNotBlank() }?.take(80),
        editRequest = editRequest.takeIf { it.isNotBlank() }?.take(800)
    )
}

/**
 * Encode app dev tag payload to JSON string
 */
internal fun encodeAppDevTagPayload(payload: AppDevTagPayload): String {
    return GsonBuilder().disableHtmlEscaping().create().toJson(payload)
}

/**
 * Parse app dev tag payload from JSON string
 */
internal fun parseAppDevTagPayload(
    content: String,
    fallbackName: String,
    fallbackStatus: String?
): AppDevTagPayload {
    val json = runCatching { JsonParser.parseString(content).asJsonObject }.getOrNull()
    val name =
        json?.get("name")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: fallbackName
    val subtitleRaw =
        json?.get("subtitle")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Developing app"
    val descriptionRaw = json?.get("description")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
    val style = json?.get("style")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
    val features =
        json?.getAsJsonArray("features")
            ?.mapNotNull { runCatching { it.asString.trim() }.getOrNull()?.takeIf { t -> t.isNotBlank() } }
            .orEmpty()
    val progress = json?.get("progress")?.takeIf { it.isJsonPrimitive }?.asInt ?: if (fallbackStatus == "success") 100 else 0
    val status = json?.get("status")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty().ifBlank { fallbackStatus.orEmpty() }
    val html = json?.get("html")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
    val error = json?.get("error")?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotBlank() }
    val sourceAppId = json?.get("sourceAppId")?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotBlank() }
    val mode = json?.get("mode")?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotBlank() } ?: "create"
    val compactDescription = compactAppDescription(descriptionRaw, subtitleRaw)
    val compactSubtitle = compactAppDescription(subtitleRaw, compactDescription)
    return AppDevTagPayload(
        name = normalizeAppDisplayName(name),
        subtitle = compactSubtitle,
        description = compactDescription,
        style = style,
        features = features,
        progress = progress.coerceIn(0, 100),
        status = status,
        html = html,
        error = error,
        sourceAppId = sourceAppId,
        mode = mode
    )
}

/**
 * Resolve existing saved app for edit mode
 */
internal fun resolveExistingSavedApp(
    spec: AppDevToolSpec,
    savedApps: List<SavedApp>
): SavedApp? {
    if (savedApps.isEmpty()) return null
    if (spec.mode != "edit") return null

    val byId = spec.targetAppId?.trim().orEmpty()
    if (byId.isNotBlank()) {
        if (byId.equals("latest", ignoreCase = true) || byId.equals("last", ignoreCase = true)) {
            return savedApps.maxByOrNull { it.updatedAt }
        }
        savedApps.firstOrNull { it.id.equals(byId, ignoreCase = true) }?.let { return it }
    }

    val candidates = mutableListOf<String>()
    spec.targetAppName?.trim()?.takeIf { it.isNotBlank() }?.let { candidates += it }
    spec.name.trim().takeIf { it.isNotBlank() && !it.equals("app", ignoreCase = true) }?.let { candidates += it }

    candidates.forEach { keyword ->
        savedApps.firstOrNull { it.name.trim().equals(keyword, ignoreCase = true) }?.let { return it }
    }
    candidates.forEach { keyword ->
        val lower = keyword.lowercase()
        val byContain = savedApps.filter { it.name.lowercase().contains(lower) }
        if (byContain.size == 1) return byContain.first()
    }

    return if (savedApps.size == 1) savedApps.first() else null
}

/**
 * Summarize saved apps for instruction context
 */
internal fun summarizeSavedAppsForInstruction(savedApps: List<SavedApp>, limit: Int = 10): String {
    if (savedApps.isEmpty()) return "No saved apps."
    return buildString {
        savedApps
            .sortedByDescending { it.updatedAt }
            .take(limit)
            .forEachIndexed { index, app ->
                append(index + 1)
                append(". id=")
                append(app.id)
                append(" | name=")
                append(normalizeAppDisplayName(app.name).take(80))
                val desc = compactAppDescription(app.description, "")
                if (desc.isNotBlank()) {
                    append(" | desc=")
                    append(desc.take(80))
                }
                append('\n')
            }
    }.trimEnd()
}

/**
 * Check if iOS 18 UI skill should be used
 */
internal fun shouldUseIos18UiSkill(
    style: String,
    description: String,
    features: List<String>,
    requestText: String = ""
): Boolean {
    val signal =
        buildString {
            append(style)
            append('\n')
            append(description)
            append('\n')
            append(features.joinToString(" "))
            append('\n')
            append(requestText)
        }.lowercase()
    if (signal.isBlank()) return false

    val markers =
        listOf(
            "ios",
            "ios18",
            "ios 18",
            "iphone",
            "ipad",
            "cupertino",
            "apple",
            "human interface",
            "sf pro",
            "inset grouped",
            "tab bar",
            "large title"
        )
    return markers.any { marker -> signal.contains(marker) }
}
