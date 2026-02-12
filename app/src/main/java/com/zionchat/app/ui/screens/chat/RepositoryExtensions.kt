package com.zionchat.app.ui.screens.chat

import com.zionchat.app.data.AppRepository
import com.zionchat.app.data.Conversation
import com.zionchat.app.data.Message
import com.zionchat.app.data.MessageTag
import kotlinx.coroutines.flow.first

/**
 * Extension functions for AppRepository to reduce code duplication
 */

/**
 * Generic helper to update a conversation in the list
 */
suspend fun AppRepository.updateConversationInList(
    conversationId: String,
    update: (Conversation) -> Conversation
): Boolean {
    val conversations = conversationsFlow.first().toMutableList()
    val index = conversations.indexOfFirst { it.id == conversationId }
    if (index < 0) return false

    val updated = update(conversations[index])
    conversations.removeAt(index)
    conversations.add(0, updated)
    saveConversations(conversations)
    return true
}

/**
 * Append a message to a conversation and move it to top
 */
suspend fun AppRepository.appendMessageToConversation(
    conversationId: String,
    message: Message
): Boolean {
    return updateConversationInList(conversationId) { conversation ->
        conversation.copy(messages = conversation.messages + message)
    }
}

/**
 * Update a message in a conversation
 */
suspend fun AppRepository.updateMessageInConversation(
    conversationId: String,
    messageId: String,
    update: (Message) -> Message
): Boolean {
    return updateConversationInList(conversationId) { conversation ->
        conversation.copy(
            messages = conversation.messages.map { msg ->
                if (msg.id == messageId) update(msg) else msg
            }
        )
    }
}

/**
 * Delete a message from a conversation
 */
suspend fun AppRepository.deleteMessageFromConversation(
    conversationId: String,
    messageId: String
): Boolean {
    return updateConversationInList(conversationId) { conversation ->
        conversation.copy(messages = conversation.messages.filter { it.id != messageId })
    }
}

/**
 * Add a tag to a message
 */
suspend fun AppRepository.addTagToMessage(
    conversationId: String,
    messageId: String,
    tag: MessageTag
): Boolean {
    return updateMessageInConversation(conversationId, messageId) { message ->
        message.copy(tags = (message.tags ?: emptyList()) + tag)
    }
}

/**
 * Update a tag in a message
 */
suspend fun AppRepository.updateTagInMessage(
    conversationId: String,
    messageId: String,
    tagId: String,
    update: (MessageTag) -> MessageTag
): Boolean {
    return updateMessageInConversation(conversationId, messageId) { message ->
        message.copy(
            tags = message.tags.orEmpty().map { tag ->
                if (tag.id == tagId) update(tag) else tag
            }
        )
    }
}

/**
 * Update conversation title
 */
suspend fun AppRepository.updateConversationTitle(
    conversationId: String,
    title: String
): Boolean {
    return updateConversationInList(conversationId) { conversation ->
        conversation.copy(title = title)
    }
}

/**
 * Helper to save conversations list
 */
private suspend fun AppRepository.saveConversations(conversations: List<Conversation>) {
    // This would call the internal save method in AppRepository
    // For now, we use updateConversation which is public
    conversations.firstOrNull()?.let { updateConversation(it) }
}

/**
 * Result wrapper for repository operations
 */
sealed class RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : RepositoryResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun getOrThrow(): T = (this as? Success)?.data ?: throw IllegalStateException((this as? Error)?.message ?: "Unknown error")

    inline fun <R> map(transform: (T) -> R): RepositoryResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun onSuccess(action: (T) -> Unit): RepositoryResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String) -> Unit): RepositoryResult<T> {
        if (this is Error) action(message)
        return this
    }

    companion object {
        inline fun <T> catching(block: () -> T): RepositoryResult<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(e.message ?: "Unknown error", e)
            }
        }
    }
}
