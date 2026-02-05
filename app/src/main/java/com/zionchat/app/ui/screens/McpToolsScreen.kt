package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.LocalAppRepository
import com.zionchat.app.data.McpClient
import com.zionchat.app.data.McpConfig
import com.zionchat.app.data.McpTool
import com.zionchat.app.ui.components.AppModalBottomSheet
import com.zionchat.app.ui.components.PageTopBar
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.Background
import com.zionchat.app.ui.theme.GrayLight
import com.zionchat.app.ui.theme.SourceSans3
import com.zionchat.app.ui.theme.Surface
import com.zionchat.app.ui.theme.TextPrimary
import com.zionchat.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun McpToolsScreen(
    navController: NavController,
    mcpId: String
) {
    val repository = LocalAppRepository.current
    val scope = rememberCoroutineScope()
    val mcpClient = remember { McpClient() }

    val mcpListOrNull by produceState<List<McpConfig>?>(initialValue = null, repository) {
        repository.mcpListFlow.collect { value = it }
    }
    val mcp = mcpListOrNull?.firstOrNull { it.id == mcpId }

    var syncing by remember { mutableStateOf(false) }
    var showToolDetail by remember { mutableStateOf<McpTool?>(null) }

    LaunchedEffect(mcpListOrNull, mcpId) {
        if (mcpListOrNull != null && mcp == null) {
            navController.navigateUp()
        }
    }

    if (mcpListOrNull == null || mcp == null) {
        Box(modifier = Modifier.fillMaxSize().background(Background))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        PageTopBar(
            title = "Available tools",
            onBack = { navController.navigateUp() },
            trailing = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Surface, CircleShape)
                        .pressableScale(
                            enabled = !syncing,
                            pressedScale = 0.95f
                        ) {
                            if (syncing) return@pressableScale
                            syncing = true
                            scope.launch {
                                try {
                                    mcpClient.fetchTools(mcp)
                                        .onSuccess { tools -> repository.updateMcpTools(mcp.id, tools) }
                                } finally {
                                    syncing = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Refresh,
                        contentDescription = "Sync",
                        tint = if (syncing) TextSecondary else TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (mcp.tools.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No tools available",
                    fontSize = 14.sp,
                    fontFamily = SourceSans3,
                    color = TextSecondary
                )
            } else {
                mcp.tools.forEach { tool ->
                    McpToolItem(
                        tool = tool,
                        onClick = { showToolDetail = tool }
                    )
                }
            }
        }
    }

    if (showToolDetail != null) {
        val toolSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        AppModalBottomSheet(
            onDismissRequest = { showToolDetail = null },
            sheetState = toolSheetState
        ) {
            ToolDetailSheetContent(
                tool = showToolDetail!!,
                onDismiss = { showToolDetail = null }
            )
        }
    }
}

@Composable
private fun McpToolItem(
    tool: McpTool,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(GrayLight, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .pressableScale(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = tool.name,
            fontSize = 17.sp,
            fontFamily = SourceSans3,
            fontWeight = FontWeight.Normal,
            color = TextPrimary,
            maxLines = 2
        )
    }
}
