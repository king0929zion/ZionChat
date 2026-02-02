package com.zionchat.app.data

import java.util.UUID

data class ProviderConfig(
    val id: String = UUID.randomUUID().toString(),
    val presetId: String? = null,
    val iconAsset: String? = null,
    val name: String,
    val type: String,
    val apiUrl: String,
    val apiKey: String
)

data class ModelConfig(
    val id: String,
    val displayName: String,
    val enabled: Boolean = true,
    val providerId: String? = null,
    val headers: List<HttpHeader> = emptyList()
)

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
    val timestamp: Long = System.currentTimeMillis()
)
