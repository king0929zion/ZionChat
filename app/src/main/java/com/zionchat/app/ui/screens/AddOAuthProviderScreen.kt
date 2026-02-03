package com.zionchat.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.SourceSans3
import com.zionchat.app.ui.components.pressableScale

// 页面状态
private enum class OAuthStep {
    STEP_1_CONNECT,
    STEP_2_CALLBACK,
    STEP_3_COMPLETED
}

// 模拟模型数据
private data class ModelItem(
    val id: String,
    val name: String,
    var enabled: Boolean
)

@Composable
fun AddOAuthProviderScreen(navController: NavController) {
    var providerName by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(OAuthStep.STEP_1_CONNECT) }
    var callbackUrl by remember { mutableStateOf("") }
    var showAvatarModal by remember { mutableStateOf(false) }
    var selectedAvatar by remember { mutableStateOf("openai") }
    var showModelsSection by remember { mutableStateOf(false) }

    // 模拟模型列表
    var models by remember {
        mutableStateOf(
            listOf(
                ModelItem("gpt-4o", "GPT-4o", true),
                ModelItem("gpt-4o-mini", "GPT-4o Mini", false),
                ModelItem("gpt-4-turbo", "GPT-4 Turbo", false),
                ModelItem("gpt-3.5-turbo", "GPT-3.5 Turbo", true),
                ModelItem("dall-e-3", "DALL-E 3", false),
                ModelItem("whisper-1", "Whisper", false)
            )
        )
    }

    val enabledCount = models.count { it.enabled }

    Scaffold(
        topBar = {
            AddProviderTopBar(
                onBack = { navController.navigateUp() },
                onSave = {
                    if (providerName.isBlank()) {
                        // 显示错误提示
                    } else if (currentStep != OAuthStep.STEP_3_COMPLETED) {
                        // 显示请先完成OAuth提示
                    } else {
                        // 保存并返回
                        navController.navigateUp()
                    }
                }
            )
        },
        containerColor = Color(0xFFF5F5F7)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 头像选择区
            AvatarSection(
                selectedAvatar = selectedAvatar,
                onAvatarClick = { showAvatarModal = true }
            )

            // 表单区域
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Provider Name 输入框
                ProviderNameInput(
                    value = providerName,
                    onValueChange = { providerName = it }
                )

                // OAuth 认证区域
                OAuthSection(
                    currentStep = currentStep,
                    callbackUrl = callbackUrl,
                    onCallbackUrlChange = { callbackUrl = it },
                    onStartOAuth = { currentStep = OAuthStep.STEP_2_CALLBACK },
                    onSubmitCallback = {
                        if (callbackUrl.isNotBlank()) {
                            currentStep = OAuthStep.STEP_3_COMPLETED
                            showModelsSection = true
                        }
                    },
                    onCancel = { currentStep = OAuthStep.STEP_1_CONNECT },
                    onReset = {
                        currentStep = OAuthStep.STEP_1_CONNECT
                        showModelsSection = false
                        callbackUrl = ""
                    }
                )

                // Models Section (OAuth完成后显示)
                AnimatedVisibility(
                    visible = showModelsSection,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 })
                ) {
                    ModelsSection(
                        models = models,
                        enabledCount = enabledCount,
                        onToggleModel = { modelId ->
                            models = models.map {
                                if (it.id == modelId) it.copy(enabled = !it.enabled) else it
                            }
                        }
                    )
                }
            }

            // 底部空间
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 头像选择弹窗
    if (showAvatarModal) {
        AvatarSelectionModal(
            selectedAvatar = selectedAvatar,
            onAvatarSelected = { selectedAvatar = it },
            onDismiss = { showAvatarModal = false },
            onImportFromGallery = { /* 打开相册 */ }
        )
    }
}

@Composable
private fun AddProviderTopBar(
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F7).copy(alpha = 0.95f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 返回按钮
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .pressableScale(pressedScale = 0.95f, onClick = onBack)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Back,
                contentDescription = "Back",
                tint = Color(0xFF1C1C1E),
                modifier = Modifier.size(20.dp)
            )
        }

        // 标题
        Text(
            text = "Add Provider",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SourceSans3,
            color = Color(0xFF1C1C1E),
            modifier = Modifier.align(Alignment.Center)
        )

        // 保存按钮
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .pressableScale(pressedScale = 0.95f, onClick = onSave)
                .align(Alignment.CenterEnd),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = "Save",
                tint = Color(0xFF1C1C1E),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AvatarSection(
    selectedAvatar: String,
    onAvatarClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 大头像
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAvatarClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // 这里应该显示实际的 provider 图标
            // 使用一个简单的占位符
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF2F2F7)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedAvatar.take(1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
            }

            // 编辑图标覆盖层 (hover效果在移动端显示为常显或点击时显示)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Edit,
                    contentDescription = "Edit",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tap to change avatar",
            fontSize = 13.sp,
            color = Color(0xFF8E8E93),
            fontFamily = SourceSans3
        )
    }
}

@Composable
private fun AvatarSelectionModal(
    selectedAvatar: String,
    onAvatarSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onImportFromGallery: () -> Unit
) {
    val builtinAvatars = listOf(
        "openai", "anthropic", "google", "azure", "ollama", "deepseek",
        "moonshot", "yi", "groq", "openrouter", "mistral", "gemini",
        "grok", "baichuan", "zhipu", "minimax", "stepfun", "hunyuan",
        "silicon", "together", "perplexity", "fireworks", "cerebras", "meta"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    ),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
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
                                .background(Color(0xFFD1D1D6), RoundedCornerShape(2.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 标题
                    Text(
                        text = "Select Avatar",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E),
                        fontFamily = SourceSans3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 头像网格
                    val rows = builtinAvatars.chunked(5)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { avatar ->
                                    val isSelected = avatar == selectedAvatar
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
                                            )
                                            .clickable {
                                                onAvatarSelected(avatar)
                                                onDismiss()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = avatar.take(1).uppercase(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF1C1C1E)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 从相册导入按钮
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF5F5F7))
                            .clickable(onClick = onImportFromGallery),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Image,
                                contentDescription = null,
                                tint = Color(0xFF1C1C1E),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Import from Gallery",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1C1C1E),
                                fontFamily = SourceSans3
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 取消按钮
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            color = Color(0xFF8E8E93),
                            fontFamily = SourceSans3
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderNameInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Provider Name",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                fontFamily = SourceSans3
            )

            Spacer(modifier = Modifier.height(6.dp))

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 17.sp,
                    color = Color(0xFF1C1C1E),
                    fontFamily = SourceSans3
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "Enter provider name",
                                fontSize = 17.sp,
                                color = Color(0xFFC7C7CC),
                                fontFamily = SourceSans3
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun OAuthSection(
    currentStep: OAuthStep,
    callbackUrl: String,
    onCallbackUrlChange: (String) -> Unit,
    onStartOAuth: () -> Unit,
    onSubmitCallback: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Authentication",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                    fontFamily = SourceSans3
                )

                // OAuth 2.0 标签
                Surface(
                    color = Color(0xFFE8F4FD),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "OAuth 2.0",
                        fontSize = 12.sp,
                        color = Color(0xFF007AFF),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontFamily = SourceSans3
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 步骤指示器
            StepIndicator(currentStep = currentStep)

            Spacer(modifier = Modifier.height(16.dp))

            // 步骤内容
            when (currentStep) {
                OAuthStep.STEP_1_CONNECT -> Step1Content(onStartOAuth = onStartOAuth)
                OAuthStep.STEP_2_CALLBACK -> Step2Content(
                    callbackUrl = callbackUrl,
                    onCallbackUrlChange = onCallbackUrlChange,
                    onSubmit = onSubmitCallback,
                    onCancel = onCancel
                )
                OAuthStep.STEP_3_COMPLETED -> Step3Content(onReset = onReset)
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: OAuthStep) {
    val stepNumber = when (currentStep) {
        OAuthStep.STEP_1_CONNECT -> 1
        OAuthStep.STEP_2_CALLBACK -> 2
        OAuthStep.STEP_3_COMPLETED -> 3
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step 1
        StepCircle(
            number = 1,
            isActive = stepNumber == 1,
            isCompleted = stepNumber > 1
        )

        // Line 1
        StepLine(isCompleted = stepNumber > 1)

        // Step 2
        StepCircle(
            number = 2,
            isActive = stepNumber == 2,
            isCompleted = stepNumber > 2
        )

        // Line 2
        StepLine(isCompleted = stepNumber > 2)

        // Step 3
        StepCircle(
            number = 3,
            isActive = stepNumber == 3,
            isCompleted = false
        )
    }
}

@Composable
private fun StepCircle(
    number: Int,
    isActive: Boolean,
    isCompleted: Boolean
) {
    val backgroundColor = when {
        isCompleted -> Color(0xFF1C1C1E)
        isActive -> Color(0xFF007AFF)
        else -> Color(0xFFF2F2F7)
    }

    val contentColor = when {
        isCompleted || isActive -> Color.White
        else -> Color(0xFF8E8E93)
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Text(
                text = number.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun StepLine(isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(2.dp)
            .padding(horizontal = 8.dp)
            .background(
                if (isCompleted) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
            )
    )
}

@Composable
private fun Step1Content(onStartOAuth: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Connect your account via OAuth to enable this provider",
            fontSize = 14.sp,
            color = Color(0xFF6B6B6B),
            fontFamily = SourceSans3,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1C1E))
                .clickable(onClick = onStartOAuth),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = AppIcons.OAuth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Connect with OAuth",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontFamily = SourceSans3
                )
            }
        }
    }
}

@Composable
private fun Step2Content(
    callbackUrl: String,
    onCallbackUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Please enter your OAuth callback URL",
            fontSize = 14.sp,
            color = Color(0xFF6B6B6B),
            fontFamily = SourceSans3,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 输入框
        Surface(
            color = Color(0xFFF5F5F7),
            shape = RoundedCornerShape(14.dp)
        ) {
            BasicTextField(
                value = callbackUrl,
                onValueChange = onCallbackUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    color = Color(0xFF1C1C1E),
                    fontFamily = SourceSans3
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (callbackUrl.isEmpty()) {
                            Text(
                                text = "https://your-app.com/oauth/callback",
                                fontSize = 16.sp,
                                color = Color(0xFFC7C7CC),
                                fontFamily = SourceSans3
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel 按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF5F5F7))
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1C1C1E),
                    fontFamily = SourceSans3
                )
            }

            // Continue 按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable(onClick = onSubmit),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontFamily = SourceSans3
                )
            }
        }
    }
}

@Composable
private fun Step3Content(onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 成功图标
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "OAuth Connected!",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C1E),
            fontFamily = SourceSans3
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "You can now select models from this provider",
            fontSize = 13.sp,
            color = Color(0xFF8E8E93),
            fontFamily = SourceSans3
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onReset) {
            Text(
                text = "Disconnect",
                fontSize = 14.sp,
                color = Color(0xFF1C1C1E).copy(alpha = 0.6f),
                fontFamily = SourceSans3
            )
        }
    }
}

@Composable
private fun ModelsSection(
    models: List<ModelItem>,
    enabledCount: Int,
    onToggleModel: (String) -> Unit
) {
    Column {
        // 标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Available Models",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                fontFamily = SourceSans3
            )

            Text(
                text = "$enabledCount of ${models.size} enabled",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                fontFamily = SourceSans3
            )
        }

        // 模型列表
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column {
                models.forEachIndexed { index, model ->
                    ModelListItem(
                        model = model,
                        onToggle = { onToggleModel(model.id) },
                        showDivider = index < models.size - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelListItem(
    model: ModelItem,
    onToggle: () -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = model.name,
                fontSize = 16.sp,
                color = Color(0xFF1C1C1E),
                fontFamily = SourceSans3
            )

            // 自定义 Toggle Switch
            CustomToggleSwitch(enabled = model.enabled)
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(1.dp)
                    .background(Color(0xFFF2F2F7))
            )
        }
    }
}

@Composable
private fun CustomToggleSwitch(enabled: Boolean) {
    val width = 50.dp
    val height = 30.dp
    val thumbSize = 22.dp

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(15.dp))
            .background(
                if (enabled) Color(0xFF1C1C1E) else Color.Transparent
            )
            .border(
                width = if (enabled) 0.dp else 2.dp,
                color = if (enabled) Color.Transparent else Color(0xFF9CA3AF),
                shape = RoundedCornerShape(15.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Thumb
        val thumbOffset by animateFloatAsState(
            targetValue = if (enabled) 1f else 0f,
            animationSpec = tween(200),
            label = "thumb"
        )

        Box(
            modifier = Modifier
                .padding(
                    start = if (enabled) 0.dp else 4.dp,
                    end = if (enabled) 4.dp else 0.dp
                )
                .offset(x = thumbOffset * (width - thumbSize - if (enabled) 8.dp else 0.dp).value.dp)
                .size(thumbSize)
                .clip(CircleShape)
                .background(if (enabled) Color.White else Color(0xFF9CA3AF)),
        )
    }
}
