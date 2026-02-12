package com.zionchat.app.ui.screens.chat

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zionchat.app.data.AppRepository
import com.zionchat.app.data.ChatApiClient
import com.zionchat.app.data.Conversation
import com.zionchat.app.data.Message
import com.zionchat.app.data.McpClient
import com.zionchat.app.data.ProviderAuthManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for ChatScreen that manages UI state and business logic.
 *
 * This ViewModel handles:
 * - Conversation management (create, select, delete)
 * - Message handling (send, receive, stream)
 * - MCP tool calls
 * - App builder integration
 * - UI state management
 */
class ChatViewModel(
    private val application: Application,
    private val repository: AppRepository,
    private val chatApiClient: ChatApiClient,
    private val providerAuthManager: ProviderAuthManager
) : ViewModel() {

    private val mcpClient = McpClient()

    // UI State
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Internal state
    private var streamingJob: Job? = null
    private var stopRequestedByUser = false
    private var preferredConversationId: String? = null
    private var preferredConversationSetAtMs = 0L

    // Image attachments
    private val _imageAttachments = MutableStateFlow<List<PendingImageAttachment>>(emptyList())
    val imageAttachments: StateFlow<List<PendingImageAttachment>> = _imageAttachments.asStateFlow()

    init {
        loadInitialState()
    }

    /**
     * Load initial state from repository
     */
    private fun loadInitialState() {
        viewModelScope.launch {
            val conversations = repository.conversationsFlow.first()
            val currentId = repository.currentConversationIdFlow.first()

            _uiState.value = ChatUiState.Active(
                conversations = conversations,
                currentConversationId = currentId ?: conversations.firstOrNull()?.id,
                messages = emptyList()
            )

            // Collect ongoing changes
            launch {
                repository.conversationsFlow.collect { convos ->
                    updateState { copy(conversations = convos) }
                    updateCurrentMessages()
                }
            }

            launch {
                repository.currentConversationIdFlow.collect { id ->
                    if (id != null) {
                        preferredConversationId = id
                        preferredConversationSetAtMs = System.currentTimeMillis()
                        updateState { copy(currentConversationId = id) }
                        updateCurrentMessages()
                    }
                }
            }
        }
    }

    /**
     * Update messages for current conversation
     */
    private fun updateCurrentMessages() {
        val state = _uiState.value as? ChatUiState.Active ?: return
        val conversation = state.conversations.firstOrNull { it.id == state.currentConversationId }
        updateState { copy(messages = conversation?.messages.orEmpty()) }
    }

    /**
     * Update state helper
     */
    private inline fun updateState(update: ChatUiState.Active.() -> ChatUiState.Active) {
        val current = _uiState.value
        if (current is ChatUiState.Active) {
            _uiState.value = current.update()
        }
    }

    /**
     * Get effective conversation ID (preferred or from state)
     */
    fun getEffectiveConversationId(): String? {
        val state = _uiState.value as? ChatUiState.Active ?: return null
        val preferred = preferredConversationId?.trim().takeIf { !it.isNullOrBlank() }
        val fromStore = state.currentConversationId?.trim().takeIf { !it.isNullOrBlank() }

        val conversations = state.conversations
        val now = System.currentTimeMillis()

        return when {
            preferred != null -> {
                val inList = conversations.any { it.id == preferred }
                val withinGrace = now - preferredConversationSetAtMs < 2500
                when {
                    inList || withinGrace -> preferred
                    !fromStore.isNullOrBlank() -> fromStore
                    else -> conversations.firstOrNull()?.id
                }
            }
            !fromStore.isNullOrBlank() -> fromStore
            else -> conversations.firstOrNull()?.id
        }
    }

    /**
     * Create a new conversation
     */
    fun createNewConversation() {
        viewModelScope.launch {
            val created = repository.createConversation()
            preferredConversationId = created.id
            preferredConversationSetAtMs = System.currentTimeMillis()
            repository.setCurrentConversationId(created.id)
            updateState {
                copy(
                    currentConversationId = created.id,
                    selectedTool = null,
                    inputText = ""
                )
            }
        }
    }

    /**
     * Select a conversation
     */
    fun selectConversation(conversationId: String) {
        viewModelScope.launch {
            preferredConversationId = conversationId
            preferredConversationSetAtMs = System.currentTimeMillis()
            repository.setCurrentConversationId(conversationId)
            updateState { copy(currentConversationId = conversationId) }
        }
    }

    /**
     * Delete a conversation
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            if (preferredConversationId == conversationId) {
                preferredConversationId = null
            }
        }
    }

    /**
     * Update input text
     */
    fun updateInputText(text: String) {
        updateState { copy(inputText = text) }
    }

    /**
     * Toggle tool menu
     */
    fun toggleToolMenu(show: Boolean) {
        updateState { copy(showToolMenu = show) }
    }

    /**
     * Select tool
     */
    fun selectTool(tool: String?) {
        updateState { copy(selectedTool = tool, showToolMenu = false) }
    }

    /**
     * Add image attachment
     */
    fun addImageAttachment(uri: Uri? = null, bitmap: Bitmap? = null) {
        _imageAttachments.update { it + PendingImageAttachment(uri, bitmap) }
    }

    /**
     * Remove image attachment
     */
    fun removeImageAttachment(index: Int) {
        _imageAttachments.update { current ->
            if (index < 0 || index >= current.size) return@update current
            current.toMutableList().apply { removeAt(index) }
        }
    }

    /**
     * Clear all image attachments
     */
    fun clearImageAttachments() {
        _imageAttachments.value = emptyList()
    }

    /**
     * Stop streaming
     */
    fun stopStreaming() {
        val state = _uiState.value as? ChatUiState.Active ?: return
        if (!state.isStreaming) return
        stopRequestedByUser = true
        streamingJob?.cancel()
    }

    /**
     * Add pending message
     */
    fun addPendingMessage(conversationId: String, message: Message) {
        updateState {
            copy(pendingMessages = pendingMessages + PendingMessage(conversationId, message))
        }
    }

    /**
     * Remove pending message
     */
    fun removePendingMessage(conversationId: String, messageId: String) {
        updateState {
            copy(
                pendingMessages = pendingMessages.filterNot {
                    it.conversationId == conversationId && it.message.id == messageId
                }
            )
        }
    }

    /**
     * Update streaming state
     */
    fun updateStreamingState(
        isStreaming: Boolean,
        messageId: String? = null,
        content: String = "",
        reasoning: String? = null,
        isThinkingActive: Boolean = false
    ) {
        updateState {
            copy(
                isStreaming = isStreaming,
                streamingMessageId = messageId,
                streamingContent = content,
                streamingReasoning = reasoning,
                isThinkingActive = isThinkingActive
            )
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        updateState { copy(error = null) }
    }

    /**
     * Factory for creating ChatViewModel with dependencies
     */
    class Factory(
        private val application: Application,
        private val repository: AppRepository,
        private val chatApiClient: ChatApiClient,
        private val providerAuthManager: ProviderAuthManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(application, repository, chatApiClient, providerAuthManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
