package com.zionchat.app.ui.screens.chat

import com.zionchat.app.data.Conversation
import com.zionchat.app.data.Message

/**
 * UI State for ChatScreen
 *
 * This sealed interface represents all possible states of the chat screen.
 */
sealed interface ChatUiState {
    val conversations: List<Conversation>
    val currentConversationId: String?
    val messages: List<Message>
    val isStreaming: Boolean
    val error: String?

    /**
     * Initial/loading state
     */
    data class Loading(
        override val conversations: List<Conversation> = emptyList(),
        override val currentConversationId: String? = null,
        override val messages: List<Message> = emptyList(),
        override val isStreaming: Boolean = false,
        override val error: String? = null
    ) : ChatUiState

    /**
     * Active chat state
     */
    data class Active(
        override val conversations: List<Conversation> = emptyList(),
        override val currentConversationId: String? = null,
        override val messages: List<Message> = emptyList(),
        override val isStreaming: Boolean = false,
        override val error: String? = null,
        val streamingMessageId: String? = null,
        val streamingContent: String = "",
        val streamingReasoning: String? = null,
        val isThinkingActive: Boolean = false,
        val inputText: String = "",
        val selectedTool: String? = null,
        val showToolMenu: Boolean = false,
        val pendingMessages: List<PendingMessage> = emptyList()
    ) : ChatUiState {
        /**
         * Get the current conversation
         */
        val currentConversation: Conversation?
            get() = conversations.firstOrNull { it.id == currentConversationId }

        /**
         * Get all messages including pending ones
         */
        val allMessages: List<Message>
            get() {
                val dataStoreMessages = messages
                val pendingForConversation = pendingMessages
                    .filter { it.conversationId == currentConversationId }
                    .map { it.message }
                    .filterNot { pendingMsg -> dataStoreMessages.any { it.id == pendingMsg.id } }
                return dataStoreMessages + pendingForConversation
            }
    }

    /**
     * Error state
     */
    data class Error(
        override val conversations: List<Conversation> = emptyList(),
        override val currentConversationId: String? = null,
        override val messages: List<Message> = emptyList(),
        override val isStreaming: Boolean = false,
        val errorMessage: String
    ) : ChatUiState {
        override val error: String? get() = errorMessage
    }
}

/**
 * Extension to check if should auto-scroll
 */
fun ChatUiState.shouldAutoScroll(lastVisibleIndex: Int, totalItems: Int): Boolean {
    if (totalItems == 0) return true
    return lastVisibleIndex >= totalItems - 2
}

/**
 * Extension to get display name
 */
fun ChatUiState.getDisplayName(nickname: String): String {
    return nickname.takeIf { it.isNotBlank() } ?: "User"
}
