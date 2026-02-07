package com.zionchat.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zionchat.app.data.extractRemoteModelId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.zionDataStore
    private val gson = Gson()
    private val prefsFlow = dataStore.data.catch { emit(emptyPreferences()) }

    private val providersKey = stringPreferencesKey("providers_json")
    private val modelsKey = stringPreferencesKey("models_json")
    private val conversationsKey = stringPreferencesKey("conversations_json")
    private val memoriesKey = stringPreferencesKey("memories_json")
    private val currentConversationIdKey = stringPreferencesKey("current_conversation_id")
    private val nicknameKey = stringPreferencesKey("nickname")
    private val personalNicknameKey = stringPreferencesKey("personal_nickname")
    private val personalNicknameMigratedKey = booleanPreferencesKey("personal_nickname_migrated_v1")
    private val handleKey = stringPreferencesKey("handle")
    private val avatarUriKey = stringPreferencesKey("avatar_uri")
    private val customInstructionsKey = stringPreferencesKey("custom_instructions")
    private val appLanguageKey = stringPreferencesKey("app_language")
    private val appAccentColorKey = stringPreferencesKey("app_accent_color")
    private val defaultChatModelIdKey = stringPreferencesKey("default_chat_model_id")
    private val defaultVisionModelIdKey = stringPreferencesKey("default_vision_model_id")
    private val defaultImageModelIdKey = stringPreferencesKey("default_image_model_id")
    private val defaultTitleModelIdKey = stringPreferencesKey("default_title_model_id")
    private val defaultAppBuilderModelIdKey = stringPreferencesKey("default_app_builder_model_id")
    private val supportedAccentKeys = setOf("default", "blue", "green", "yellow", "pink", "orange", "purple", "black")

    private val providerListType = object : TypeToken<List<ProviderConfig>>() {}.type
    private val modelListType = object : TypeToken<List<ModelConfig>>() {}.type
    private val conversationListType = object : TypeToken<List<Conversation>>() {}.type
    private val memoryListType = object : TypeToken<List<MemoryItem>>() {}.type

    private fun safeTrim(value: String?): String = value?.trim().orEmpty()

    private fun safeTrimOrNull(value: String?): String? = value?.trim()?.takeIf { it.isNotBlank() }

    private fun sanitizeHeaders(headers: List<HttpHeader>?): List<HttpHeader> {
        return headers.orEmpty().mapNotNull { header ->
            val h: HttpHeader? = header
            val key = safeTrim(h?.key)
            if (key.isBlank()) null else HttpHeader(key, h?.value.orEmpty())
        }
    }

    private fun sanitizeProvider(provider: ProviderConfig?): ProviderConfig? {
        if (provider == null) return null
        val id = safeTrim(provider.id)
        if (id.isBlank()) return null

        return ProviderConfig(
            id = id,
            presetId = safeTrimOrNull(provider.presetId),
            iconAsset = safeTrimOrNull(provider.iconAsset),
            name = safeTrim(provider.name),
            type = safeTrim(provider.type),
            apiUrl = safeTrim(provider.apiUrl),
            apiKey = safeTrim(provider.apiKey),
            headers = sanitizeHeaders(provider.headers),
            oauthProvider = safeTrimOrNull(provider.oauthProvider),
            oauthAccessToken = safeTrimOrNull(provider.oauthAccessToken),
            oauthRefreshToken = safeTrimOrNull(provider.oauthRefreshToken),
            oauthIdToken = safeTrimOrNull(provider.oauthIdToken),
            oauthAccountId = safeTrimOrNull(provider.oauthAccountId),
            oauthEmail = safeTrimOrNull(provider.oauthEmail),
            oauthProjectId = safeTrimOrNull(provider.oauthProjectId),
            oauthExpiresAtMs = provider.oauthExpiresAtMs
        )
    }

    private fun sanitizeModel(model: ModelConfig?): ModelConfig? {
        if (model == null) return null
        val id = safeTrim(model.id)
        if (id.isBlank()) return null

        val displayName = safeTrim(model.displayName).ifBlank { id }
        return ModelConfig(
            id = id,
            displayName = displayName,
            enabled = model.enabled,
            providerId = safeTrimOrNull(model.providerId),
            headers = sanitizeHeaders(model.headers),
            reasoningEffort = safeTrimOrNull(model.reasoningEffort)
        )
    }

    private fun sanitizeTag(tag: MessageTag?): MessageTag? {
        if (tag == null) return null
        val kind = safeTrim(tag.kind)
        val title = safeTrim(tag.title)
        val content = tag.content.orEmpty()
        if (kind.isBlank() && title.isBlank() && content.isBlank()) return null

        val id = safeTrim(tag.id).ifBlank { UUID.randomUUID().toString() }
        val createdAt = tag.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        val status =
            tag.status
                ?.trim()
                ?.lowercase()
                ?.takeIf { it == "running" || it == "success" || it == "error" || it == "info" }
        return MessageTag(
            id = id,
            kind = kind.ifBlank { "tag" },
            title = title,
            content = content,
            status = status,
            createdAt = createdAt
        )
    }

    private fun sanitizeMessage(message: Message?): Message? {
        if (message == null) return null
        val role = safeTrim(message.role).ifBlank { "assistant" }
        val content = message.content.orEmpty()
        val timestamp = message.timestamp.takeIf { it > 0 } ?: System.currentTimeMillis()
        val id = safeTrim(message.id).ifBlank {
            UUID.nameUUIDFromBytes("${timestamp}_${role}_${content}".toByteArray()).toString()
        }

        val tags = message.tags.orEmpty().mapNotNull(::sanitizeTag).takeIf { it.isNotEmpty() }
        val reasoning = message.reasoning?.takeIf { it.isNotBlank() }
        return Message(
            id = id,
            role = role,
            content = content,
            reasoning = reasoning,
            tags = tags,
            timestamp = timestamp
        )
    }

    private fun sanitizeConversation(conversation: Conversation?): Conversation? {
        if (conversation == null) return null
        val id = safeTrim(conversation.id)
        if (id.isBlank()) return null

        val title = safeTrim(conversation.title).ifBlank { "New chat" }
        val createdAt = conversation.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        val updatedAt = conversation.updatedAt.takeIf { it > 0 } ?: createdAt
        val messages = conversation.messages.orEmpty().mapNotNull(::sanitizeMessage)
        return Conversation(
            id = id,
            title = title,
            messages = messages,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun sanitizeMemory(item: MemoryItem?): MemoryItem? {
        if (item == null) return null
        val content = safeTrim(item.content)
        if (content.isBlank()) return null

        val id = safeTrim(item.id).ifBlank { UUID.randomUUID().toString() }
        val createdAt = item.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        return MemoryItem(id = id, content = content, createdAt = createdAt)
    }

    private fun sanitizeMcpToolParameter(param: McpToolParameter?): McpToolParameter? {
        if (param == null) return null
        val name = safeTrim(param.name)
        val type = safeTrim(param.type)
        if (name.isBlank() || type.isBlank()) return null
        return McpToolParameter(
            name = name,
            type = type,
            required = param.required,
            description = safeTrim(param.description)
        )
    }

    private fun sanitizeMcpTool(tool: McpTool?): McpTool? {
        if (tool == null) return null
        val name = safeTrim(tool.name)
        if (name.isBlank()) return null
        val description = safeTrim(tool.description)
        val parameters = tool.parameters.orEmpty().mapNotNull(::sanitizeMcpToolParameter)
        return McpTool(name = name, description = description, parameters = parameters)
    }

    private fun sanitizeMcpConfig(mcp: McpConfig?): McpConfig? {
        if (mcp == null) return null
        val name = safeTrim(mcp.name)
        val url = safeTrim(mcp.url)
        if (name.isBlank() && url.isBlank()) return null

        val idSeed = "${name}_$url"
        val id = safeTrim(mcp.id).ifBlank { UUID.nameUUIDFromBytes(idSeed.toByteArray()).toString() }
        val protocol: McpProtocol = (mcp.protocol as McpProtocol?) ?: McpProtocol.HTTP
        val headers = sanitizeHeaders(mcp.headers).distinctBy { it.key.trim().lowercase() }
        val tools = mcp.tools.orEmpty().mapNotNull(::sanitizeMcpTool)

        return McpConfig(
            id = id,
            name = name,
            url = url,
            protocol = protocol,
            enabled = mcp.enabled,
            description = safeTrim(mcp.description),
            headers = headers,
            tools = tools,
            lastSyncAt = mcp.lastSyncAt
        )
    }

    val providersFlow: Flow<List<ProviderConfig>> = prefsFlow.map { prefs ->
        val json = prefs[providersKey] ?: "[]"
        runCatching { gson.fromJson<List<ProviderConfig>>(json, providerListType) }
            .getOrNull()
            .orEmpty()
            .mapNotNull(::sanitizeProvider)
    }

    val modelsFlow: Flow<List<ModelConfig>> = prefsFlow.map { prefs ->
        val json = prefs[modelsKey] ?: "[]"
        runCatching { gson.fromJson<List<ModelConfig>>(json, modelListType) }
            .getOrNull()
            .orEmpty()
            .mapNotNull(::sanitizeModel)
    }

    val conversationsFlow: Flow<List<Conversation>> = prefsFlow.map { prefs ->
        val json = prefs[conversationsKey] ?: "[]"
        runCatching { gson.fromJson<List<Conversation>>(json, conversationListType) }
            .getOrNull()
            .orEmpty()
            .mapNotNull(::sanitizeConversation)
            .sortedByDescending { it.updatedAt }
    }

    val memoriesFlow: Flow<List<MemoryItem>> = prefsFlow.map { prefs ->
        val json = prefs[memoriesKey] ?: "[]"
        runCatching { gson.fromJson<List<MemoryItem>>(json, memoryListType) }
            .getOrNull()
            .orEmpty()
            .mapNotNull(::sanitizeMemory)
            .sortedByDescending { it.createdAt }
    }

    val currentConversationIdFlow: Flow<String?> = prefsFlow.map { prefs ->
        prefs[currentConversationIdKey]
    }

    val nicknameFlow: Flow<String> = prefsFlow.map { prefs ->
        val nickname = prefs[nicknameKey].orEmpty().trim()
        if (nickname.isNotBlank()) nickname else prefs[personalNicknameKey].orEmpty().trim()
    }

    val personalNicknameFlow: Flow<String> = prefsFlow.map { prefs ->
        prefs[personalNicknameKey].orEmpty()
    }

    val handleFlow: Flow<String> = prefsFlow.map { prefs ->
        prefs[handleKey].orEmpty()
    }

    val avatarUriFlow: Flow<String> = prefsFlow.map { prefs ->
        prefs[avatarUriKey].orEmpty()
    }

    val customInstructionsFlow: Flow<String> = prefsFlow.map { prefs ->
        prefs[customInstructionsKey].orEmpty()
    }

    val appLanguageFlow: Flow<String> = prefsFlow.map { prefs ->
        prefs[appLanguageKey]?.trim()?.takeIf { it.isNotBlank() } ?: "system"
    }

    val appAccentColorFlow: Flow<String> = prefsFlow.map { prefs ->
        val key = prefs[appAccentColorKey]?.trim()?.lowercase().orEmpty()
        if (supportedAccentKeys.contains(key)) key else "default"
    }

    val defaultChatModelIdFlow: Flow<String?> = prefsFlow.map { prefs ->
        prefs[defaultChatModelIdKey]
    }

    val defaultVisionModelIdFlow: Flow<String?> = prefsFlow.map { prefs ->
        prefs[defaultVisionModelIdKey]
    }

    val defaultImageModelIdFlow: Flow<String?> = prefsFlow.map { prefs ->
        prefs[defaultImageModelIdKey]
    }

    val defaultTitleModelIdFlow: Flow<String?> = prefsFlow.map { prefs ->
        prefs[defaultTitleModelIdKey]
    }

    val defaultAppBuilderModelIdFlow: Flow<String?> = prefsFlow.map { prefs ->
        prefs[defaultAppBuilderModelIdKey]
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
            val trimmed = value.trimEnd()
            if (trimmed.isBlank()) {
                prefs.remove(nicknameKey)
                prefs.remove(personalNicknameKey)
            } else {
                prefs[nicknameKey] = trimmed
                prefs[personalNicknameKey] = trimmed
            }
        }
    }

    suspend fun setPersonalNickname(value: String) {
        dataStore.edit { prefs ->
            val trimmed = value.trimEnd()
            if (trimmed.isBlank()) {
                prefs.remove(personalNicknameKey)
                prefs.remove(nicknameKey)
            } else {
                prefs[personalNicknameKey] = trimmed
                prefs[nicknameKey] = trimmed
            }
        }
    }

    suspend fun setHandle(value: String) {
        dataStore.edit { prefs ->
            prefs[handleKey] = value
        }
    }

    suspend fun setAvatarUri(uri: String) {
        dataStore.edit { prefs ->
            if (uri.isBlank()) {
                prefs.remove(avatarUriKey)
            } else {
                prefs[avatarUriKey] = uri
            }
        }
    }

    suspend fun setCustomInstructions(value: String) {
        dataStore.edit { prefs ->
            prefs[customInstructionsKey] = value
        }
    }

    suspend fun setAppLanguage(value: String) {
        val key =
            when (value.trim().lowercase()) {
                "en", "zh", "system" -> value.trim().lowercase()
                else -> "system"
            }
        dataStore.edit { prefs ->
            prefs[appLanguageKey] = key
        }
    }

    suspend fun setAppAccentColor(value: String) {
        val key = value.trim().lowercase()
        dataStore.edit { prefs ->
            prefs[appAccentColorKey] = if (supportedAccentKeys.contains(key)) key else "default"
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

    suspend fun setDefaultAppBuilderModelId(modelId: String?) {
        dataStore.edit { prefs ->
            if (modelId.isNullOrBlank()) prefs.remove(defaultAppBuilderModelIdKey) else prefs[defaultAppBuilderModelIdKey] = modelId
        }
    }

    suspend fun migratePersonalNicknameIfNeeded() {
        dataStore.edit { prefs ->
            val migrated = prefs[personalNicknameMigratedKey] ?: false
            if (migrated) return@edit

            val legacy = prefs[nicknameKey].orEmpty().trim()
            if (prefs[personalNicknameKey].isNullOrBlank() && legacy.isNotBlank()) {
                prefs[personalNicknameKey] = legacy
            }
            val personal = prefs[personalNicknameKey].orEmpty().trim()
            if (prefs[nicknameKey].isNullOrBlank() && personal.isNotBlank()) {
                prefs[nicknameKey] = personal
            }

            prefs[personalNicknameMigratedKey] = true
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

    suspend fun deleteProviderAndModels(providerId: String) {
        val existingProviders = providersFlow.first()
        val existingModels = modelsFlow.first()

        val providers = existingProviders.filterNot { it.id == providerId }
        val removedModels = existingModels.filter { it.providerId == providerId }
        val remainingModels = existingModels.filterNot { it.providerId == providerId }

        val removedStorageIds = removedModels.map { it.id }.toSet()
        val removedRemoteIds = removedModels.map { extractRemoteModelId(it.id) }.toSet()
        val remainingRemoteIds = remainingModels.map { extractRemoteModelId(it.id) }.toSet()

        dataStore.edit { prefs ->
            prefs[providersKey] = gson.toJson(providers)
            prefs[modelsKey] = gson.toJson(remainingModels)

            fun maybeClearDefaultModel(key: androidx.datastore.preferences.core.Preferences.Key<String>) {
                val value = prefs[key]?.trim().orEmpty()
                if (value.isBlank()) return
                val shouldClear =
                    removedStorageIds.contains(value) ||
                        (removedRemoteIds.contains(value) && !remainingRemoteIds.contains(value))
                if (shouldClear) prefs.remove(key)
            }

            maybeClearDefaultModel(defaultChatModelIdKey)
            maybeClearDefaultModel(defaultVisionModelIdKey)
            maybeClearDefaultModel(defaultImageModelIdKey)
            maybeClearDefaultModel(defaultTitleModelIdKey)
            maybeClearDefaultModel(defaultAppBuilderModelIdKey)
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

    suspend fun setAllModelsEnabled(providerId: String, enabled: Boolean) {
        val models = modelsFlow.first()
        if (models.isEmpty()) return
        val updated = models.map { model ->
            if (model.providerId == providerId) model.copy(enabled = enabled) else model
        }
        if (updated == models) return
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

    suspend fun appendMessageTag(conversationId: String, messageId: String, tag: MessageTag) {
        val conversations = conversationsFlow.first().toMutableList()
        val index = conversations.indexOfFirst { it.id == conversationId }
        if (index < 0) return

        val conversation = conversations[index]
        val updatedMessages =
            conversation.messages.map { msg ->
                if (msg.id == messageId) {
                    val tags = msg.tags.orEmpty() + tag
                    msg.copy(tags = tags)
                } else {
                    msg
                }
            }

        val updatedConversation =
            conversation.copy(
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

    suspend fun addMemory(content: String): MemoryItem? {
        val text = content.trim()
        if (text.isBlank()) return null

        val memories = memoriesFlow.first().toMutableList()
        val existingIndex = memories.indexOfFirst { it.content.trim().equals(text, ignoreCase = true) }
        if (existingIndex >= 0) {
            return memories[existingIndex]
        }

        val item = MemoryItem(content = text)
        memories.add(0, item)
        dataStore.edit { prefs ->
            prefs[memoriesKey] = gson.toJson(memories)
        }
        return item
    }

    suspend fun deleteMemory(id: String) {
        val key = id.trim()
        if (key.isBlank()) return
        val memories = memoriesFlow.first().filterNot { it.id == key }
        dataStore.edit { prefs ->
            prefs[memoriesKey] = gson.toJson(memories)
        }
    }

    suspend fun clearMemories() {
        dataStore.edit { prefs ->
            prefs[memoriesKey] = "[]"
        }
    }

    // MCP (Model Context Protocol) Storage
    private val mcpListKey = stringPreferencesKey("mcp_list_json")
    private val mcpListType = object : TypeToken<List<McpConfig>>() {}.type

    val mcpListFlow: Flow<List<McpConfig>> = prefsFlow.map { prefs ->
        val json = prefs[mcpListKey] ?: "[]"
        runCatching { gson.fromJson<List<McpConfig>>(json, mcpListType) }
            .getOrNull()
            .orEmpty()
            .mapNotNull(::sanitizeMcpConfig)
    }

    suspend fun upsertMcp(mcp: McpConfig) {
        val mcpList = mcpListFlow.first().toMutableList()
        val index = mcpList.indexOfFirst { it.id == mcp.id }
        if (index >= 0) {
            mcpList[index] = mcp
        } else {
            mcpList.add(0, mcp)
        }
        dataStore.edit { prefs ->
            prefs[mcpListKey] = gson.toJson(mcpList)
        }
    }

    suspend fun deleteMcp(mcpId: String) {
        val mcpList = mcpListFlow.first().filterNot { it.id == mcpId }
        dataStore.edit { prefs ->
            prefs[mcpListKey] = gson.toJson(mcpList)
        }
    }

    suspend fun updateMcpTools(mcpId: String, tools: List<McpTool>) {
        val mcpList = mcpListFlow.first().toMutableList()
        val index = mcpList.indexOfFirst { it.id == mcpId }
        if (index >= 0) {
            mcpList[index] = mcpList[index].copy(
                tools = tools,
                lastSyncAt = System.currentTimeMillis()
            )
            dataStore.edit { prefs ->
                prefs[mcpListKey] = gson.toJson(mcpList)
            }
        }
    }

    suspend fun toggleMcpEnabled(mcpId: String) {
        val mcpList = mcpListFlow.first().toMutableList()
        val index = mcpList.indexOfFirst { it.id == mcpId }
        if (index >= 0) {
            mcpList[index] = mcpList[index].copy(enabled = !mcpList[index].enabled)
            dataStore.edit { prefs ->
                prefs[mcpListKey] = gson.toJson(mcpList)
            }
        }
    }
}
