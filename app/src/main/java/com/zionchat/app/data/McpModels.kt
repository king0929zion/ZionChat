package com.zionchat.app.data

import java.util.UUID

/**
 * MCP (Model Context Protocol) Configuration
 */
data class McpConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val protocol: McpProtocol = McpProtocol.HTTP,
    val enabled: Boolean = true,
    val description: String = "",
    val headers: List<HttpHeader> = emptyList(),
    val tools: List<McpTool> = emptyList(),
    val lastSyncAt: Long = 0L
)

enum class McpProtocol {
    HTTP,
    SSE
}

/**
 * MCP Tool definition
 */
data class McpTool(
    val name: String,
    val description: String,
    val parameters: List<McpToolParameter> = emptyList()
)

/**
 * MCP Tool Parameter
 */
data class McpToolParameter(
    val name: String,
    val type: String,
    val required: Boolean = true,
    val description: String = ""
)

/**
 * MCP Tool Call Request
 */
data class McpToolCall(
    val toolName: String,
    val arguments: Map<String, Any>
)

/**
 * MCP Tool Call Result
 */
data class McpToolResult(
    val success: Boolean,
    val content: String,
    val error: String? = null
)

/**
 * MCP Server Info Response
 */
data class McpServerInfo(
    val name: String,
    val version: String,
    val tools: List<McpTool>
)
