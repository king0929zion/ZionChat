package com.zionchat.app.ui.screens.chat

import com.zionchat.app.data.AppAutomationTask
import com.zionchat.app.data.McpConfig
import com.zionchat.app.data.SavedApp

/**
 * Prompt building utilities for ChatScreen
 */

/**
 * Build MCP tool call instruction for assistant
 */
internal fun buildMcpToolCallInstruction(
    servers: List<McpConfig>,
    roundIndex: Int,
    maxCallsPerRound: Int
): String {
    val maxToolsPerServer = 24

    return buildString {
        appendLine("You can use MCP tools in this round.")
        appendLine("Current round: $roundIndex")
        appendLine("First write a normal visible reply for the user.")
        appendLine("If tool calls are needed, append tags AFTER your visible reply:")
        appendLine("<mcp_call>{\"serverId\":\"...\",\"toolName\":\"...\",\"arguments\":{}}</mcp_call>")
        appendLine()
        appendLine("Rules:")
        appendLine("- If no tool is needed, do not output any mcp_call tags.")
        appendLine("- At most $maxCallsPerRound tool calls in this round.")
        appendLine("- toolName must match exactly from the list below.")
        appendLine("- arguments must be a JSON object.")
        appendLine("- Do not mention planning, routers, or internal logic.")
        appendLine()
        appendLine("Available MCP servers and tools:")

        servers.forEach { server ->
            append("- Server: ")
            append(server.name.trim().ifBlank { "Unnamed" })
            append(" (id=")
            append(server.id)
            appendLine(")")

            server.tools.take(maxToolsPerServer).forEach { tool ->
                append("  - Tool: ")
                appendLine(tool.name)
                val desc = tool.description.trim()
                if (desc.isNotBlank()) {
                    append("    Description: ")
                    appendLine(desc.take(240))
                }
                if (tool.parameters.isNotEmpty()) {
                    appendLine("    Parameters:")
                    tool.parameters.forEach { param ->
                        append("      - ")
                        append(param.name)
                        append(" (")
                        append(param.type)
                        append(")")
                        if (param.required) append(" [required]")
                        val pDesc = param.description.trim()
                        if (pDesc.isNotBlank()) {
                            append(": ")
                            append(pDesc.take(180))
                        }
                        appendLine()
                    }
                }
            }
        }
    }.trim()
}

/**
 * Build MCP round result context for next round
 */
internal fun buildMcpRoundResultContext(roundIndex: Int, summary: String): String {
    val cleaned = summary.trim().ifBlank { "- No tool output." }
    return buildString {
        append("MCP tool results from round ")
        append(roundIndex)
        appendLine(":")
        appendLine(cleaned.take(5000))
        appendLine()
        appendLine("Use these results to continue the same user request.")
        appendLine("If more data is needed, you may output new <mcp_call> tags.")
    }.trim()
}

/**
 * Build app developer tool instruction
 */
internal fun buildAppDeveloperToolInstruction(
    roundIndex: Int,
    maxCallsPerRound: Int,
    savedApps: List<SavedApp>
): String {
    return buildString {
        appendLine("Built-in tool available: app_developer.")
        appendLine("Current round: $roundIndex")
        appendLine("Use this tool ONLY when the user explicitly asks to create/build/develop an app or HTML page.")
        appendLine("Do not call this tool for generic Q&A.")
        appendLine("First write a normal visible reply, then append tool call tags if needed.")
        appendLine("At most $maxCallsPerRound tool calls in this round.")
        appendLine()
        appendLine("Saved apps context:")
        appendLine(summarizeSavedAppsForInstruction(savedApps))
        appendLine()
        appendLine("Tool call format:")
        appendLine("<mcp_call>{\"serverId\":\"builtin_app_developer\",\"toolName\":\"app_developer\",\"arguments\":{...}}</mcp_call>")
        appendLine()
        appendLine("For create mode, required arguments:")
        appendLine("- mode: \"create\" (or omit)")
        appendLine("- name: app name in ONE language only (Chinese OR English), never bilingual")
        appendLine("- description: concise description (one short sentence, no filler)")
        appendLine("- style: visual style direction")
        appendLine("- features: array of detailed functional requirements")
        appendLine("- All declared UI interactions must be real and fully functional (no dead buttons/links).")
        appendLine("- If style mentions iOS/iPhone/Cupertino/iOS 18, enforce strict iOS 18 HIG constraints.")
        appendLine()
        appendLine("For edit mode, required arguments:")
        appendLine("- mode: \"edit\"")
        appendLine("- targetAppId or targetAppName: choose from saved apps context above")
        appendLine("- editRequest: clear and detailed modification request")
        appendLine("- Keep existing working behavior unless explicitly changed.")
        appendLine("- Never ship simulated-only functionality.")
    }.trim()
}

/**
 * Build pending app automation prompt
 */
internal fun buildPendingAppAutomationPrompt(task: AppAutomationTask): String {
    val request = task.request.trim()
    val html = task.appHtml.trim()
    if (request.isBlank() || html.isBlank()) return ""
    val mode = task.mode.trim().lowercase()
    val appName = normalizeAppDisplayName(task.appName.trim().ifBlank { "App" })
    val appId = task.appId.trim().ifBlank { "unknown-app-id" }
    val htmlPayload = html.take(180_000)

    return buildString {
        if (mode == "debug_fix") {
            appendLine("Please fix an existing saved HTML app.")
        } else {
            appendLine("Please edit an existing saved HTML app.")
        }
        appendLine("Do NOT create a new app. Update the target app in-place.")
        appendLine("Use app_developer tool in edit mode.")
        appendLine()
        appendLine("Required tool arguments:")
        appendLine("- mode: \"edit\"")
        append("- targetAppId: \"")
        append(appId)
        appendLine("\"")
        append("- targetAppName: \"")
        append(appName)
        appendLine("\"")
        append("- name: one-language app name only (Chinese OR English)")
        appendLine("- description: one short sentence")
        appendLine("- style: keep existing style unless request changes style")
        appendLine("- features: list only real, working functionality")
        appendLine("- editRequest: include the request below")
        appendLine()
        appendLine(if (mode == "debug_fix") "Bug report:" else "Edit request:")
        appendLine(request)
        appendLine()
        appendLine("Hard constraints:")
        appendLine("- No mock/placeholder/simulated-only interactions.")
        appendLine("- Every button/link/tab/form control must have real behavior.")
        appendLine("- Preserve unaffected working modules.")
        appendLine("- Return one full updated HTML document.")
        appendLine()
        appendLine("Current app HTML:")
        appendLine(htmlPayload)
    }.trim()
}

/**
 * Format elapsed duration in human-readable form
 */
internal fun formatElapsedDuration(ms: Long): String {
    val seconds = ms / 1000.0
    return when {
        seconds < 1 -> "${ms}ms"
        seconds < 60 -> String.format("%.1fs", seconds)
        else -> {
            val mins = (seconds / 60).toInt()
            val secs = (seconds % 60).toInt()
            "${mins}m ${secs}s"
        }
    }
}
