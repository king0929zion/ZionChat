package com.zionchat.app.ui.screens.chat

import android.net.Uri
import android.graphics.Bitmap
import com.zionchat.app.data.Message

/**
 * Data classes for ChatScreen internal state management
 */

/**
 * Represents a pending message that hasn't been persisted to DataStore yet
 */
internal data class PendingMessage(val conversationId: String, val message: Message)

/**
 * Represents a pending image attachment in the input area
 */
internal data class PendingImageAttachment(val uri: Uri? = null, val bitmap: Bitmap? = null)

/**
 * Tool specification for app developer functionality
 */
internal data class AppDevToolSpec(
    val mode: String,
    val name: String,
    val description: String,
    val style: String,
    val features: List<String>,
    val targetAppId: String? = null,
    val targetAppName: String? = null,
    val editRequest: String? = null
)

/**
 * Payload for app development tag display
 */
internal data class AppDevTagPayload(
    val name: String,
    val subtitle: String,
    val description: String,
    val style: String,
    val features: List<String>,
    val progress: Int,
    val status: String,
    val html: String,
    val error: String? = null,
    val sourceAppId: String? = null,
    val mode: String = "create"
)

/**
 * Planned MCP tool call parsed from assistant response
 */
internal data class PlannedMcpToolCall(
    val serverId: String,
    val toolName: String,
    val arguments: Map<String, Any?>
)

/**
 * Anchor point for MCP tag insertion in content
 */
internal data class McpTagAnchor(
    val tagId: String,
    val charIndex: Int
)

/**
 * Result of extracting inline MCP call blocks from content
 */
internal data class InlineMcpCallExtraction(
    val visibleText: String,
    val blocks: List<String>
)

/**
 * Segment of message content - either text or tag reference
 */
internal data class MessageInlineSegment(
    val text: String? = null,
    val tagId: String? = null
)

/**
 * Result of splitting thinking blocks from visible content
 */
internal data class ThinkingSplitResult(
    val visible: String,
    val thinking: String
)
