package com.zionchat.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    var sidebarVisible by remember { mutableStateOf(false) }
    var toolMenuVisible by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }

    val chatHistory = remember {
        mutableStateListOf(
            "问号信息量分析",
            "AI 小说创作步骤",
            "npm缓存删除指南",
            "无法输出系统提示",
            "便宜.fun域名购买建议",
            "Clawdbot定义与背景",
            "克苏鲁恐怖解析"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Chat Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            // Top Navigation Bar
            TopNavigationBar(
                onMenuClick = { sidebarVisible = true },
                onNewChatClick = { /* Clear chat */ }
            )

            // Chat Messages Area
            ChatMessagesArea(modifier = Modifier.weight(1f))

            // Bottom Input Area
            BottomInputArea(
                selectedTool = selectedTool,
                onToolClick = { toolMenuVisible = true },
                onClearTool = { selectedTool = null },
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        messageText = ""
                    }
                }
            )
        }

        // Sidebar Overlay
        if (sidebarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable { sidebarVisible = false }
            )
        }

        // Sidebar
        AnimatedVisibility(
            visible = sidebarVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Sidebar(
                chatHistory = chatHistory,
                onNewChat = {
                    /* Clear chat */
                    sidebarVisible = false
                },
                onChatClick = { sidebarVisible = false },
                onSettingsClick = {
                    sidebarVisible = false
                    navController.navigate("settings")
                },
                onDismiss = { sidebarVisible = false }
            )
        }

        // Tool Menu Overlay
        if (toolMenuVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable { toolMenuVisible = false }
            )
        }

        // Tool Menu Bottom Sheet
        AnimatedVisibility(
            visible = toolMenuVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ToolMenuBottomSheet(
                onToolSelect = { tool ->
                    selectedTool = tool
                    toolMenuVisible = false
                },
                onDismiss = { toolMenuVisible = false }
            )
        }
    }
}

@Composable
fun TopNavigationBar(
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
        // Left group - Menu and ChatGPT label
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger Menu Button
            Button(
                onClick = onMenuClick,
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Surface
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
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

            // ChatGPT Label
            Button(
                onClick = { },
                modifier = Modifier.height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Surface
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Text(
                    text = "ChatGPT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    letterSpacing = (-0.3).sp
                )
            }
        }

        // Right - New Chat Button
        Button(
            onClick = onNewChatClick,
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Surface
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "New Chat",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun ChatMessagesArea(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User message
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .background(GrayLighter, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "哨",
                        fontSize = 16.sp,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // AI Response
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "……在，在的，别敲了，再敲我就假装离线了。",
                    fontSize = 16.sp,
                    color = TextPrimary,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MessageActionButton(onClick = { })
                    MessageActionButton(onClick = { })
                    MessageActionButton(onClick = { })
                    MessageActionButton(onClick = { })
                    MessageActionButton(onClick = { })
                }
            }
        }
    }
}

@Composable
fun MessageActionButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color(0xFF374151), CircleShape)
        )
    }
}

@Composable
fun BottomInputArea(
    selectedTool: String?,
    onToolClick: () -> Unit,
    onClearTool: () -> Unit,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp, top = 8.dp)
            .background(Background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Tool Toggle Button
            Button(
                onClick = onToolClick,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Surface
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tools",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Input Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Surface, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column {
                    // Selected Tool Tag
                    if (selectedTool != null) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .background(SelectedToolBackground, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(AccentBlue, CircleShape)
                            )
                            Text(
                                text = selectedTool,
                                fontSize = 15.sp,
                                color = AccentBlue,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = onClearTool,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Clear",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Text Input
                    BasicTextField(
                        value = messageText,
                        onValueChange = onMessageChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 40.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 17.sp,
                            color = TextPrimary,
                            lineHeight = 22.sp
                        ),
                        decorationBox = { innerTextField ->
                            if (messageText.isEmpty()) {
                                Text(
                                    text = "Message...",
                                    fontSize = 17.sp,
                                    color = TextSecondary
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // Send Button
                Button(
                    onClick = onSend,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextPrimary,
                        disabledContainerColor = TextPrimary.copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(0.dp),
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Send",
                        tint = Surface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Sidebar(
    chatHistory: List<String>,
    onNewChat: () -> Unit,
    onChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        color = Surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Search Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Box
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(GrayLighter, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Search",
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                }

                // New Chat Button
                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New Chat",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Menu Items
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SidebarMenuItem(
                    icon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp)) },
                    label = "New chat",
                    onClick = onNewChat
                )
                SidebarMenuItem(
                    icon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp)) },
                    label = "Images",
                    onClick = { }
                )
                SidebarMenuItem(
                    icon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp)) },
                    label = "Apps",
                    onClick = { }
                )
            }

            // Chat History
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(chatHistory) { chat ->
                    SidebarHistoryItem(
                        title = chat,
                        onClick = onChatClick
                    )
                }
            }

            // User Profile
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable { onSettingsClick() },
                color = GrayLighter,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(GrayLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "User",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                        imageVector = Icons.Default.Add,
                        contentDescription = "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
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
fun SidebarHistoryItem(
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (title == "问号信息量分析") GrayLighter else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ToolMenuBottomSheet(
    onToolSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(GrayLight, RoundedCornerShape(2.dp))
                )
            }

            // Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = "📷",
                    label = "Camera",
                    onClick = { onToolSelect("Camera") }
                )
                QuickActionButton(
                    icon = "🖼",
                    label = "Photos",
                    onClick = { onToolSelect("Photos") }
                )
                QuickActionButton(
                    icon = "📄",
                    label = "Files",
                    onClick = { onToolSelect("Files") }
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = GrayLight
            )

            // Tool List
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ToolListItem(
                    icon = "🌐",
                    title = "Web search",
                    subtitle = "Find real-time news and info",
                    onClick = { onToolSelect("Web search") }
                )
                ToolListItem(
                    icon = "🎨",
                    title = "Create image",
                    subtitle = "Visualize anything",
                    onClick = { onToolSelect("Create image") }
                )
                ToolListItem(
                    icon = "🔧",
                    title = "MCP Tools",
                    subtitle = "Connect external tools",
                    onClick = { onToolSelect("MCP Tools") }
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(GrayLighter, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = icon,
            fontSize = 28.sp
        )
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
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.size(36.dp)
        )
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

// BasicTextField placeholder workaround
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        decorationBox = decorationBox
    )
}
