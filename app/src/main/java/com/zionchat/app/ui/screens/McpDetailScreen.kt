package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
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
import com.zionchat.app.data.McpTool
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpDetailScreen(
    navController: NavController,
    mcpId: String
) {
    val repository = LocalAppRepository.current
    val scope = rememberCoroutineScope()
    val mcpClient = remember { McpClient() }
    
    val mcpList by repository.mcpListFlow.collectAsState(initial = emptyList())
    val mcp = mcpList.firstOrNull { it.id == mcpId }
    
    var showEditModal by remember { mutableStateOf(false) }
    var showToolDetail by remember { mutableStateOf<McpTool?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    if (mcp == null) {
        // MCP not found, navigate back
        LaunchedEffect(Unit) {
            navController.navigateUp()
        }
        return
    }
    
    Scaffold(
        topBar = {
            McpDetailTopBar(
                title = mcp.name,
                onBackClick = { navController.navigateUp() },
                onEditClick = { showEditModal = true }
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // MCP Info Section
            McpInfoSection(mcp = mcp)
            
            // Connection Info
            McpConnectionSection(mcp = mcp)
            
            // Tools Section
            McpToolsSection(
                tools = mcp.tools,
                onToolClick = { showToolDetail = it }
            )
            
            // Delete Button
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Delete MCP",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Edit Modal
        if (showEditModal) {
            McpEditModal(
                mcp = mcp,
                onDismiss = { showEditModal = false },
                onSave = { updatedMcp ->
                    scope.launch {
                        repository.upsertMcp(updatedMcp)
                        // Try to fetch tools
                        runCatching {
                            val tools = mcpClient.fetchTools(updatedMcp).getOrNull()
                            if (tools != null) {
                                repository.updateMcpTools(updatedMcp.id, tools)
                            }
                        }
                        showEditModal = false
                    }
                }
            )
        }
        
        // Tool Detail Modal
        if (showToolDetail != null) {
            ToolDetailModal(
                tool = showToolDetail!!,
                onDismiss = { showToolDetail = null }
            )
        }
        
        // Delete Confirm Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = {
                    Text(
                        text = "Delete MCP?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = "This action cannot be undone.",
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.deleteMcp(mcpId)
                                showDeleteConfirm = false
                                navController.navigateUp()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3B30)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteConfirm = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrayLight,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = Surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun McpDetailTopBar(
    title: String,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit
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
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Edit button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .clip(CircleShape)
                .background(Surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEditClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Edit,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TextPrimary
            )
        }
    }
}

@Composable
fun McpInfoSection(mcp: McpConfig) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Protocol Icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GrayLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (mcp.protocol == McpProtocol.HTTP) AppIcons.Globe else AppIcons.Stream,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = TextPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Name
        Text(
            text = mcp.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        
        // Protocol
        Text(
            text = mcp.protocol.name,
            fontSize = 15.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        // Status badge
        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(CircleShape)
                .background(Surface)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (mcp.enabled) Color(0xFF34C759) else TextSecondary)
            )
            Text(
                text = if (mcp.enabled) "Connected" else "Disabled",
                fontSize = 14.sp,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun McpConnectionSection(mcp: McpConfig) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section title
        Text(
            text = "Connection",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        
        // Info cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
        ) {
            // Server URL
            ConnectionInfoItem(
                label = "Server URL",
                value = mcp.url,
                isLast = mcp.description.isBlank()
            )
            
            // Description (if exists)
            if (mcp.description.isNotBlank()) {
                ConnectionInfoItem(
                    label = "Description",
                    value = mcp.description,
                    isLast = true
                )
            }
        }
    }
}

@Composable
fun ConnectionInfoItem(
    label: String,
    value: String,
    isLast: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = TextPrimary,
            maxLines = 3
        )
    }
    
    if (!isLast) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
                .height(1.dp)
                .background(GrayLighter)
        )
    }
}

@Composable
fun McpToolsSection(
    tools: List<McpTool>,
    onToolClick: (McpTool) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section title
        Row(
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Available Tools",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = " (${tools.size})",
                fontSize = 13.sp,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
        
        if (tools.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GrayLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Tool,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "No Tools Available",
                    fontSize = 17.sp,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "This MCP doesn't expose any tools",
                    fontSize = 14.sp,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        } else {
            // Tools list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tools.forEach { tool ->
                    ToolItem(
                        tool = tool,
                        onClick = { onToolClick(tool) }
                    )
                }
            }
        }
    }
}

@Composable
fun ToolItem(
    tool: McpTool,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = tool.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = tool.description,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color(0xFFC7C7CC)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailModal(
    tool: McpTool,
    onDismiss: () -> Unit
) {
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
                    text = tool.name,
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
            
            // Description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GrayLighter)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Description",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = tool.description,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Parameters
            Text(
                text = "Parameters",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
            
            if (tool.parameters.isEmpty()) {
                Text(
                    text = "No parameters required",
                    fontSize = 14.sp,
                    color = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tool.parameters.forEach { param ->
                        ParameterItem(param = param)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Close button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Close",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ParameterItem(param: com.zionchat.app.data.McpToolParameter) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GrayLighter)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = param.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = param.type,
                fontSize = 12.sp,
                color = TextSecondary
            )
            if (param.required) {
                Text(
                    text = "required",
                    fontSize = 12.sp,
                    color = Color(0xFFFF3B30)
                )
            } else {
                Text(
                    text = "optional",
                    fontSize = 12.sp,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
        
        if (param.description.isNotBlank()) {
            Text(
                text = param.description,
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
