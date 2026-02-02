package com.zionchat.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.LocalChatApiClient
import com.zionchat.app.LocalAppRepository
import com.zionchat.app.data.ModelConfig
import com.zionchat.app.data.ProviderConfig
import com.zionchat.app.data.buildModelStorageId
import com.zionchat.app.ui.components.TopFadeScrim
import com.zionchat.app.ui.components.BottomFadeScrim
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ModelsScreen(navController: NavController, providerId: String? = null) {
    val repository = LocalAppRepository.current
    val chatApiClient = LocalChatApiClient.current
    val scope = rememberCoroutineScope()
    var showAddModal by remember { mutableStateOf(false) }

    val providers by repository.providersFlow.collectAsState(initial = emptyList())
    val models by repository.modelsFlow.collectAsState(initial = emptyList())
    val activeProvider = remember(providers, providerId) {
        val id = providerId?.trim().orEmpty()
        if (id.isNotBlank()) providers.firstOrNull { it.id == id } else providers.firstOrNull()
    }

    var isFetchingRemote by remember { mutableStateOf(false) }
    var remoteError by remember { mutableStateOf<String?>(null) }
    var fetchedSignature by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeProvider?.id, activeProvider?.apiUrl, activeProvider?.apiKey, fetchedSignature) {
        val provider = activeProvider ?: return@LaunchedEffect
        if (provider.apiUrl.isBlank() || provider.apiKey.isBlank()) return@LaunchedEffect

        val signature = "${provider.id}|${provider.apiUrl}|${provider.apiKey}"
        if (signature == fetchedSignature) return@LaunchedEffect

        isFetchingRemote = true
        remoteError = null
        val result = chatApiClient.listModels(provider)
        val modelIds = result.getOrElse { throwable ->
            remoteError = "Failed to load models: ${throwable.message ?: throwable.toString()}"
            emptyList()
        }
        if (modelIds.isNotEmpty()) {
            repository.upsertModels(
                modelIds.map { idValue ->
                    ModelConfig(
                        id = buildModelStorageId(provider.id, idValue),
                        displayName = idValue,
                        enabled = false,
                        providerId = provider.id
                    )
                }
            )
        }
        fetchedSignature = signature
        isFetchingRemote = false
    }
    val providerModels = remember(models, activeProvider?.id) {
        val pid = activeProvider?.id
        if (pid.isNullOrBlank()) emptyList()
        else models.filter { it.providerId == pid }
    }

    // 搜索状态
    var searchQuery by remember { mutableStateOf("") }

    val filteredModels = remember(providerModels, searchQuery) {
        val query = searchQuery.trim().lowercase()
        providerModels
            .filter { query.isEmpty() || it.displayName.lowercase().contains(query) }
            .sortedWith(compareByDescending<ModelConfig> { it.enabled }
                .thenBy { it.displayName.lowercase() })
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isFetchingRemote,
        onRefresh = { fetchedSignature = null }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            // Top Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(Surface, CircleShape)
                        .pressableScale(pressedScale = 0.95f) { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Back,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title
                Text(
                    text = "Models",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Add Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(Surface, CircleShape)
                        .pressableScale(pressedScale = 0.95f) { showAddModal = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Plus,
                        contentDescription = "Add Model",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Status row
            if (isFetchingRemote || remoteError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isFetchingRemote -> "Loading models from provider..."
                            remoteError != null -> remoteError.orEmpty()
                            else -> ""
                        },
                        fontSize = 13.sp,
                        color = if (remoteError != null) Color(0xFFFF3B30) else TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (remoteError != null) {
                        Text(
                            text = "Retry",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { fetchedSignature = null }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Model List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pullRefresh(pullRefreshState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(GrayLight, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Search,
                            contentDescription = "Search",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 15.sp,
                                color = TextPrimary
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search models...",
                                        fontSize = 15.sp,
                                        color = TextSecondary
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = AppIcons.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }

                    filteredModels.forEach { model ->
                        ModelItem(
                            model = model,
                            onToggle = {
                                scope.launch { repository.upsertModel(model.copy(enabled = !model.enabled)) }
                            },
                            onClick = {
                                navController.navigate("model_config?id=${model.id}")
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                PullRefreshIndicator(
                    refreshing = isFetchingRemote,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    contentColor = TextPrimary,
                    backgroundColor = Surface
                )

                TopFadeScrim(
                    color = Background,
                    height = 40.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-10).dp)
                        .zIndex(1f)
                )
                BottomFadeScrim(
                    color = Background,
                    height = 44.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(1f)
                )
            }
        }

        // Add Model Modal
        AddModelModal(
            visible = showAddModal,
            onDismiss = { showAddModal = false },
            onAdd = { id, name ->
                scope.launch {
                    val provider = activeProvider ?: return@launch
                    repository.upsertModel(
                        ModelConfig(
                            id = buildModelStorageId(provider.id, id.trim()),
                            displayName = name.trim(),
                            enabled = false,
                            providerId = provider.id
                        )
                    )
                    showAddModal = false
                }
            }
        )
    }
}


@Composable
fun ModelItem(
    model: ModelConfig,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(GrayLight, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .pressableScale(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = model.displayName,
            fontSize = 17.sp,
            color = TextPrimary,
            maxLines = 2,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )

        ModelToggleSwitch(enabled = model.enabled, onToggle = onToggle)
    }
}

@Composable
private fun ModelToggleSwitch(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .background(
                if (enabled) TextPrimary else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .border(1.5.dp, if (enabled) TextPrimary else TextSecondary, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle() },
        contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(22.dp)
                .background(
                    if (enabled) Surface else TextSecondary,
                    CircleShape
                )
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AddModelModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var modelId by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }

    LaunchedEffect(visible) {
        if (visible) {
            modelId = ""
            modelName = ""
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                Surface(
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
                    color = Surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                    // Drag handle
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

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Add Model",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Model ID
                Column {
                    Text(
                        text = "Model ID",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = GrayLighter,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        TextField(
                            value = modelId,
                            onValueChange = { modelId = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            placeholder = {
                                Text(
                                    text = "e.g. gpt-4o",
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
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 17.sp,
                                color = TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display Name
                Column {
                    Text(
                        text = "Display Name",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = GrayLighter,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        TextField(
                            value = modelName,
                            onValueChange = { modelName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            placeholder = {
                                Text(
                                    text = "e.g. GPT-4o",
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
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 17.sp,
                                color = TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrayLight
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = {
                            if (modelId.isNotBlank() && modelName.isNotBlank()) {
                                onAdd(modelId, modelName)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextPrimary
                        )
                    ) {
                        Text(
                            text = "Add",
                            fontSize = 17.sp,
                            color = Surface
                        )
                    }
                }
                }
            }
        }
    }
}
}
