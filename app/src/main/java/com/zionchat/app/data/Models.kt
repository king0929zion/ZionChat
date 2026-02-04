package com.zionchat.app.data

import java.util.UUID

data class ProviderConfig(
    val id: String = UUID.randomUUID().toString(),
    val presetId: String? = null,
    val iconAsset: String? = null,
    val name: String,
    val type: String,
    val apiUrl: String,
    val apiKey: String,
    val oauthProvider: String? = null,
    val oauthAccessToken: String? = null,
    val oauthRefreshToken: String? = null,
    val oauthIdToken: String? = null,
    val oauthAccountId: String? = null,
    val oauthEmail: String? = null,
    val oauthProjectId: String? = null,
    val oauthExpiresAtMs: Long? = null
)

data class ModelConfig(
    val id: String,
    val displayName: String,
    val enabled: Boolean = true,
    val providerId: String? = null,
    val headers: List<HttpHeader> = emptyList(),
    val reasoningEffort: String? = null
)

private const val MODEL_ID_SEPARATOR = "::"

fun buildModelStorageId(providerId: String, modelId: String): String {
    return "${providerId.trim()}$MODEL_ID_SEPARATOR${modelId.trim()}"
}

fun extractRemoteModelId(storageId: String): String {
    return storageId.substringAfter(MODEL_ID_SEPARATOR, storageId).trim()
}

data class HttpHeader(
    val key: String,
    val value: String
)

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New chat",
    val messages: List<Message> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val reasoning: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
