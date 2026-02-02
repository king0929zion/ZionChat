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
    private val nicknameKey = stringPreferencesKey("nickname")
    private val customInstructionsKey = stringPreferencesKey("custom_instructions")
    private val defaultChatModelIdKey = stringPreferencesKey("default_chat_model_id")
    private val defaultVisionModelIdKey = stringPreferencesKey("default_vision_model_id")
    private val defaultImageModelIdKey = stringPreferencesKey("default_image_model_id")
    private val defaultTitleModelIdKey = stringPreferencesKey("default_title_model_id")

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

    val nicknameFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[nicknameKey].orEmpty()
    }

    val customInstructionsFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[customInstructionsKey].orEmpty()
    }

    val defaultChatModelIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[defaultChatModelIdKey]
    }

    val defaultVisionModelIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[defaultVisionModelIdKey]
    }

    val defaultImageModelIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[defaultImageModelIdKey]
    }

    val defaultTitleModelIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[defaultTitleModelIdKey]
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

    suspend fun setNickname(value: String) {
        dataStore.edit { prefs ->
            prefs[nicknameKey] = value
        }
    }

    suspend fun setCustomInstructions(value: String) {
        dataStore.edit { prefs ->
            prefs[customInstructionsKey] = value
        }
    }

    suspend fun setDefaultChatModelId(modelId: String?) {
        dataStore.edit { prefs ->
            if (modelId.isNullOrBlank()) prefs.remove(defaultChatModelIdKey) else prefs[defaultChatModelIdKey] = modelId
        }
    }

    suspend fun setDefaultVisionModelId(modelId: String?) {
        dataStore.edit { prefs ->
            if (modelId.isNullOrBlank()) prefs.remove(defaultVisionModelIdKey) else prefs[defaultVisionModelIdKey] = modelId
        }
    }

    suspend fun setDefaultImageModelId(modelId: String?) {
        dataStore.edit { prefs ->
            if (modelId.isNullOrBlank()) prefs.remove(defaultImageModelIdKey) else prefs[defaultImageModelIdKey] = modelId
        }
    }

    suspend fun setDefaultTitleModelId(modelId: String?) {
        dataStore.edit { prefs ->
            if (modelId.isNullOrBlank()) prefs.remove(defaultTitleModelIdKey) else prefs[defaultTitleModelIdKey] = modelId
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

    suspend fun upsertModels(modelsToUpsert: List<ModelConfig>) {
        if (modelsToUpsert.isEmpty()) return
        val models = modelsFlow.first().toMutableList()
        var changed = false

        modelsToUpsert.forEach { incoming ->
            val index = models.indexOfFirst { it.id == incoming.id }
            if (index >= 0) {
                val existing = models[index]
                val merged = existing.copy(
                    displayName = existing.displayName.ifBlank { incoming.displayName },
                    providerId = existing.providerId ?: incoming.providerId,
                )
                if (merged != existing) {
                    models[index] = merged
                    changed = true
                }
            } else {
                models.add(0, incoming)
                changed = true
            }
        }

        if (!changed) return
        dataStore.edit { prefs ->
            prefs[modelsKey] = gson.toJson(models)
        }
    }

    suspend fun setAllModelsEnabled(enabled: Boolean) {
        val models = modelsFlow.first()
        if (models.isEmpty()) return
        val updated = models.map { it.copy(enabled = enabled) }
        dataStore.edit { prefs ->
            prefs[modelsKey] = gson.toJson(updated)
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
