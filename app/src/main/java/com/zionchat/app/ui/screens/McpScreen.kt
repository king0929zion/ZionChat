package com.zionchat.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.zionchat.app.LocalAppRepository
import com.zionchat.app.data.HttpHeader
import com.zionchat.app.data.McpClient
import com.zionchat.app.data.McpConfig
import com.zionchat.app.data.McpProtocol
import com.zionchat.app.ui.components.PageTopBar
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpScreen(navController: NavController) {
    val repository = LocalAppRepository.current
    val scope = rememberCoroutineScope()
    val mcpClient = remember { McpClient() }
    
    val mcpList by repository.mcpListFlow.collectAsState(initial = emptyList())
    
    var showAddModal by remember { mutableStateOf(false) }
    var editingMcp by remember { mutableStateOf<McpConfig?>(null) }
    
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageTopBar(
                title = "MCP Tools",
                onBack = { navController.navigateUp() },
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Surface, CircleShape)
                            .pressableScale(pressedScale = 0.95f) {
                                editingMcp = null
                                showAddModal = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Plus,
                            contentDescription = "Add",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )
            
            if (mcpList.isEmpty()) {
                McpEmptyState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    mcpList.forEach { mcp ->
                        McpListItem(
                            mcp = mcp,
                            onClick = { navController.navigate("mcp_detail/${mcp.id}") },
                            onToggle = {
                                scope.launch {
                                    repository.toggleMcpEnabled(mcp.id)
                                }
                            }
                        )
                    }
                }
            }
        }
        
        // Full screen modal with scrim covering status bar
        if (showAddModal) {
            FullScreenModal(
                onDismiss = { showAddModal = false }
            ) {
                McpEditSheetContent(
                    mcp = editingMcp,
                    onDismiss = { showAddModal = false },
                    onSave = { mcp ->
                        scope.launch {
                            repository.upsertMcp(mcp)
                            runCatching {
                                val tools = mcpClient.fetchTools(mcp).getOrNull()
                                if (tools != null) {
                                    repository.updateMcpTools(mcp.id, tools)
                                }
                            }
                            showAddModal = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun McpEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GrayLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.MCPTools,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No MCP Tools",
            fontSize = 17.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Tap + to add your first MCP",
            fontSize = 14.sp,
            color = TextSecondary.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun McpListItem(
    mcp: McpConfig,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        color = GrayLight,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GrayLighter),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (mcp.protocol == McpProtocol.HTTP) AppIcons.Globe else AppIcons.Stream,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = mcp.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (mcp.enabled) TextPrimary else TextSecondary
                )
                Text(
                    text = mcp.protocol.name,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            
            McpToggleSwitch(
                enabled = mcp.enabled,
                onToggle = onToggle
            )
        }
    }
}

@Composable
fun McpToggleSwitch(
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val thumbPosition by animateFloatAsState(
        targetValue = if (enabled) 20f else 2f,
        label = "toggle_thumb"
    )
    
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) TextPrimary else Color.Transparent)
            .border(2.dp, if (enabled) TextPrimary else TextSecondary, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    translationX = thumbPosition
                }
                .clip(CircleShape)
                .background(if (enabled) Color.White else TextSecondary)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpEditSheetContent(
    mcp: McpConfig?,
    onDismiss: () -> Unit,
    onSave: (McpConfig) -> Unit
) {
    val isEditing = mcp != null
    
    var name by remember { mutableStateOf(mcp?.name ?: "") }
    var url by remember { mutableStateOf(mcp?.url ?: "") }
    var description by remember { mutableStateOf(mcp?.description ?: "") }
    var protocol by remember { mutableStateOf(mcp?.protocol ?: McpProtocol.HTTP) }
    var headers by remember { mutableStateOf(mcp?.headers ?: emptyList()) }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(bottom = 24.dp)
            .navigationBarsPadding()
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit MCP" else "Add MCP",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SourceSans3,
                    color = TextPrimary
                )
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GrayLighter)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Protocol Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Protocol",
                    fontSize = 13.sp,
                    fontFamily = SourceSans3,
                    color = TextSecondary
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GrayLighter, RoundedCornerShape(20.dp))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProtocolOption(
                        text = "HTTP",
                        selected = protocol == McpProtocol.HTTP,
                        onClick = { protocol = McpProtocol.HTTP },
                        modifier = Modifier.weight(1f)
                    )
                    ProtocolOption(
                        text = "SSE",
                        selected = protocol == McpProtocol.SSE,
                        onClick = { protocol = McpProtocol.SSE },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Name input
            McpConfigInputField(
                label = "Name",
                value = name,
                onValueChange = { name = it },
                placeholder = "Enter MCP name"
            )
            
            // URL input
            McpConfigInputField(
                label = "Server URL",
                value = url,
                onValueChange = { url = it },
                placeholder = "https://api.example.com/mcp"
            )
            
            // Description input
            McpConfigInputField(
                label = "Description (Optional)",
                value = description,
                onValueChange = { description = it },
                placeholder = "Brief description"
            )
            
            // Headers section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Surface,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Headers",
                            fontSize = 13.sp,
                            fontFamily = SourceSans3,
                            color = TextSecondary
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GrayLighter, CircleShape)
                                .pressableScale(pressedScale = 0.95f) {
                                    headers = headers + HttpHeader("", "")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.Plus,
                                contentDescription = "Add Header",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    if (headers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No custom headers",
                                fontSize = 14.sp,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        headers.forEachIndexed { index, header ->
                            HeaderInputRow(
                                key = header.key,
                                value = header.value,
                                onKeyChange = { newKey ->
                                    headers = headers.toMutableList().apply {
                                        this[index] = header.copy(key = newKey)
                                    }
                                },
                                onValueChange = { newValue ->
                                    headers = headers.toMutableList().apply {
                                        this[index] = header.copy(value = newValue)
                                    }
                                },
                                onDelete = {
                                    headers = headers.filterIndexed { i, _ -> i != index }
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayLight,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                Button(
                    onClick = {
                        if (name.isNotBlank() && url.isNotBlank()) {
                            onSave(
                                McpConfig(
                                    id = mcp?.id ?: java.util.UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    url = url.trim(),
                                    protocol = protocol,
                                    enabled = mcp?.enabled ?: true,
                                    description = description.trim(),
                                    headers = headers.filter { it.key.isNotBlank() && it.value.isNotBlank() }
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = name.isNotBlank() && url.isNotBlank()
                ) {
                    Text(
                        text = "Save",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProtocolOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) TextPrimary else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else TextPrimary
        )
    }
}

@Composable
fun McpConfigInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontFamily = SourceSans3,
            color = TextSecondary
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Surface,
            shape = RoundedCornerShape(20.dp)
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                placeholder = {
                    Text(
                        text = placeholder,
                        fontSize = 17.sp,
                        color = TextSecondary
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    color = TextPrimary
                ),
                singleLine = true
            )
        }
    }
}

@Composable
fun HeaderInputRow(
    key: String,
    value: String,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            color = GrayLighter,
            shape = RoundedCornerShape(12.dp)
        ) {
            TextField(
                value = key,
                onValueChange = onKeyChange,
                modifier = Modifier.padding(horizontal = 4.dp),
                placeholder = {
                    Text(
                        text = "Key",
                        fontSize = 15.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = TextPrimary
                ),
                singleLine = true
            )
        }
        
        Surface(
            modifier = Modifier.weight(1f),
            color = GrayLighter,
            shape = RoundedCornerShape(12.dp)
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.padding(horizontal = 4.dp),
                placeholder = {
                    Text(
                        text = "Value",
                        fontSize = 15.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = TextPrimary
                ),
                singleLine = true
            )
        }
        
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GrayLighter)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDelete
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = TextSecondary
            )
        }
    }
}
