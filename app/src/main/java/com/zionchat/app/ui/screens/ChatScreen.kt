package com.zionchat.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.zionchat.app.R
import com.zionchat.app.LocalAppRepository
import com.zionchat.app.LocalChatApiClient
import com.zionchat.app.data.Conversation
import com.zionchat.app.data.Message
import com.zionchat.app.data.extractRemoteModelId
import com.zionchat.app.ui.components.TopFadeScrim
import com.zionchat.app.ui.components.BottomFadeScrim
import com.zionchat.app.ui.components.MarkdownText
import com.zionchat.app.ui.components.rememberResourceDrawablePainter
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import kotlin.math.roundToInt

// 颜色常量 - 完全匹配HTML原型
private data class PendingMessage(val conversationId: String, val message: Message)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    val repository = LocalAppRepository.current
    val chatApiClient = LocalChatApiClient.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var showToolMenu by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }

    val conversations by repository.conversationsFlow.collectAsState(initial = emptyList())
    val currentConversationId by repository.currentConversationIdFlow.collectAsState(initial = null)
    val nickname by repository.nicknameFlow.collectAsState(initial = "")
    val customInstructions by repository.customInstructionsFlow.collectAsState(initial = "")
    val defaultChatModelId by repository.defaultChatModelIdFlow.collectAsState(initial = null)

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
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val bottomBarHeightDp = with(LocalDensity.current) { bottomBarHeightPx.toDp() }
    val bottomContentPadding = maxOf(80.dp, bottomBarHeightDp + 12.dp)
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

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
        if (trimmed.isEmpty() || isStreaming) return

        scope.launch {
            val nowMs = System.currentTimeMillis()
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

            val userMessage = Message(role = "user", content = trimmed)
            pendingMessages = pendingMessages + PendingMessage(safeConversationId, userMessage)
            messageText = ""
            scrollToBottomToken++
            repository.appendMessage(safeConversationId, userMessage)

            // Update conversation title only using the latest persisted conversation to avoid wiping messages.
            val latestConversationForTitle =
                repository.conversationsFlow.first().firstOrNull { it.id == safeConversationId }
            if (
                latestConversationForTitle != null &&
                    (latestConversationForTitle.title.isBlank() || latestConversationForTitle.title == "New chat")
            ) {
                val title = trimmed.lineSequence().firstOrNull().orEmpty().trim().take(24)
                if (title.isNotBlank()) {
                    repository.updateConversation(latestConversationForTitle.copy(title = title))
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

            val allModels = repository.modelsFlow.first()
            val selectedModel =
                allModels.firstOrNull { it.id == latestDefaultChatModelId }
                    ?: allModels.firstOrNull { extractRemoteModelId(it.id) == latestDefaultChatModelId }
            if (selectedModel == null) {
                repository.appendMessage(
                    safeConversationId,
                    Message(
                        role = "assistant",
                        content = "Default chat model not found: $latestDefaultChatModelId. Enable or add it in Models, then re-select it in Settings → Default model."
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
                val content = buildString {
                    if (latestNickname.isNotBlank()) {
                        append("Nickname: ")
                        append(latestNickname)
                    }
                    if (latestInstructions.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append(latestInstructions)
                    }
                }.trim()
                if (content.isBlank()) null else Message(role = "system", content = content)
            }

            // 获取最新的消息列表
            val refreshedConversations = repository.conversationsFlow.first()
            val latestConversation = refreshedConversations.firstOrNull { it.id == safeConversationId }
            val currentMessages = latestConversation?.messages.orEmpty()

            val requestMessages = buildList {
                if (systemMessage != null) add(systemMessage)
                addAll(currentMessages)
                if (currentMessages.lastOrNull()?.id != userMessage.id) add(userMessage)
            }

            // 流式输出
            val assistantMessage = Message(role = "assistant", content = "")
            pendingMessages = pendingMessages + PendingMessage(safeConversationId, assistantMessage)
            isStreaming = true
            streamingMessageId = assistantMessage.id
            streamingConversationId = safeConversationId
            scrollToBottomToken++

            fun updateAssistantContent(content: String) {
                pendingMessages =
                    pendingMessages.map { pending ->
                        if (pending.conversationId == safeConversationId && pending.message.id == assistantMessage.id) {
                            pending.copy(message = pending.message.copy(content = content))
                        } else {
                            pending
                        }
                    }
            }

            try {
                val fullContent = StringBuilder()
                var lastUiUpdateMs = 0L

                chatApiClient.chatCompletionsStream(
                    provider = provider,
                    modelId = extractRemoteModelId(selectedModel.id),
                    messages = requestMessages,
                    extraHeaders = selectedModel.headers
                ).collect { chunk ->
                    fullContent.append(chunk)
                    val now = System.currentTimeMillis()
                    if (now - lastUiUpdateMs >= 33L || fullContent.length % 80 == 0) {
                        updateAssistantContent(fullContent.toString())
                        lastUiUpdateMs = now
                    }
                }

                val finalContent = fullContent.toString()
                updateAssistantContent(finalContent)
                if (finalContent.isNotBlank()) {
                    repository.appendMessage(safeConversationId, assistantMessage.copy(content = finalContent))
                }
            } catch (e: Exception) {
                val errorMsg = "Request failed: ${e.message ?: e.toString()}"
                updateAssistantContent(errorMsg)
                repository.appendMessage(safeConversationId, assistantMessage.copy(content = errorMsg))
            } finally {
                isStreaming = false
                streamingMessageId = null
                streamingConversationId = null
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarContent(
                conversations = conversations,
                currentConversationId = effectiveConversationId,
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
                .background(Background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // 顶部导航栏
                TopNavBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNewChatClick = ::startNewChat
                )

                // 聊天内容区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = bottomContentPadding)
                ) {
                    if (localMessages.isEmpty()) {
                        // 空状态
                        EmptyChatState()
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(localMessages, key = { it.id }) { message ->
                                MessageItem(
                                    message = message,
                                    conversationId = effectiveConversationId,
                                    isStreaming = isStreaming
                                        && streamingConversationId == effectiveConversationId
                                        && message.id == streamingMessageId,
                                    onEdit = { /* TODO: 编辑消息 */ },
                                    onDelete = { convoId, messageId ->
                                        scope.launch { repository.deleteMessage(convoId, messageId) }
                                    }
                                )
                            }
                        }
                    }

                    BottomFadeScrim(
                        color = Background,
                        height = 52.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f)
                    )
                }
            }

            // 顶部渐变遮罩 - 放在最外层，覆盖在导航栏下方
            TopFadeScrim(
                color = Background,
                height = 72.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp) // 导航栏高度约72dp (状态栏+TopNavBar)
                    .zIndex(2f)
            )

            // 底部输入框区域 - 固定在底部（高度动态，支持多行/工具标签）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomBarHeightPx = it.height }
            ) {
                BottomInputArea(
                    selectedTool = selectedTool,
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

            // 底部工具面板（覆盖在输入框上方）
            ToolMenuPanel(
                visible = showToolMenu,
                onDismiss = { showToolMenu = false },
                onToolSelect = { tool ->
                    selectedTool = tool
                    showToolMenu = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: Message,
    conversationId: String?,
    isStreaming: Boolean = false,
    onEdit: () -> Unit,
    onDelete: (conversationId: String, messageId: String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val isUser = message.role == "user"
    if (isUser) {
        // 用户消息 - 右对齐
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
                Text(
                    text = message.content,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
        }
    } else {
        // AI消息 - 左对齐
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
            } else {
                // 工具栏
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        onClick = { /* 重新生成 */ }
                    )
                    ActionButton(icon = AppIcons.More, onClick = { showMenu = true })
                }
            }
        }
    }

    // 长按菜单
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
                    .background(GrayLight, RoundedCornerShape(20.dp))
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
                icon = { Icon(AppIcons.NewChat, null, Modifier.size(20.dp), TextPrimary) },
                label = "New chat",
                onClick = onNewChat
            )
            SidebarMenuItem(
                icon = { Icon(AppIcons.ChatGPTLogo, null, Modifier.size(20.dp), TextPrimary) },
                label = "Images",
                onClick = { }
            )
            SidebarMenuItem(
                icon = {
                    Icon(
                        painter = rememberResourceDrawablePainter(R.drawable.ic_apps),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(GrayLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.User,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kendall Williamson",
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
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = label,
            fontSize = 16.sp,
            fontFamily = SourceSans3,
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
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
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
                        ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
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
    onToolToggle: () -> Unit,
    onClearTool: () -> Unit,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    sendAllowed: Boolean = true,
    imeVisible: Boolean = false
) {
    val hasText = messageText.trim().isNotEmpty()
    val sendEnabled = hasText && sendAllowed
    val sendBackground = if (sendEnabled) TextPrimary else GrayLight
    val sendIconTint = if (sendEnabled) Color.White else TextSecondary
    val maxTextHeight = if (selectedTool != null) 140.dp else 120.dp
    val maxLines = if (selectedTool != null) 6 else 5
    val toolLabel = when (selectedTool) {
        "camera" -> "Camera"
        "photos" -> "Photos"
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
        "camera" -> AppIcons.Camera
        "photos" -> AppIcons.ChatGPTLogo
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
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp, bottom = bottomPadding)
            .background(Background)
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
                    .background(Surface, RoundedCornerShape(23.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 48.dp, top = 8.dp, bottom = 8.dp)
                ) {
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
