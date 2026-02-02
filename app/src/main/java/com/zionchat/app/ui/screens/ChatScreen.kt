package com.zionchat.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.zionchat.app.ui.components.rememberResourceDrawablePainter
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 颜色常量 - 完全匹配HTML原型
val Background = Color(0xFFF5F5F7)
val Surface = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1C1C1E)
val TextSecondary = Color(0xFF8E8E93)
val GrayLight = Color(0xFFE5E5EA)
val GrayLighter = Color(0xFFF2F2F7)
val ActionIcon = Color(0xFF374151)
val ToggleActive = Color(0xFF34C759)
val UserMessageBubble = Color(0xFFE5E5EA)

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
    var isLoading by remember { mutableStateOf(false) }

    val conversations by repository.conversationsFlow.collectAsState(initial = emptyList())
    val providers by repository.providersFlow.collectAsState(initial = emptyList())
    val models by repository.modelsFlow.collectAsState(initial = emptyList())
    val currentConversationId by repository.currentConversationIdFlow.collectAsState(initial = null)

    val currentConversation = remember(conversations, currentConversationId) {
        conversations.firstOrNull { it.id == currentConversationId } ?: conversations.firstOrNull()
    }

    LaunchedEffect(conversations, currentConversationId) {
        if (currentConversationId.isNullOrBlank()) {
            if (conversations.isNotEmpty()) {
                repository.setCurrentConversationId(conversations.first().id)
            } else {
                repository.createConversation()
            }
        }
    }

    val messages = currentConversation?.messages.orEmpty()

    val listState = rememberLazyListState()
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val bottomBarHeightDp = with(LocalDensity.current) { bottomBarHeightPx.toDp() }
    val bottomContentPadding = maxOf(80.dp, bottomBarHeightDp + 12.dp)
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // 滚动到底部当新消息添加时
    LaunchedEffect(messages.size, currentConversation?.id) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun startNewChat() {
        scope.launch {
            repository.createConversation()
            selectedTool = null
            messageText = ""
            drawerState.close()
        }
    }

    fun sendMessage() {
        val trimmed = messageText.trim()
        if (trimmed.isEmpty() || isLoading) return

        scope.launch {
            val conversationId = currentConversation?.id ?: repository.createConversation().id
            val existingConversations = repository.conversationsFlow.first()
            val conversation = existingConversations.firstOrNull { it.id == conversationId }

            val userMessage = Message(role = "user", content = trimmed)
            repository.appendMessage(conversationId, userMessage)
            messageText = ""

            if (conversation?.title == "New chat") {
                val title = trimmed.lineSequence().firstOrNull().orEmpty().trim().take(24)
                if (title.isNotBlank()) {
                    repository.updateConversation(conversation.copy(title = title))
                }
            }

            val provider = repository.providersFlow.first().firstOrNull()
            val selectedModel = repository.modelsFlow.first().firstOrNull { it.enabled }
            val modelId = selectedModel?.id ?: "gpt-4o"
            if (provider == null) {
                repository.appendMessage(
                    conversationId,
                    Message(
                        role = "assistant",
                        content = "请先在 Settings → Model services 添加供应商，并填写 API URL 与 API Key。"
                    )
                )
                return@launch
            }

            val requestMessages = (conversation?.messages.orEmpty()) + userMessage
            isLoading = true
            val result = chatApiClient.chatCompletions(
                provider = provider,
                modelId = modelId,
                messages = requestMessages,
                extraHeaders = selectedModel?.headers.orEmpty()
            )
            val reply = result.getOrElse { throwable ->
                "请求失败：${throwable.message ?: throwable.toString()}"
            }.ifBlank { "（空响应）" }
            repository.appendMessage(conversationId, Message(role = "assistant", content = reply))
            isLoading = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarContent(
                conversations = conversations,
                currentConversationId = currentConversation?.id,
                onClose = { scope.launch { drawerState.close() } },
                onNewChat = ::startNewChat,
                onConversationClick = { convo ->
                    scope.launch {
                        repository.setCurrentConversationId(convo.id)
                        drawerState.close()
                    }
                },
                onDeleteConversation = { id ->
                    scope.launch { repository.deleteConversation(id) }
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
                    if (messages.isEmpty()) {
                        // 空状态
                        EmptyChatState()
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(messages, key = { it.id }) { message ->
                                MessageItem(
                                    message = message,
                                    conversationId = currentConversation?.id,
                                    onEdit = { /* TODO: 编辑消息 */ },
                                    onDelete = { convoId, messageId ->
                                        scope.launch { repository.deleteMessage(convoId, messageId) }
                                    }
                                )
                            }

                            // 加载指示器
                            if (isLoading) {
                                item {
                                    LoadingIndicator()
                                }
                            }
                        }
                    }
                }
            }

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
                    onSend = ::sendMessage
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
            Text(
                text = message.content,
                fontSize = 16.sp,
                color = TextPrimary,
                lineHeight = 24.sp
            )

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
                DialogOption(text = "复制", onClick = onCopy)
                if (isUser) {
                    DialogOption(text = "编辑", onClick = onEdit)
                }
                DialogOption(text = "删除", onClick = onDelete, isDestructive = true)
                DialogOption(text = "取消", onClick = onDismiss)
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
fun LoadingIndicator() {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = TextSecondary
        )
        Text(
            text = "思考中...",
            fontSize = 14.sp,
            color = TextSecondary
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = AppIcons.ChatGPTLogo,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "开始新的对话",
                fontSize = 17.sp,
                color = TextSecondary
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
    onSend: () -> Unit
) {
    val canSend = messageText.trim().isNotEmpty()
    val sendAlpha = if (canSend) 1f else 0.4f
    val sendBackground = TextPrimary.copy(alpha = sendAlpha)
    val inputMinHeight = if (selectedTool != null) 68.dp else 44.dp
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
    val isSingleLineLike = messageText.isEmpty() || (!messageText.contains('\n') && messageText.length < 60)
    val inputAlignment = if (isSingleLineLike) Alignment.CenterStart else Alignment.TopStart

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp, bottom = 20.dp)
            .background(Background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 工具图标按钮 - 44dp，高度对齐输入框底部
            Box(
                modifier = Modifier
                    .size(44.dp)
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

            // 输入框容器 - 默认48dp，可多行扩展；发送按钮固定右下角
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = inputMinHeight)
                    .background(Surface, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 60.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    // 选中的工具标签 - 位于输入框内部，出现时顶起输入框高度
                    if (selectedTool != null) {
                        Row(
                            modifier = Modifier
                                .offset(x = (-12).dp)
                                .background(Color(0xFFE8F4FD), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (toolIconRes != null) {
                                Icon(
                                    painter = rememberResourceDrawablePainter(toolIconRes),
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = toolIconVector ?: AppIcons.Globe,
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(20.dp)
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
                            .heightIn(min = 32.dp, max = 120.dp),
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(TextPrimary),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { if (canSend) onSend() }
                        ),
                        minLines = 1,
                        maxLines = 5,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = inputAlignment
                            ) {
                                if (messageText.isEmpty()) {
                                    Text(
                                        text = "Message...",
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
                            enabled = canSend,
                            pressedScale = 0.95f,
                            onClick = onSend
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Send,
                        contentDescription = "Send",
                        tint = Color.White.copy(alpha = sendAlpha),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
