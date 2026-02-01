package com.zionchat.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.zionDataStore
    private val gson = Gson()

    private val providersKey = stringPreferencesKey("providers_json")
    private val modelsKey = stringPreferencesKey("models_json")
    private val conversationsKey = stringPreferencesKey("conversations_json")
    private val currentConversationIdKey = stringPreferencesKey("current_conversation_id")

    private val providerListType = object : TypeToken<List<ProviderConfig>>() {}.type
    private val modelListType = object : TypeToken<List<ModelConfig>>() {}.type
    private val conversationListType = object : TypeToken<List<Conversation>>() {}.type

    val providersFlow: Flow<List<ProviderConfig>> = dataStore.data.map { prefs ->
        val json = prefs[providersKey] ?: "[]"
        runCatching { gson.fromJson<List<ProviderConfig>>(json, providerListType) }.getOrDefault(emptyList())
    }

    val modelsFlow: Flow<List<ModelConfig>> = dataStore.data.map { prefs ->
        val json = prefs[modelsKey] ?: "[]"
        runCatching { gson.fromJson<List<ModelConfig>>(json, modelListType) }.getOrDefault(emptyList())
    }

    val conversationsFlow: Flow<List<Conversation>> = dataStore.data.map { prefs ->
        val json = prefs[conversationsKey] ?: "[]"
        runCatching { gson.fromJson<List<Conversation>>(json, conversationListType) }.getOrDefault(emptyList())
            .sortedByDescending { it.updatedAt }
    }

    val currentConversationIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[currentConversationIdKey]
    }

    suspend fun setCurrentConversationId(conversationId: String?) {
        dataStore.edit { prefs ->
            if (conversationId.isNullOrBlank()) {
                prefs.remove(currentConversationIdKey)
            } else {
                prefs[currentConversationIdKey] = conversationId
            }
        }
    }

    suspend fun upsertProvider(provider: ProviderConfig) {
        val providers = providersFlow.first().toMutableList()
        val index = providers.indexOfFirst { it.id == provider.id }
        if (index >= 0) {
            providers[index] = provider
        } else {
            providers.add(0, provider)
        }
        dataStore.edit { prefs ->
            prefs[providersKey] = gson.toJson(providers)
        }
    }

    suspend fun deleteProvider(providerId: String) {
        val providers = providersFlow.first().filterNot { it.id == providerId }
        dataStore.edit { prefs ->
            prefs[providersKey] = gson.toJson(providers)
        }
    }

    suspend fun upsertModel(model: ModelConfig) {
        val models = modelsFlow.first().toMutableList()
        val index = models.indexOfFirst { it.id == model.id }
        if (index >= 0) {
            models[index] = model
        } else {
            models.add(0, model)
        }
        dataStore.edit { prefs ->
            prefs[modelsKey] = gson.toJson(models)
        }
    }

    suspend fun deleteModel(modelId: String) {
        val models = modelsFlow.first().filterNot { it.id == modelId }
        dataStore.edit { prefs ->
            prefs[modelsKey] = gson.toJson(models)
        }
    }

    suspend fun createConversation(title: String = "New chat"): Conversation {
        val conversation = Conversation(id = UUID.randomUUID().toString(), title = title)
        val conversations = conversationsFlow.first().toMutableList()
        conversations.add(0, conversation)
        dataStore.edit { prefs ->
            prefs[conversationsKey] = gson.toJson(conversations)
            prefs[currentConversationIdKey] = conversation.id
        }
        return conversation
    }

    suspend fun updateConversation(conversation: Conversation) {
        val conversations = conversationsFlow.first().toMutableList()
        val index = conversations.indexOfFirst { it.id == conversation.id }
        if (index >= 0) {
            conversations[index] = conversation.copy(updatedAt = System.currentTimeMillis())
        } else {
            conversations.add(0, conversation.copy(updatedAt = System.currentTimeMillis()))
        }
        dataStore.edit { prefs ->
            prefs[conversationsKey] = gson.toJson(conversations)
        }
    }

    suspend fun deleteConversation(conversationId: String) {
        val conversations = conversationsFlow.first().filterNot { it.id == conversationId }
        dataStore.edit { prefs ->
            prefs[conversationsKey] = gson.toJson(conversations)
            val current = prefs[currentConversationIdKey]
            if (current == conversationId) {
                prefs.remove(currentConversationIdKey)
            }
        }
    }

    suspend fun appendMessage(conversationId: String, message: Message) {
        val conversations = conversationsFlow.first().toMutableList()
        val index = conversations.indexOfFirst { it.id == conversationId }
        if (index < 0) return

        val conversation = conversations[index]
        val updated = conversation.copy(
            messages = conversation.messages + message,
            updatedAt = System.currentTimeMillis()
        )
        conversations.removeAt(index)
        conversations.add(0, updated)
        dataStore.edit { prefs ->
            prefs[conversationsKey] = gson.toJson(conversations)
        }
    }

    suspend fun updateMessage(conversationId: String, messageId: String, newContent: String) {
        val conversations = conversationsFlow.first().toMutableList()
        val index = conversations.indexOfFirst { it.id == conversationId }
        if (index < 0) return

        val conversation = conversations[index]
        val updatedMessages = conversation.messages.map { msg ->
            if (msg.id == messageId) msg.copy(content = newContent) else msg
        }
        val updatedConversation = conversation.copy(
            messages = updatedMessages,
            updatedAt = System.currentTimeMillis()
        )
        conversations.removeAt(index)
        conversations.add(0, updatedConversation)
        dataStore.edit { prefs ->
            prefs[conversationsKey] = gson.toJson(conversations)
        }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String) {
        val conversations = conversationsFlow.first().toMutableList()
        val index = conversations.indexOfFirst { it.id == conversationId }
        if (index < 0) return

        val conversation = conversations[index]
        val updatedMessages = conversation.messages.filterNot { it.id == messageId }
        val updatedConversation = conversation.copy(
            messages = updatedMessages,
            updatedAt = System.currentTimeMillis()
        )
        conversations.removeAt(index)
        conversations.add(0, updatedConversation)
        dataStore.edit { prefs ->
            prefs[conversationsKey] = gson.toJson(conversations)
        }
    }
}
