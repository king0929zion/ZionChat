package com.zionchat.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.zionchat.app.ui.icons.AppIcons
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

// 消息数据类
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

// 历史记录数据类
data class ChatHistory(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val lastMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showToolMenu by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // 消息列表状态
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    content = "哈喽",
                    isUser = true
                ),
                ChatMessage(
                    content = "……在，在的，别敲了，再敲我就假装离线了。",
                    isUser = false
                )
            )
        )
    }

    // 历史记录状态
    var chatHistory by remember {
        mutableStateOf(
            listOf(
                ChatHistory(title = "问号信息量分析", lastMessage = "这是一个测试消息"),
                ChatHistory(title = "AI 小说创作步骤", lastMessage = "你好"),
                ChatHistory(title = "npm缓存删除指南", lastMessage = "如何删除缓存？"),
                ChatHistory(title = "无法输出系统提示", lastMessage = "系统错误"),
                ChatHistory(title = "便宜.fun域名购买建议", lastMessage = "推荐一些域名"),
                ChatHistory(title = "Clawdbot定义与背景", lastMessage = "什么是Clawdbot"),
                ChatHistory(title = "克苏鲁恐怖解析", lastMessage = "恐怖小说分析")
            )
        )
    }

    val listState = rememberLazyListState()

    // 滚动到底部当新消息添加时
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 发送消息功能
    fun sendMessage() {
        if (messageText.isBlank()) return

        val userMessage = ChatMessage(
            content = messageText.trim(),
            isUser = true
        )
        messages = messages + userMessage
        messageText = ""
        isLoading = true

        // 模拟AI回复
        scope.launch {
            kotlinx.coroutines.delay(1000)
            val aiResponse = when {
                userMessage.content.contains("哈喽") || userMessage.content.contains("你好") ->
                    "你好！有什么我可以帮助你的吗？"
                userMessage.content.contains("?") || userMessage.content.contains("？") ->
                    "这是一个很好的问题！让我来为你解答..."
                else -> "收到！我会尽力帮助你解决${userMessage.content}相关的问题。"
            }
            messages = messages + ChatMessage(
                content = aiResponse,
                isUser = false
            )
            isLoading = false
        }
    }

    // 新建对话
    fun startNewChat() {
        messages = emptyList()
        scope.launch { drawerState.close() }
    }

    // 删除历史记录
    fun deleteHistory(historyId: String) {
        chatHistory = chatHistory.filter { it.id != historyId }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarContent(
                chatHistory = chatHistory,
                onClose = { scope.launch { drawerState.close() } },
                onNewChat = ::startNewChat,
                onHistoryClick = { /* 加载历史对话 */ },
                onDeleteHistory = ::deleteHistory,
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
                modifier = Modifier.fillMaxSize()
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
                        .padding(bottom = 80.dp)
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
                                    onCopy = { /* 复制 */ },
                                    onEdit = { /* 编辑 */ },
                                    onDelete = {
                                        messages = messages.filter { it.id != message.id }
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

            // 底部输入框区域 - 固定在底部
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                // 底部工具面板（在输入框上方）
                if (showToolMenu) {
                    ToolMenuPanel(
                        onDismiss = { showToolMenu = false },
                        onToolSelect = { tool ->
                            selectedTool = tool
                            showToolMenu = false
                        }
                    )
                }

                BottomInputArea(
                    selectedTool = selectedTool,
                    onToolToggle = { showToolMenu = !showToolMenu },
                    onClearTool = { selectedTool = null },
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    onSend = ::sendMessage
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: ChatMessage,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    if (message.isUser) {
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
            isUser = message.isUser,
            onCopy = {
                clipboardManager.setText(AnnotatedString(message.content))
                showMenu = false
            },
            onEdit = {
                onEdit()
                showMenu = false
            },
            onDelete = {
                onDelete()
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
            .clickable(onClick = onClick)
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
    chatHistory: List<ChatHistory>,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onHistoryClick: (ChatHistory) -> Unit,
    onDeleteHistory: (String) -> Unit,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Surface)
            .padding(vertical = 8.dp)
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
                    .clickable { onNewChat() },
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
                icon = { Icon(AppIcons.Apps, null, Modifier.size(20.dp), TextPrimary) },
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
            chatHistory.forEachIndexed { index, history ->
                val isSelected = index == 0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) GrayLighter else Color.Transparent)
                        .combinedClickable(
                            onClick = { onHistoryClick(history) },
                            onLongClick = { onDeleteHistory(history.id) }
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = history.title,
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
                .clickable { navController.navigate("settings") }
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
            .clickable(onClick = onClick)
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
                    .background(Surface, CircleShape)
                    .clickable(onClick = onMenuClick),
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
                .background(Surface, CircleShape)
                .clickable(onClick = onNewChatClick),
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
            .clickable(onClick = onClick)
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

@Composable
fun ToolMenuPanel(
    onDismiss: () -> Unit,
    onToolSelect: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .clickable(onClick = onDismiss)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
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
                        icon = { Icon(AppIcons.Files, null, Modifier.size(28.dp), TextPrimary) },
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
            .clickable(onClick = onClick)
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
            .clickable(onClick = onClick)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 工具图标按钮 - 44dp 高度
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Surface, CircleShape)
                    .clickable(onClick = onToolToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.ChatGPTLogo,
                    contentDescription = "Tools",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 输入框容器 - 高度与工具按钮一致 (44dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(Surface, RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 选中的工具标签 - 根据HTML样式：rounded-full (圆角胶囊形状), 高度匹配
                    if (selectedTool != null) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE8F4FD), RoundedCornerShape(50.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .heightIn(min = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Globe,
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = selectedTool.replaceFirstChar { it.uppercase() },
                                    fontSize = 14.sp,
                                    color = Color(0xFF007AFF),
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = AppIcons.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onClearTool() }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // 输入框
                    TextField(
                        value = messageText,
                        onValueChange = onMessageChange,
                        placeholder = { Text("Message...", fontSize = 16.sp) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                    )

                    // 发送按钮
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (messageText.isNotBlank()) TextPrimary else GrayLight,
                                CircleShape
                            )
                            .clickable(
                                enabled = messageText.isNotBlank(),
                                onClick = onSend
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}