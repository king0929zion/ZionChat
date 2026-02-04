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
import androidx.navigation.NavController
import com.zionchat.app.LocalAppRepository
import com.zionchat.app.data.HttpHeader
import com.zionchat.app.data.McpClient
import com.zionchat.app.data.McpConfig
import com.zionchat.app.data.McpProtocol
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
    var deleteMcpId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            McpTopBar(
                onBackClick = { navController.navigateUp() },
                onAddClick = { 
                    editingMcp = null
                    showAddModal = true 
                }
            )
        },
        containerColor = Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (mcpList.isEmpty()) {
                McpEmptyState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
            
            // Add/Edit Modal
            if (showAddModal) {
                McpEditModal(
                    mcp = editingMcp,
                    onDismiss = { showAddModal = false },
                    onSave = { mcp ->
                        scope.launch {
                            repository.upsertMcp(mcp)
                            // Try to fetch tools
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
fun McpTopBar(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Back button
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .background(Surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBackClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Back,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TextPrimary
            )
        }
        
        // Title
        Text(
            text = "MCP Tools",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Add button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .clip(CircleShape)
                .background(Surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAddClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Plus,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = TextPrimary
            )
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
                imageVector = AppIcons.Mcp,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GrayLight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Protocol Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GrayLighter),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (mcp.protocol == McpProtocol.HTTP) AppIcons.Globe else AppIcons.Stream,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = TextPrimary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Info
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
        
        // Toggle switch
        McpToggleSwitch(
            enabled = mcp.enabled,
            onToggle = onToggle
        )
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
fun McpEditModal(
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
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit MCP" else "Add MCP",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Protocol Selection
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Protocol",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GrayLighter)
                        .padding(4.dp),
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Name input
            McpInputField(
                label = "Name",
                value = name,
                onValueChange = { name = it },
                placeholder = "Enter MCP name"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // URL input
            McpInputField(
                label = "Server URL",
                value = url,
                onValueChange = { url = it },
                placeholder = "https://api.example.com/mcp"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description input
            McpInputField(
                label = "Description (Optional)",
                value = description,
                onValueChange = { description = it },
                placeholder = "Brief description"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Headers section
            McpHeadersSection(
                headers = headers,
                onHeadersChange = { headers = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
                                    headers = headers
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
            .clip(RoundedCornerShape(16.dp))
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
fun McpInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 17.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Normal
            ),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 17.sp,
                        color = TextSecondary.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun McpHeadersSection(
    headers: List<HttpHeader>,
    onHeadersChange: (List<HttpHeader>) -> Unit
) {
    var showAddHeader by remember { mutableStateOf(false) }
    var newHeaderKey by remember { mutableStateOf("") }
    var newHeaderValue by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom Headers",
                fontSize = 13.sp,
                color = TextSecondary
            )
            
            IconButton(
                onClick = { showAddHeader = true }
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GrayLighter),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = TextPrimary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Headers list
        headers.forEachIndexed { index, header ->
            HeaderItem(
                header = header,
                onDelete = {
                    onHeadersChange(headers.filterIndexed { i, _ -> i != index })
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        if (headers.isEmpty() && !showAddHeader) {
            Text(
                text = "No custom headers",
                fontSize = 14.sp,
                color = TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
        }
        
        // Add new header
        if (showAddHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = newHeaderKey,
                    onValueChange = { newHeaderKey = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                    decorationBox = { innerTextField ->
                        if (newHeaderKey.isEmpty()) {
                            Text(
                                text = "Key",
                                fontSize = 15.sp,
                                color = TextSecondary.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                )
                
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(GrayLight)
                )
                
                BasicTextField(
                    value = newHeaderValue,
                    onValueChange = { newHeaderValue = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                    decorationBox = { innerTextField ->
                        if (newHeaderValue.isEmpty()) {
                            Text(
                                text = "Value",
                                fontSize = 15.sp,
                                color = TextSecondary.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                )
                
                IconButton(
                    onClick = {
                        if (newHeaderKey.isNotBlank() && newHeaderValue.isNotBlank()) {
                            onHeadersChange(headers + HttpHeader(newHeaderKey.trim(), newHeaderValue.trim()))
                            newHeaderKey = ""
                            newHeaderValue = ""
                            showAddHeader = false
                        }
                    }
                ) {
                    Icon(
                        imageVector = AppIcons.Check,
                        contentDescription = null,
                        tint = AccentBlue
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderItem(
    header: HttpHeader,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrayLighter)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = header.key,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = header.value,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
        
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = AppIcons.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = TextSecondary
            )
        }
    }
}
