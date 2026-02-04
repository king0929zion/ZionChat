package com.zionchat.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.zionchat.app.R
import com.zionchat.app.LocalAppRepository
import com.zionchat.app.LocalChatApiClient
import com.zionchat.app.LocalProviderAuthManager
import com.zionchat.app.data.AppRepository
import com.zionchat.app.data.ChatApiClient
import com.zionchat.app.data.Conversation
import com.zionchat.app.data.HttpHeader
import com.zionchat.app.data.Message
import com.zionchat.app.data.ProviderConfig
import com.zionchat.app.data.extractRemoteModelId
import com.zionchat.app.ui.components.TopFadeScrim
import com.zionchat.app.ui.components.BottomFadeScrim
import com.zionchat.app.ui.components.MarkdownText
import com.zionchat.app.ui.components.rememberResourceDrawablePainter
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.*
import coil.compose.AsyncImage
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

// 颜色常量 - 完全匹配HTML原型
private data class PendingMessage(val conversationId: String, val message: Message)
private data class PendingImageAttachment(val uri: Uri? = null, val bitmap: Bitmap? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    val repository = LocalAppRepository.current
    val chatApiClient = LocalChatApiClient.current
    val providerAuthManager = LocalProviderAuthManager.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showToolMenu by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }
    var imageAttachments by remember { mutableStateOf<List<PendingImageAttachment>>(emptyList()) }
    var showThinkingSheet by remember { mutableStateOf(false) }
    var thinkingSheetText by remember { mutableStateOf<String?>(null) }
    val thinkingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val photoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageAttachments = imageAttachments + PendingImageAttachment(uri = uri)
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                imageAttachments = imageAttachments + PendingImageAttachment(bitmap = bitmap)
            }
        }

    val conversations by repository.conversationsFlow.collectAsState(initial = emptyList())
    val currentConversationId by repository.currentConversationIdFlow.collectAsState(initial = null)
    val nickname by repository.nicknameFlow.collectAsState(initial = "")
    val avatarUri by repository.avatarUriFlow.collectAsState(initial = "")
    val customInstructions by repository.customInstructionsFlow.collectAsState(initial = "")
    val defaultChatModelId by repository.defaultChatModelIdFlow.collectAsState(initial = null)
    val defaultImageModelId by repository.defaultImageModelIdFlow.collectAsState(initial = null)

    // 本地优先的会话选择：避免 DataStore 状态滞后导致“首条消息消失/会话跳回”
    var preferredConversationId by remember { mutableStateOf<String?>(null) }
    var preferredConversationSetAtMs by remember { mutableStateOf(0L) }

    LaunchedEffect(currentConversationId) {
        if (preferredConversationId.isNullOrBlank() && !currentConversationId.isNullOrBlank()) {
            preferredConversationId = currentConversationId
            preferredConversationSetAtMs = System.currentTimeMillis()
        }
    }

    val effectiveConversationId = remember(
        conversations,
        currentConversationId,
        preferredConversationId,
        preferredConversationSetAtMs
    ) {
        val preferred = preferredConversationId?.trim().takeIf { !it.isNullOrBlank() }
        val fromStore = currentConversationId?.trim().takeIf { !it.isNullOrBlank() }
        if (preferred == null) {
            fromStore ?: conversations.firstOrNull()?.id
        } else {
            val inList = conversations.any { it.id == preferred }
            val withinGrace = System.currentTimeMillis() - preferredConversationSetAtMs < 2500
            when {
                inList || withinGrace -> preferred
                !fromStore.isNullOrBlank() -> fromStore
                else -> conversations.firstOrNull()?.id
            }
        }
    }

    val currentConversation = remember(conversations, effectiveConversationId) {
        val cid = effectiveConversationId?.trim().orEmpty()
        if (cid.isBlank()) null else conversations.firstOrNull { it.id == cid }
    }

    LaunchedEffect(conversations, currentConversationId) {
        if (currentConversationId.isNullOrBlank() && conversations.isNotEmpty()) {
            repository.setCurrentConversationId(conversations.first().id)
        }
    }

    val messages = currentConversation?.messages.orEmpty()

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeightDp = with(density) { if (topBarHeightPx == 0) 66.dp else topBarHeightPx.toDp() }
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listTopPadding = statusBarTopPadding + topBarHeightDp + 8.dp
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val bottomBarHeightDp = with(density) { bottomBarHeightPx.toDp() }
    val bottomContentPadding = maxOf(80.dp, bottomBarHeightDp + 12.dp)
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    // Pending 消息：在 DataStore 落盘前立即显示，彻底修复“首条消息消失”
    var pendingMessages by remember { mutableStateOf<List<PendingMessage>>(emptyList()) }

    // 流式输出：用同一条 assistant 消息实时更新，避免结束时“闪一下重新渲染”
    var isStreaming by remember { mutableStateOf(false) }
    var streamingMessageId by remember { mutableStateOf<String?>(null) }
    var streamingConversationId by remember { mutableStateOf<String?>(null) }

    // 清理已落盘的 pending，避免列表不断增长
    LaunchedEffect(conversations, pendingMessages) {
        if (pendingMessages.isEmpty()) return@LaunchedEffect
        val updated = pendingMessages.filter { pending ->
            val convo = conversations.firstOrNull { it.id == pending.conversationId } ?: return@filter true
            convo.messages.none { it.id == pending.message.id }
        }
        if (updated.size != pendingMessages.size) {
            pendingMessages = updated
        }
    }

    val localMessages = remember(messages, pendingMessages, effectiveConversationId) {
        val convoId = effectiveConversationId?.trim().orEmpty()
        val dataStoreMessages = messages
        if (convoId.isBlank()) return@remember dataStoreMessages
        val pendingForConversation = pendingMessages
            .filter { it.conversationId == convoId }
            .map { it.message }
            .filterNot { pendingMsg -> dataStoreMessages.any { it.id == pendingMsg.id } }
        dataStoreMessages + pendingForConversation
    }

    val shouldAutoScroll by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            if (total == 0) return@derivedStateOf true
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= total - 2
        }
    }
    val latestLocalMessagesSize by rememberUpdatedState(localMessages.size)
    val latestShouldAutoScroll by rememberUpdatedState(shouldAutoScroll)
    val latestEffectiveConversationId by rememberUpdatedState(effectiveConversationId)

    var lastAutoScrolledConversationId by remember { mutableStateOf<String?>(null) }
    var scrollToBottomToken by remember { mutableIntStateOf(0) }

    // 进入新会话时直接定位到底部；新消息仅在“接近底部”时自动滚动，避免跳动
    LaunchedEffect(effectiveConversationId, localMessages.size) {
        val convoId = effectiveConversationId?.trim().orEmpty()
        if (convoId.isBlank() || localMessages.isEmpty()) return@LaunchedEffect

        if (lastAutoScrolledConversationId != convoId) {
            lastAutoScrolledConversationId = convoId
            listState.scrollToItem(localMessages.size - 1, scrollOffset = Int.MAX_VALUE)
            return@LaunchedEffect
        }
        if (shouldAutoScroll) {
            listState.animateScrollToItem(localMessages.size - 1, scrollOffset = Int.MAX_VALUE)
        }
    }

    // 发送消息时强制滚动到底部
    LaunchedEffect(scrollToBottomToken) {
        if (latestLocalMessagesSize > 0) {
            listState.animateScrollToItem(latestLocalMessagesSize - 1, scrollOffset = Int.MAX_VALUE)
        }
    }

    // 流式过程中节流保持到底部（仅当用户未手动上滑）
    LaunchedEffect(isStreaming, streamingMessageId, streamingConversationId) {
        if (!isStreaming) return@LaunchedEffect
        while (isStreaming) {
            val convoId = latestEffectiveConversationId?.trim().orEmpty()
            if (convoId.isNotBlank() && convoId == streamingConversationId && latestShouldAutoScroll) {
                val lastIndex = latestLocalMessagesSize - 1
                if (lastIndex >= 0) {
                    listState.scrollToItem(lastIndex, scrollOffset = Int.MAX_VALUE)
                }
            }
            delay(120)
        }
    }

    fun startNewChat() {
        scope.launch {
            val created = repository.createConversation()
            preferredConversationId = created.id
            preferredConversationSetAtMs = System.currentTimeMillis()
            selectedTool = null
            messageText = ""
            drawerState.close()
            scrollToBottomToken++
        }
    }

    fun sendMessage() {
        val trimmed = messageText.trim()
        val attachmentsSnapshot = imageAttachments
        if ((trimmed.isEmpty() && attachmentsSnapshot.isEmpty()) || isStreaming) return

        scope.launch {
            val nowMs = System.currentTimeMillis()
            val provisionalTitle = trimmed.lineSequence().firstOrNull().orEmpty().trim().take(24)
            val initialConversations = repository.conversationsFlow.first()

            var conversationId = effectiveConversationId?.trim().takeIf { !it.isNullOrBlank() }
                ?: repository.currentConversationIdFlow.first()?.trim().takeIf { !it.isNullOrBlank() }
            var conversation = conversationId?.let { cid -> initialConversations.firstOrNull { it.id == cid } }

            if (conversation == null) {
                val created = repository.createConversation()
                conversation = created
                conversationId = created.id
            }

            val safeConversationId = conversationId ?: return@launch
            preferredConversationId = safeConversationId
            preferredConversationSetAtMs = nowMs
            repository.setCurrentConversationId(safeConversationId)

            val encodedImages = mutableListOf<String>()
            for (attachment in attachmentsSnapshot) {
                val url = encodeImageAttachmentToDataUrl(context, attachment)
                if (url.isNullOrBlank()) {
                    repository.appendMessage(
                        safeConversationId,
                        Message(
                            role = "assistant",
                            content = "Failed to read the selected image. Please try again."
                        )
                    )
                    return@launch
                }
                encodedImages.add(url)
            }

            val imageMarkdown = encodedImages.joinToString("\n\n") { url -> "![]($url)" }.trim()
            val userContent =
                buildString {
                    val text = trimmed.trim()
                    if (text.isNotBlank()) append(text)
                    if (imageMarkdown.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(imageMarkdown)
                    }
                }.trim()
            val hasVisionInput =
                userContent.contains("data:image", ignoreCase = true) ||
                    Regex("!\\[[^\\]]*\\]\\(([^)]+)\\)").containsMatchIn(userContent)
            val userMessage = Message(role = "user", content = userContent)
            pendingMessages = pendingMessages + PendingMessage(safeConversationId, userMessage)
            messageText = ""
            imageAttachments = emptyList()
            scrollToBottomToken++
            repository.appendMessage(safeConversationId, userMessage)

            // Update conversation title only using the latest persisted conversation to avoid wiping messages.
            val latestConversationForTitle =
                repository.conversationsFlow.first().firstOrNull { it.id == safeConversationId }
            if (
                latestConversationForTitle != null &&
                    (latestConversationForTitle.title.isBlank() || latestConversationForTitle.title == "New chat")
            ) {
                if (provisionalTitle.isNotBlank()) {
                    repository.updateConversation(latestConversationForTitle.copy(title = provisionalTitle))
                }
            }

            val latestDefaultChatModelId = repository.defaultChatModelIdFlow.first()
            if (latestDefaultChatModelId.isNullOrBlank()) {
                repository.appendMessage(
                    safeConversationId,
                    Message(
                        role = "assistant",
                        content = "Please configure Chat Model (required) in Settings → Default model before chatting."
                    )
                )
                return@launch
            }

            val effectiveModelId =
                if (hasVisionInput) {
                    repository.defaultVisionModelIdFlow.first()?.trim().takeIf { !it.isNullOrBlank() }
                        ?: latestDefaultChatModelId
                } else {
                    latestDefaultChatModelId
                }

            val allModels = repository.modelsFlow.first()
            val selectedModel =
                allModels.firstOrNull { it.id == effectiveModelId }
                    ?: allModels.firstOrNull { extractRemoteModelId(it.id) == effectiveModelId }
            if (selectedModel == null) {
                repository.appendMessage(
                    safeConversationId,
                    Message(
                        role = "assistant",
                        content = "Default model not found: $effectiveModelId. Enable or add it in Models, then re-select it in Settings → Default model."
                    )
                )
                return@launch
            }

            val providerList = repository.providersFlow.first()
            val provider = selectedModel.providerId?.let { pid -> providerList.firstOrNull { it.id == pid } } ?: providerList.firstOrNull()
            if (provider == null || provider.apiUrl.isBlank() || provider.apiKey.isBlank()) {
                repository.appendMessage(
                    safeConversationId,
                    Message(
                        role = "assistant",
                        content = "Please add a provider in Settings → Model services, and fill in API URL and API Key."
                    )
                )
                return@launch
            }

            val systemMessage = run {
                val latestNickname = repository.nicknameFlow.first().trim()
                val latestInstructions = repository.customInstructionsFlow.first().trim()
                val latestMemories =
                    repository.memoriesFlow.first()
                        .map { it.content.trim() }
                        .filter { it.isNotBlank() }
                        .take(12)
                val content = buildString {
                    if (latestNickname.isNotBlank()) {
                        append("Nickname: ")
                        append(latestNickname)
                    }
                    if (latestInstructions.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(latestInstructions)
                    }
                    if (latestMemories.isNotEmpty()) {
                        if (isNotEmpty()) append("\n\n")
                        append("Memories:\n")
                        latestMemories.forEach { memory ->
                            append("- ")
                            append(memory)
                            append('\n')
                        }
                    }
                }.trim()
                if (content.isBlank()) null else Message(role = "system", content = content)
            }

            // 获取最新的消息列表
            val refreshedConversations = repository.conversationsFlow.first()
            val latestConversation = refreshedConversations.firstOrNull { it.id == safeConversationId }
            val currentMessages = latestConversation?.messages.orEmpty()

            // 检查是否为图片生成请求
            val isImageGeneration = selectedTool == "image"

            // 流式输出
            val assistantMessage = Message(role = "assistant", content = "")
            pendingMessages = pendingMessages + PendingMessage(safeConversationId, assistantMessage)
            isStreaming = true
            streamingMessageId = assistantMessage.id
            streamingConversationId = safeConversationId
            scrollToBottomToken++

            fun updateAssistantContent(content: String, reasoning: String?) {
                pendingMessages =
                    pendingMessages.map { pending ->
                        if (pending.conversationId == safeConversationId && pending.message.id == assistantMessage.id) {
                            pending.copy(message = pending.message.copy(content = content, reasoning = reasoning))
                        } else {
                            pending
                        }
                    }
            }

            try {
                val resolvedProvider = providerAuthManager.ensureValidProvider(provider)
                if (isImageGeneration) {
                    // 图片生成流程
                    handleImageGeneration(
                        repository = repository,
                        chatApiClient = chatApiClient,
                        safeConversationId = safeConversationId,
                        userPrompt = trimmed,
                        assistantMessage = assistantMessage,
                        updateAssistantContent = { updateAssistantContent(it, null) }
                    )
                } else {
                    // 普通聊天流程
                    val toolContextMessage =
                        if (selectedTool == "web") {
                            val webContext = chatApiClient.webSearch(trimmed).getOrDefault("")
                            webContext.takeIf { it.isNotBlank() }?.let { content ->
                                Message(
                                    role = "system",
                                    content =
                                        "Use the following web search results as reference. If they are insufficient, answer based on best effort.\n\n$content"
                                )
                            }
                        } else {
                            null
                        }

                    val requestMessages = buildList {
                        if (systemMessage != null) add(systemMessage)
                        if (toolContextMessage != null) add(toolContextMessage)
                        addAll(currentMessages)
                        if (currentMessages.lastOrNull()?.id != userMessage.id) add(userMessage)
                    }

                    val visibleContent = StringBuilder()
                    val thinkingContent = StringBuilder()
                    var inThink = false
                    var remainder = ""
                    val keepTail = 9 // tag boundary buffer
                    var lastUiUpdateMs = 0L

                    fun appendWithThinkExtraction(input: String) {
                        if (input.isEmpty()) return
                        var s = remainder + input
                        remainder = ""
                        var i = 0
                        while (i < s.length) {
                            if (!inThink) {
                                val idxThink = s.indexOf("<think>", i)
                                val idxThinking = s.indexOf("<thinking>", i)
                                val next = when {
                                    idxThink < 0 -> idxThinking
                                    idxThinking < 0 -> idxThink
                                    else -> minOf(idxThink, idxThinking)
                                }
                                if (next < 0) {
                                    val safeEnd = maxOf(i, s.length - keepTail)
                                    if (safeEnd > i) visibleContent.append(s.substring(i, safeEnd))
                                    remainder = s.substring(safeEnd)
                                    return
                                }
                                if (next > i) visibleContent.append(s.substring(i, next))
                                if (idxThinking == next) {
                                    inThink = true
                                    i = next + "<thinking>".length
                                } else {
                                    inThink = true
                                    i = next + "<think>".length
                                }
                            } else {
                                val idxEndThink = s.indexOf("</think>", i)
                                val idxEndThinking = s.indexOf("</thinking>", i)
                                val next = when {
                                    idxEndThink < 0 -> idxEndThinking
                                    idxEndThinking < 0 -> idxEndThink
                                    else -> minOf(idxEndThink, idxEndThinking)
                                }
                                if (next < 0) {
                                    val safeEnd = maxOf(i, s.length - keepTail)
                                    if (safeEnd > i) thinkingContent.append(s.substring(i, safeEnd))
                                    remainder = s.substring(safeEnd)
                                    return
                                }
                                if (next > i) thinkingContent.append(s.substring(i, next))
                                if (idxEndThinking == next) {
                                    inThink = false
                                    i = next + "</thinking>".length
                                } else {
                                    inThink = false
                                    i = next + "</think>".length
                                }
                            }
                        }
                    }

                    chatApiClient.chatCompletionsStream(
                        provider = resolvedProvider,
                        modelId = extractRemoteModelId(selectedModel.id),
                        messages = requestMessages,
                        extraHeaders = selectedModel.headers
                    ).collect { delta ->
                        delta.reasoning?.takeIf { it.isNotBlank() }?.let { thinkingContent.append(it) }
                        delta.content?.let { appendWithThinkExtraction(it) }
                        val now = System.currentTimeMillis()
                        val shouldUpdate = now - lastUiUpdateMs >= 33L ||
                            (visibleContent.length + thinkingContent.length) % 120 == 0
                        if (shouldUpdate) {
                            val thinkingNow = thinkingContent.toString().trim().ifBlank { null }
                            updateAssistantContent(visibleContent.toString(), thinkingNow)
                            lastUiUpdateMs = now
                        }
                    }

                    if (remainder.isNotEmpty()) {
                        if (inThink) thinkingContent.append(remainder) else visibleContent.append(remainder)
                        remainder = ""
                    }
                    val finalContent = visibleContent.toString()
                    val finalReasoning = thinkingContent.toString().trim().ifBlank { null }
                    updateAssistantContent(finalContent, finalReasoning)
                    if (finalContent.isNotBlank() || !finalReasoning.isNullOrBlank()) {
                        repository.appendMessage(
                            safeConversationId,
                            assistantMessage.copy(content = finalContent, reasoning = finalReasoning)
                        )
                    }

                    if (finalContent.isNotBlank()) {
                        val chatModelId = extractRemoteModelId(selectedModel.id)
                        val chatExtraHeaders = selectedModel.headers
                        val userTextForMemory = trimmed
                        val assistantTextForMemory = finalContent

                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                val candidates =
                                    extractMemoryCandidatesFromTurn(
                                        chatApiClient = chatApiClient,
                                        provider = resolvedProvider,
                                        modelId = chatModelId,
                                        userText = userTextForMemory,
                                        assistantText = assistantTextForMemory,
                                        extraHeaders = chatExtraHeaders
                                    )
                                candidates.forEach { repository.addMemory(it) }
                            }
                        }

                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                val titleModelKey = repository.defaultTitleModelIdFlow.first()?.trim().orEmpty()
                                if (titleModelKey.isBlank()) return@runCatching

                                val modelsAll = repository.modelsFlow.first()
                                val titleModel =
                                    modelsAll.firstOrNull { it.id == titleModelKey }
                                        ?: modelsAll.firstOrNull { extractRemoteModelId(it.id) == titleModelKey }
                                        ?: return@runCatching

                                val providersAll = repository.providersFlow.first()
                                val titleProvider =
                                    titleModel.providerId?.let { pid -> providersAll.firstOrNull { it.id == pid } }
                                        ?: providersAll.firstOrNull()
                                        ?: return@runCatching

                                if (titleProvider.apiUrl.isBlank() || titleProvider.apiKey.isBlank()) return@runCatching

                                val validTitleProvider = providerAuthManager.ensureValidProvider(titleProvider)
                                val convo = repository.conversationsFlow.first().firstOrNull { it.id == safeConversationId }
                                    ?: return@runCatching

                                val currentTitle = convo.title.trim()
                                val shouldGenerateTitle =
                                    currentTitle.isBlank() ||
                                        currentTitle == "New chat" ||
                                        (provisionalTitle.isNotBlank() && currentTitle == provisionalTitle.trim())
                                if (!shouldGenerateTitle) return@runCatching

                                val transcript =
                                    buildConversationTranscript(
                                        messages = convo.messages,
                                        maxMessages = 12,
                                        maxCharsPerMessage = 420
                                    )
                                if (transcript.isBlank()) return@runCatching

                                val generatedTitle =
                                    generateConversationTitle(
                                        chatApiClient = chatApiClient,
                                        provider = validTitleProvider,
                                        modelId = extractRemoteModelId(titleModel.id),
                                        transcript = transcript,
                                        extraHeaders = titleModel.headers
                                    )?.trim().orEmpty()

                                val finalTitle = generatedTitle.trim().trim('"', '\'').take(48)
                                if (finalTitle.isBlank()) return@runCatching

                                repository.updateConversation(convo.copy(title = finalTitle))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Request failed: ${e.message ?: e.toString()}"
                updateAssistantContent(errorMsg, null)
                repository.appendMessage(safeConversationId, assistantMessage.copy(content = errorMsg, reasoning = null))
            } finally {
                isStreaming = false
                streamingMessageId = null
                streamingConversationId = null
                selectedTool = null // 清除选中的工具
            }
        }
    }

    val displayName = nickname.takeIf { it.isNotBlank() } ?: "Kendall Williamson"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarContent(
                conversations = conversations,
                currentConversationId = effectiveConversationId,
                nickname = displayName,
                avatarUri = avatarUri,
                onClose = { scope.launch { drawerState.close() } },
                onNewChat = ::startNewChat,
                onConversationClick = { convo ->
                    scope.launch {
                        preferredConversationId = convo.id
                        preferredConversationSetAtMs = System.currentTimeMillis()
                        repository.setCurrentConversationId(convo.id)
                        drawerState.close()
                        scrollToBottomToken++
                    }
                },
                onDeleteConversation = { id ->
                    scope.launch {
                        repository.deleteConversation(id)
                        if (preferredConversationId == id) {
                            preferredConversationId = null
                        }
                    }
                },
                navController = navController
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.25f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatBackground)
        ) {
            // Chat content (behind the top bar), so messages can scroll into the fade region.
            if (localMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = listTopPadding, bottom = bottomContentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyChatState()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = listTopPadding,
                        bottom = bottomContentPadding + 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(localMessages, key = { _, item -> item.id }) { index, message ->
                        val showToolbar = !isStreaming || (streamingMessageId == null) ||
                            (index >= localMessages.size - 3)
                        MessageItem(
                            message = message,
                            conversationId = effectiveConversationId,
                            isStreaming = isStreaming
                                && streamingConversationId == effectiveConversationId
                                && message.id == streamingMessageId,
                            showToolbar = showToolbar,
                            onShowReasoning = { reasoning ->
                                keyboardController?.hide()
                                showToolMenu = false
                                thinkingSheetText = reasoning
                                showThinkingSheet = true
                            },
                            onEdit = { /* TODO */ },
                            onDelete = { convoId, messageId ->
                                scope.launch { repository.deleteMessage(convoId, messageId) }
                            }
                        )
                    }
                }
            }

            // Top fade: start at the bottom of TopNavBar (blue line), fully hidden above (orange line).
            val topFadeHeight = 36.dp
            val topFadeTopPadding = maxOf(
                statusBarTopPadding,
                statusBarTopPadding + topBarHeightDp - topFadeHeight
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topFadeTopPadding)
                    .background(ChatBackground)
                    .zIndex(1f)
            )
            TopFadeScrim(
                color = ChatBackground,
                height = topFadeHeight,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topFadeTopPadding)
                    .zIndex(1f)
            )

            // Bottom fade: start at the input bar top and fade into the bar.
            val bottomFadeHeight = 24.dp
            val bottomMaskHeight = bottomBarHeightDp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(bottomMaskHeight)
                    .imePadding()
                    .background(ChatBackground)
                    .zIndex(1f)
            )
            BottomFadeScrim(
                color = ChatBackground,
                height = bottomFadeHeight,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(bottom = bottomMaskHeight)
                    .zIndex(1f)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .zIndex(2f)
            ) {
                Box(modifier = Modifier.onSizeChanged { topBarHeightPx = it.height }) {
                    TopNavBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onNewChatClick = ::startNewChat
                    )
                }
            }

            // 底部输入框区域 - 固定在底部（高度动态，支持多行/工具标签）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .zIndex(5f)
            ) {
                Box(
                    modifier = Modifier
                        .onSizeChanged { bottomBarHeightPx = it.height }
                ) {
                    BottomInputArea(
                        selectedTool = selectedTool,
                        attachments = imageAttachments,
                        onRemoveAttachment = { index ->
                            if (index < 0 || index >= imageAttachments.size) return@BottomInputArea
                            val updated = imageAttachments.toMutableList()
                            updated.removeAt(index)
                            imageAttachments = updated.toList()
                        },
                        onToolToggle = {
                            if (showToolMenu) {
                                showToolMenu = false
                                return@BottomInputArea
                            }
                            if (imeVisible) {
                                keyboardController?.hide()
                                scope.launch {
                                    delay(180)
                                    showToolMenu = true
                                }
                            } else {
                                showToolMenu = true
                            }
                        },
                        onClearTool = { selectedTool = null },
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        onSend = ::sendMessage,
                        sendAllowed = !defaultChatModelId.isNullOrBlank(),
                        imeVisible = imeVisible
                    )
                }
            }

            // 底部工具面板（覆盖在输入框上方）
            ToolMenuPanel(
                visible = showToolMenu,
                bottomBarHeight = bottomBarHeightDp,
                modifier = Modifier.zIndex(20f),
                onDismiss = { showToolMenu = false },
                onToolSelect = { tool ->
                    when (tool) {
                        "camera" -> {
                            showToolMenu = false
                            keyboardController?.hide()
                            cameraLauncher.launch(null)
                        }
                        "photos" -> {
                            showToolMenu = false
                            keyboardController?.hide()
                            photoPickerLauncher.launch("image/*")
                        }
                        else -> {
                            selectedTool = tool
                            showToolMenu = false
                        }
                    }
                }
            )

            // 获取当前对话的实时thinking内容
            val currentMessageThinking = localMessages.lastOrNull { it.reasoning != null }?.reasoning
            val displayThinkingText = if (showThinkingSheet && currentMessageThinking != null) {
                currentMessageThinking
            } else thinkingSheetText

            if (showThinkingSheet && !displayThinkingText.isNullOrBlank()) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showThinkingSheet = false
                        thinkingSheetText = null
                    },
                    sheetState = thinkingSheetState,
                    containerColor = ThinkingBackground,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ThinkingBackground)
                                .padding(top = 12.dp, bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(GrayLight, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.67f)
                            .background(ThinkingBackground)
                            .padding(horizontal = 20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 标题 Thinking
                        Text(
                            text = "Thinking",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        // 内容区域
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MarkdownText(
                                markdown = displayThinkingText.orEmpty(),
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: Message,
    conversationId: String?,
    isStreaming: Boolean = false,
    showToolbar: Boolean = true,
    onShowReasoning: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: (conversationId: String, messageId: String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val isUser = message.role == "user"
    if (isUser) {
        // User message (right aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 60.dp)
                    .background(UserMessageBubble, RoundedCornerShape(18.dp))
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { },
                        onLongClick = { showMenu = true }
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                MarkdownText(
                    markdown = message.content,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = TextPrimary
                    )
                )
            }
        }
    } else {
        // Assistant message (left aligned)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { },
                    onLongClick = { showMenu = true }
                )
        ) {
            val reasoningText = message.reasoning?.trim().orEmpty()
            if (reasoningText.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .pressableScale(
                            pressedScale = 0.98f,
                            onClick = { onShowReasoning(reasoningText) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thinking",
                        fontSize = 13.sp,
                        fontFamily = SourceSans3,
                        fontWeight = FontWeight.Medium,
                        color = ThinkingLabelColor
                    )
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = ThinkingLabelColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            MarkdownText(
                markdown = message.content,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = TextPrimary
                )
            )

            if (isStreaming) {
                val infiniteTransition = rememberInfiniteTransition(label = "cursor_pulse")
                val cursorAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "cursor_alpha"
                )
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(8.dp)
                        .background(TextSecondary.copy(alpha = cursorAlpha), CircleShape)
                )
            } else if (showToolbar) {
                // Message action buttons (assistant only)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    ActionButton(
                        icon = AppIcons.Copy,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                        }
                    )
                    ActionButton(icon = AppIcons.Edit, onClick = onEdit)
                    ActionButton(icon = AppIcons.Volume, onClick = { })
                    ActionButton(icon = AppIcons.Share, onClick = { })
                    ActionButton(
                        icon = AppIcons.Refresh,
                        onClick = { /* Regenerate */ }
                    )
                    ActionButton(icon = AppIcons.More, onClick = { showMenu = true })
                }
            }
        }
    }

    // Long-press menu
    if (showMenu) {
        MessageOptionsDialog(
            isUser = isUser,
            onCopy = {
                clipboardManager.setText(AnnotatedString(message.content))
                showMenu = false
            },
            onEdit = {
                onEdit()
                showMenu = false
            },
            onDelete = {
                val convoId = conversationId
                if (!convoId.isNullOrBlank()) {
                    onDelete(convoId, message.id)
                }
                showMenu = false
            },
            onDismiss = { showMenu = false }
        )
    }
}

@Composable
fun MessageOptionsDialog(
    isUser: Boolean,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                DialogOption(text = "Copy", onClick = onCopy)
                if (isUser) {
                    DialogOption(text = "Edit", onClick = onEdit)
                }
                DialogOption(text = "Delete", onClick = onDelete, isDestructive = true)
                DialogOption(text = "Cancel", onClick = onDismiss)
            }
        }
    }
}

@Composable
fun DialogOption(
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressableScale(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            color = if (isDestructive) Color(0xFFFF3B30) else TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyChatState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "What's on your mind today?",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0D0D0D)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SidebarContent(
    conversations: List<Conversation>,
    currentConversationId: String?,
    nickname: String,
    avatarUri: String,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onConversationClick: (Conversation) -> Unit,
    onDeleteConversation: (String) -> Unit,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // 顶部搜索区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 搜索框
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search",
                    color = TextSecondary,
                    fontSize = 15.sp
                )
            }

            // 新建对话按钮
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .pressableScale(pressedScale = 0.95f, onClick = onNewChat),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.NewChat,
                    contentDescription = "New Chat",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 菜单区域
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            SidebarMenuItem(
                icon = { Icon(AppIcons.NewChat, null, Modifier.size(22.dp), TextPrimary) },
                label = "New chat",
                onClick = onNewChat
            )
            SidebarMenuItem(
                icon = { Icon(AppIcons.ChatGPTLogo, null, Modifier.size(22.dp), TextPrimary) },
                label = "Images",
                onClick = { }
            )
            SidebarMenuItem(
                icon = {
                    Icon(
                        painter = rememberResourceDrawablePainter(R.drawable.ic_apps),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.Unspecified
                    )
                },
                label = "Apps",
                onClick = { }
            )
        }

        // 历史记录区域
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            conversations.forEach { conversation ->
                val isSelected = conversation.id == currentConversationId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) GrayLighter else Color.Transparent)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onConversationClick(conversation) },
                            onLongClick = { onDeleteConversation(conversation.id) }
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = conversation.title.ifBlank { "New chat" },
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
            }
        }

        // 底部用户信息
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .pressableScale { navController.navigate("settings") }
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像显示
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GrayLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri.isNotBlank()) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = AppIcons.User,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nickname,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Personal",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SidebarMenuItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pressableScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = label,
            fontSize = 17.sp,
            fontFamily = SourceSans3,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
fun TopNavBar(
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧组
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 汉堡菜单
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
                    .clip(CircleShape)
                    .background(Surface, CircleShape)
                    .pressableScale(pressedScale = 0.95f, onClick = onMenuClick),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(TextPrimary, RoundedCornerShape(1.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .background(TextPrimary, RoundedCornerShape(1.dp))
                    )
                }
            }

            // ChatGPT 标签
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(21.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
                    .background(Surface, RoundedCornerShape(21.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ChatGPT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }

        // 右侧新建对话
        Box(
            modifier = Modifier
                .size(42.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
                .clip(CircleShape)
                .background(Surface, CircleShape)
                .pressableScale(pressedScale = 0.95f, onClick = onNewChatClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.NewChat,
                contentDescription = "New Chat",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .pressableScale(pressedScale = 0.95f, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ActionIcon,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ToolMenuPanel(
    visible: Boolean,
    bottomBarHeight: Dp,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onToolSelect: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = remember(density) { with(density) { 120.dp.toPx() } }

    LaunchedEffect(visible) {
        if (!visible) dragOffsetPx = 0f
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomBarHeight)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
            Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomBarHeight)
                        .offset { IntOffset(0, dragOffsetPx.roundToInt()) }
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                dragOffsetPx = (dragOffsetPx + delta).coerceAtLeast(0f)
                            },
                            onDragStopped = { velocity ->
                                val shouldDismiss =
                                    dragOffsetPx > dismissThresholdPx || velocity > 2400f
                                if (shouldDismiss) {
                                    onDismiss()
                                    dragOffsetPx = 0f
                                } else {
                                    scope.launch {
                                        animate(
                                            initialValue = dragOffsetPx,
                                            targetValue = 0f,
                                            animationSpec = tween(
                                                durationMillis = 180,
                                                easing = LinearEasing
                                            )
                                        ) { value, _ ->
                                            dragOffsetPx = value
                                        }
                                    }
                                }
                            }
                        )
                        .animateEnterExit(
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { }
                        )
                        .zIndex(10f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface)
                            .padding(16.dp)
                    ) {
                    // 拖动条
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(GrayLight, RoundedCornerShape(2.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 顶部三个快捷按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            icon = { Icon(AppIcons.Camera, null, Modifier.size(28.dp), TextPrimary) },
                            label = "Camera",
                            onClick = { onToolSelect("camera") },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            icon = { Icon(AppIcons.ChatGPTLogo, null, Modifier.size(28.dp), TextPrimary) },
                            label = "Photos",
                            onClick = { onToolSelect("photos") },
                            modifier = Modifier.weight(1f)
                        )
                    QuickActionButton(
                        icon = {
                            Icon(
                                painter = rememberResourceDrawablePainter(R.drawable.ic_files),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.Unspecified
                            )
                        },
                        label = "Files",
                        onClick = { onToolSelect("files") },
                        modifier = Modifier.weight(1f)
                    )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = GrayLight, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    // 列表项
                    ToolListItem(
                        icon = { Icon(AppIcons.Globe, null, Modifier.size(24.dp), TextPrimary) },
                        title = "Web search",
                        subtitle = "Find real-time news and info",
                        onClick = { onToolSelect("web") }
                    )
                    ToolListItem(
                        icon = { Icon(AppIcons.CreateImage, null, Modifier.size(24.dp), TextPrimary) },
                        title = "Create image",
                        subtitle = "Visualize anything",
                        onClick = { onToolSelect("image") }
                    )
                    ToolListItem(
                        icon = { Icon(AppIcons.MCPTools, null, Modifier.size(24.dp), TextPrimary) },
                        title = "MCP Tools",
                        subtitle = "Connect external tools",
                        onClick = { onToolSelect("mcp") }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GrayLighter, RoundedCornerShape(16.dp))
            .pressableScale(pressedScale = 0.95f, onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SourceSans3,
            color = TextPrimary
        )
    }
}

private suspend fun handleImageGeneration(
    repository: AppRepository,
    chatApiClient: ChatApiClient,
    safeConversationId: String,
    userPrompt: String,
    assistantMessage: Message,
    updateAssistantContent: (String) -> Unit
) {
    // 显示正在生成的提示
    updateAssistantContent("🎨 Generating image...")

    // 获取生图模型配置
    val imageModelId = repository.defaultImageModelIdFlow.first()
    if (imageModelId.isNullOrBlank()) {
        updateAssistantContent("❌ Please configure Image Model in Settings → Default model before generating images.")
        repository.appendMessage(
            safeConversationId,
            assistantMessage.copy(content = "Please configure Image Model in Settings → Default model before generating images.")
        )
        return
    }

    val allModels = repository.modelsFlow.first()
    val imageModel = allModels.firstOrNull { it.id == imageModelId }
        ?: allModels.firstOrNull { extractRemoteModelId(it.id) == imageModelId }
    if (imageModel == null) {
        updateAssistantContent("❌ Image model not found: $imageModelId. Enable or add it in Models.")
        repository.appendMessage(
            safeConversationId,
            assistantMessage.copy(content = "Image model not found: $imageModelId. Enable or add it in Models.")
        )
        return
    }

    val providerList = repository.providersFlow.first()
    val provider = imageModel.providerId?.let { pid -> providerList.firstOrNull { it.id == pid } }
        ?: providerList.firstOrNull()
    if (provider == null || provider.apiUrl.isBlank() || provider.apiKey.isBlank()) {
        updateAssistantContent("❌ Please add a provider with valid API URL and API Key.")
        repository.appendMessage(
            safeConversationId,
            assistantMessage.copy(content = "Please add a provider with valid API URL and API Key.")
        )
        return
    }

    // 调用图片生成API
    val result = chatApiClient.generateImage(
        provider = provider,
        modelId = extractRemoteModelId(imageModel.id),
        prompt = userPrompt,
        extraHeaders = imageModel.headers,
        size = "1024x1024",
        quality = "standard",
        n = 1
    )

    result.fold(
        onSuccess = { imageUrl ->
            val markdownImage = "![Generated Image]($imageUrl)"
            updateAssistantContent(markdownImage)
            repository.appendMessage(
                safeConversationId,
                assistantMessage.copy(content = markdownImage)
            )
        },
        onFailure = { error ->
            val errorMsg = "❌ Image generation failed: ${error.message ?: error.toString()}"
            updateAssistantContent(errorMsg)
            repository.appendMessage(
                safeConversationId,
                assistantMessage.copy(content = errorMsg)
            )
        }
    )
}

@Composable
fun ToolListItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pressableScale(pressedScale = 0.98f, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontFamily = SourceSans3,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomInputArea(
    selectedTool: String?,
    attachments: List<PendingImageAttachment>,
    onRemoveAttachment: (Int) -> Unit,
    onToolToggle: () -> Unit,
    onClearTool: () -> Unit,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    sendAllowed: Boolean = true,
    imeVisible: Boolean = false
) {
    val hasText = messageText.trim().isNotEmpty()
    val hasAttachments = attachments.isNotEmpty()
    val sendEnabled = (hasText || hasAttachments) && sendAllowed
    val sendBackground = if (sendEnabled) TextPrimary else GrayLight
    val sendIconTint = if (sendEnabled) Color.White else TextSecondary
    val maxTextHeight = if (selectedTool != null) 140.dp else 120.dp
    val maxLines = if (selectedTool != null) 6 else 5
    val toolLabel = when (selectedTool) {
        "files" -> "Files"
        "web" -> "Search"
        "image" -> "Image"
        "mcp" -> "Tools"
        else -> selectedTool?.replaceFirstChar { it.uppercase() }.orEmpty()
    }
    val toolIconRes: Int? = when (selectedTool) {
        "files" -> R.drawable.ic_files
        else -> null
    }
    val toolIconVector = when (selectedTool) {
        "files" -> null
        "web" -> AppIcons.Globe
        "image" -> AppIcons.CreateImage
        "mcp" -> AppIcons.MCPTools
        else -> AppIcons.Globe
    }
    val bottomPadding = if (imeVisible) 4.dp else 24.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp, bottom = bottomPadding)
            .background(ChatBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 工具图标按钮 - 46dp，与输入框等高
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
                    .clip(CircleShape)
                    .background(Surface, CircleShape)
                    .pressableScale(pressedScale = 0.95f, onClick = onToolToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.ChatGPTLogo,
                    contentDescription = "Tools",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 输入框容器 - 默认46dp与工具按钮对齐，可多行扩展；发送按钮固定右下角
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(23.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
                    .background(Surface, RoundedCornerShape(23.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 48.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    if (attachments.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            attachments.forEachIndexed { index, attachment ->
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(GrayLighter),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = attachment.uri ?: attachment.bitmap,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.92f))
                                            .pressableScale(
                                                pressedScale = 0.92f,
                                                onClick = { onRemoveAttachment(index) }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Close,
                                            contentDescription = "Remove",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 选中的工具标签 - 位于输入框内部，出现时顶起输入框高度
                    if (selectedTool != null) {
                        Row(
                            modifier = Modifier
                                .offset(x = (-4).dp)
                                .background(Color(0xFFE8F4FD), RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (toolIconRes != null) {
                                Icon(
                                    painter = rememberResourceDrawablePainter(toolIconRes),
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = toolIconVector ?: AppIcons.Globe,
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = toolLabel,
                                fontSize = 15.sp,
                                color = Color(0xFF007AFF),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.offset(y = (-1).dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .pressableScale(
                                        pressedScale = 0.95f,
                                        onClick = onClearTool
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppIcons.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 输入框 - 多行自动增高，最大高度120dp
                    BasicTextField(
                        value = messageText,
                        onValueChange = onMessageChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 24.dp, max = maxTextHeight),
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(TextPrimary),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { if (sendEnabled) onSend() }
                        ),
                        minLines = 1,
                        maxLines = maxLines,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (messageText.isEmpty()) {
                                    Text(
                                        text = "Ask anything",
                                        fontSize = 17.sp,
                                        lineHeight = 22.sp,
                                        color = TextSecondary
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // 发送按钮 - 固定在右下角（随输入框高度一起上移）
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(sendBackground, CircleShape)
                        .zIndex(2f)
                        .pressableScale(
                            enabled = sendEnabled,
                            pressedScale = 0.95f,
                            onClick = onSend
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Send,
                        contentDescription = "Send",
                        tint = sendIconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private suspend fun encodeImageAttachmentToDataUrl(
    context: Context,
    attachment: PendingImageAttachment,
    maxDimension: Int = 1024,
    jpegQuality: Int = 85
): String? {
    return withContext(Dispatchers.IO) {
        val bitmap =
            attachment.bitmap
                ?: attachment.uri?.let { uri -> decodeBitmapFromUri(context, uri, maxDimension) }
                ?: return@withContext null

        val scaled = scaleBitmapDown(bitmap, maxDimension)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(1, 100), out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        "data:image/jpeg;base64,$b64"
    }
}

private fun decodeBitmapFromUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    val resolver = context.contentResolver
    val bounds =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

    resolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
    val opts =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            this.inSampleSize = inSampleSize
        }

    return resolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, opts)
    }
}

private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    val maxDim = maxOf(width, height)
    if (maxDim <= maxDimension) return 1
    var sample = 1
    while (maxDim / sample > maxDimension) {
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width.coerceAtLeast(1)
    val height = bitmap.height.coerceAtLeast(1)
    val maxDim = maxOf(width, height)
    if (maxDim <= maxDimension) return bitmap
    val scale = maxDimension.toFloat() / maxDim.toFloat()
    val newWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val newHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

private fun buildConversationTranscript(
    messages: List<Message>,
    maxMessages: Int,
    maxCharsPerMessage: Int
): String {
    if (messages.isEmpty()) return ""
    val tail = messages.takeLast(maxMessages)
    return buildString {
        tail.forEach { msg ->
            val prefix =
                when (msg.role) {
                    "user" -> "User"
                    "assistant" -> "Assistant"
                    else -> msg.role.replaceFirstChar { it.uppercase() }
                }
            append(prefix)
            append(": ")
            append(msg.content.replace('\n', ' ').trim().take(maxCharsPerMessage))
            append('\n')
        }
    }.trim()
}

private suspend fun collectStreamContent(
    chatApiClient: ChatApiClient,
    provider: ProviderConfig,
    modelId: String,
    messages: List<Message>,
    extraHeaders: List<HttpHeader>
): String {
    val sb = StringBuilder()
    chatApiClient.chatCompletionsStream(
        provider = provider,
        modelId = modelId,
        messages = messages,
        extraHeaders = extraHeaders
    ).collect { delta ->
        delta.content?.let { sb.append(it) }
    }
    return sb.toString().trim()
}

private fun stripMarkdownCodeFences(text: String): String {
    var t = text.trim()
    if (t.startsWith("```")) {
        t = t.substringAfter('\n', t).trim()
    }
    if (t.endsWith("```")) {
        t = t.dropLast(3).trim()
    }
    return t.trim()
}

private fun parseJsonStringArray(text: String): List<String> {
    val cleaned = stripMarkdownCodeFences(text).trim()
    if (cleaned.isBlank()) return emptyList()

    val candidate = run {
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
    }

    val element = runCatching { JsonParser.parseString(candidate) }.getOrNull() ?: return emptyList()
    val array =
        when {
            element.isJsonArray -> element.asJsonArray
            element.isJsonObject -> element.asJsonObject.getAsJsonArray("memories")
            else -> null
        } ?: return emptyList()

    return array.mapNotNull { el ->
        runCatching { el.asString }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }
}

private suspend fun extractMemoryCandidatesFromTurn(
    chatApiClient: ChatApiClient,
    provider: ProviderConfig,
    modelId: String,
    userText: String,
    assistantText: String,
    extraHeaders: List<HttpHeader>
): List<String> {
    val u = userText.trim()
    val a = assistantText.trim()
    if (u.isBlank() || a.isBlank()) return emptyList()

    val systemPrompt =
        "Extract stable user-specific facts that could help in future conversations. Return ONLY a JSON array of strings. " +
            "If there is nothing worth saving, return []. Do not include temporary requests or task-specific details."

    val userPrompt =
        buildString {
            append("User message:\n")
            append(u.take(900))
            append("\n\nAssistant reply:\n")
            append(a.take(1400))
            append("\n\nReturn JSON array only.")
        }

    val raw =
        collectStreamContent(
            chatApiClient = chatApiClient,
            provider = provider,
            modelId = modelId,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            extraHeaders = extraHeaders
        )

    return parseJsonStringArray(raw)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(3)
}

private suspend fun generateConversationTitle(
    chatApiClient: ChatApiClient,
    provider: ProviderConfig,
    modelId: String,
    transcript: String,
    extraHeaders: List<HttpHeader>
): String? {
    val t = transcript.trim()
    if (t.isBlank()) return null

    val systemPrompt =
        "Generate a concise conversation title (max 6 words). Respond with ONLY the title, no quotes, no punctuation."

    val raw =
        collectStreamContent(
            chatApiClient = chatApiClient,
            provider = provider,
            modelId = modelId,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = t.take(1800))
            ),
            extraHeaders = extraHeaders
        )

    val firstLine = stripMarkdownCodeFences(raw).lineSequence().firstOrNull().orEmpty().trim()
    return firstLine.takeIf { it.isNotBlank() }
}
