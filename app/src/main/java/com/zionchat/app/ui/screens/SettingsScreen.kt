package com.zionchat.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.R
import com.zionchat.app.LocalAppRepository
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.zionchat.app.data.extractRemoteModelId
import com.zionchat.app.ui.components.TopFadeScrim
import com.zionchat.app.ui.components.rememberResourceDrawablePainter
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(navController: NavController) {
    val repository = LocalAppRepository.current
    val scope = rememberCoroutineScope()
    val models by repository.modelsFlow.collectAsState(initial = emptyList())
    val defaultChatModelId by repository.defaultChatModelIdFlow.collectAsState(initial = null)
    val nickname by repository.nicknameFlow.collectAsState(initial = "")
    val avatarUri by repository.avatarUriFlow.collectAsState(initial = "")

    var showEditProfile by remember { mutableStateOf(false) }

    // Appearance & Accent Color 菜单状态
    var showAppearanceMenu by remember { mutableStateOf(false) }
    var showAccentColorMenu by remember { mutableStateOf(false) }
    var selectedAppearance by remember { mutableStateOf("light") }
    var selectedAccentColor by remember { mutableStateOf("default") }

    // 菜单锚点位置
    var appearanceAnchorY by remember { mutableFloatStateOf(0f) }
    var accentColorAnchorY by remember { mutableFloatStateOf(0f) }

    val defaultChatModelName = remember(models, defaultChatModelId) {
        val id = defaultChatModelId?.trim().orEmpty()
        if (id.isBlank()) null
        else models.firstOrNull { it.id == id }?.displayName
            ?: models.firstOrNull { extractRemoteModelId(it.id) == id }?.displayName
    }

    val displayName = nickname.takeIf { it.isNotBlank() } ?: "Kendall Williamson"

    Scaffold(
        topBar = { SettingsTopBar(navController) },
        containerColor = Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // User Profile Section
                UserProfileSection(
                    nickname = displayName,
                    avatarUri = avatarUri,
                    onEditClick = { showEditProfile = true }
                )

                // My ChatGPT 分组
                SettingsGroup(title = "My ChatGPT", itemCount = 2) {
                    SettingsItem(
                        icon = { Icon(AppIcons.Personalization, null, Modifier.size(22.dp), tint = Color.Unspecified) },
                        label = "Personalization",
                        showDivider = true,
                        onClick = { navController.navigate("personalization") }
                    )
                    SettingsItem(
                        icon = {
                            Icon(
                                painter = rememberResourceDrawablePainter(R.drawable.ic_apps),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color.Unspecified
                            )
                        },
                        label = "Apps",
                        onClick = { }
                    )
                }

                // Appearance 分组
                SettingsGroup(title = "Appearance", itemCount = 2) {
                    Box(modifier = Modifier.onGloballyPositioned { coordinates ->
                        appearanceAnchorY = coordinates.positionInWindow().y
                    }) {
                        SettingsItem(
                            icon = { Icon(AppIcons.Appearance, null, Modifier.size(22.dp), tint = Color.Unspecified) },
                            label = "Appearance",
                            value = when(selectedAppearance) {
                                "system" -> "System"
                                "light" -> "Light"
                                "dark" -> "Dark"
                                else -> "Light"
                            },
                            showChevron = true,
                            showDivider = true,
                            onClick = {
                                showAccentColorMenu = false
                                showAppearanceMenu = true
                            }
                        )
                    }
                    Box(modifier = Modifier.onGloballyPositioned { coordinates ->
                        accentColorAnchorY = coordinates.positionInWindow().y
                    }) {
                        SettingsItem(
                            icon = { Icon(AppIcons.Accent, null, Modifier.size(22.dp), tint = Color.Unspecified) },
                            label = "Accent color",
                            value = selectedAccentColor.replaceFirstChar { it.uppercase() },
                            showChevron = true,
                            onClick = {
                                showAppearanceMenu = false
                                showAccentColorMenu = true
                            }
                        )
                    }
                }

                // General 分组
                SettingsGroup(title = "General", itemCount = 2) {
                    SettingsItem(
                        icon = { Icon(AppIcons.Language, null, Modifier.size(22.dp), tint = Color.Unspecified) },
                        label = "Language",
                        value = "English",
                        showChevron = true,
                        showDivider = true,
                        onClick = { }
                    )
                    SettingsItem(
                        icon = { Icon(AppIcons.Notifications, null, Modifier.size(22.dp), tint = Color.Unspecified) },
                        label = "Notifications",
                        showChevron = true,
                        onClick = { }
                    )
                }

                // AI Model 分组
                SettingsGroup(title = "AI Model", itemCount = 3) {
                    SettingsItem(
                        icon = {
                            Icon(
                                painter = rememberResourceDrawablePainter(R.drawable.ic_model),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color.Unspecified
                            )
                        },
                        label = "Default model",
                        value = defaultChatModelName ?: "Not set",
                        showChevron = true,
                        showDivider = true,
                        onClick = { navController.navigate("default_model") }
                    )
                    SettingsItem(
                        icon = {
                            Icon(
                                painter = rememberResourceDrawablePainter(R.drawable.ic_model_services),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color.Unspecified
                            )
                        },
                        label = "Model services",
                        showChevron = true,
                        showDivider = true,
                        onClick = { navController.navigate("model_services") }
                    )
                    SettingsItem(
                        icon = { Icon(AppIcons.MCPTools, null, Modifier.size(22.dp), tint = Color.Unspecified) },
                        label = "MCP Tools",
                        showChevron = true,
                        onClick = { navController.navigate("mcp") }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            TopFadeScrim(
                color = Background,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // 编辑资料弹窗
    EditProfileModal(
        visible = showEditProfile,
        onDismiss = { showEditProfile = false },
        currentNickname = displayName,
        currentAvatarUri = avatarUri,
        onSave = { newNickname, newAvatarUri ->
            scope.launch {
                repository.setNickname(newNickname)
                repository.setAvatarUri(newAvatarUri)
                showEditProfile = false
            }
        }
    )

    // Appearance 菜单
    AppearanceMenu(
        visible = showAppearanceMenu,
        selected = selectedAppearance,
        anchorY = appearanceAnchorY,
        onDismiss = { showAppearanceMenu = false },
        onSelect = { selectedAppearance = it }
    )

    // Accent Color 菜单
    AccentColorMenu(
        visible = showAccentColorMenu,
        selected = selectedAccentColor,
        anchorY = accentColorAnchorY,
        onDismiss = { showAccentColorMenu = false },
        onSelect = { selectedAccentColor = it }
    )
}

@Composable
fun SettingsTopBar(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background.copy(alpha = 0.95f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // 返回按钮
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Surface, CircleShape)
                .pressableScale(pressedScale = 0.95f) { navController.navigateUp() }
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Back,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // 标题
        Text(
            text = "Settings",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SourceSans3,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun UserAvatar(
    avatarUri: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(GrayLight, CircleShape)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri.isNotBlank()) {
            AsyncImage(
                model = avatarUri,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = AppIcons.User,
                contentDescription = "Avatar",
                tint = TextSecondary,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

@Composable
fun UserProfileSection(
    nickname: String,
    avatarUri: String,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 用户头像
        UserAvatar(
            avatarUri = avatarUri,
            size = 80.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 用户名
        Text(
            text = nickname,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 编辑资料按钮
        Button(
            onClick = onEditClick,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Surface
            ),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            Text(
                text = "Edit profile",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EditProfileModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    currentNickname: String,
    currentAvatarUri: String,
    onSave: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = remember(density) { with(density) { 120.dp.toPx() } }

    var nickname by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf("") }

    // 图片选择器
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { avatarUri = it.toString() }
    }

    LaunchedEffect(visible) {
        if (visible) {
            nickname = currentNickname
            avatarUri = currentAvatarUri
            dragOffsetPx = 0f
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
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
            ) {
                Surface(
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
                                val shouldDismiss = dragOffsetPx > dismissThresholdPx || velocity > 2400f
                                if (shouldDismiss) {
                                    onDismiss()
                                    dragOffsetPx = 0f
                                } else {
                                    scope.launch {
                                        animate(
                                            initialValue = dragOffsetPx,
                                            targetValue = 0f,
                                            animationSpec = tween(durationMillis = 180, easing = LinearEasing)
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
                    color = Surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
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

                        // 标题
                        Text(
                            text = "Edit Profile",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SourceSans3,
                            color = TextPrimary,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        // 头像上传区域 - 居中
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 可点击的头像
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(GrayLight, CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            imagePicker.launch("image/*")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatarUri.isNotBlank()) {
                                        AsyncImage(
                                            model = avatarUri,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = AppIcons.User,
                                            contentDescription = "Avatar",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(50.dp)
                                        )
                                    }

                                    // 相机图标叠加
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Camera,
                                            contentDescription = "Change Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Tap to change photo",
                                    fontSize = 13.sp,
                                    fontFamily = SourceSans3,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Name 输入框
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Name",
                                fontSize = 13.sp,
                                fontFamily = SourceSans3,
                                color = TextSecondary
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = GrayLighter,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                BasicTextField(
                                    value = nickname,
                                    onValueChange = { nickname = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    textStyle = TextStyle(
                                        fontSize = 17.sp,
                                        color = TextPrimary
                                    ),
                                    cursorBrush = SolidColor(TextPrimary),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 按钮
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
                                colors = ButtonDefaults.buttonColors(containerColor = GrayLight)
                            ) {
                                Text(text = "Cancel", fontSize = 17.sp, color = TextPrimary)
                            }

                            Button(
                                onClick = {
                                    onSave(nickname.trim(), avatarUri)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
                            ) {
                                Text(text = "Save", fontSize = 17.sp, color = Surface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    itemCount: Int = 0,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 分组标题
        Text(
            text = title.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SourceSans3,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        // 分组内容 - 白色卡片带圆角
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(0.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: @Composable () -> Unit,
    label: String,
    value: String? = null,
    showChevron: Boolean = false,
    showDivider: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 点击时的缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isPressed) Color(0xFFE5E5EA) else Surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                fontSize = 16.sp,
                fontFamily = SourceSans3,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            if (value != null) {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (showChevron) {
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 白色分割线（带轻微阴影效果）
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(1.dp)
                    .background(Color.White)
                    .shadow(0.5.dp, spotColor = Color(0xFFE5E5EA), ambientColor = Color(0xFFE5E5EA))
            )
        }
    }
}

// Appearance 选项数据 - 使用正确图标：System=Monitor, Light=Sun, Dark=Moon
private val appearanceOptions = listOf(
    Triple("system", "System", AppIcons.Monitor),
    Triple("light", "Light", AppIcons.Sun),
    Triple("dark", "Dark", AppIcons.Moon)
)

// Accent Color 选项数据
private val accentColorOptions = listOf(
    Pair("default", Color(0xFF9CA3AF)),
    Pair("blue", Color(0xFF3B82F6)),
    Pair("green", Color(0xFF22C55E)),
    Pair("yellow", Color(0xFFEAB308)),
    Pair("pink", Color(0xFFEC4899)),
    Pair("orange", Color(0xFFF97316)),
    Pair("purple", Color(0xFFA855F7))
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppearanceMenu(
    visible: Boolean,
    selected: String,
    anchorY: Float,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(100)),
        exit = fadeOut(animationSpec = tween(100))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = tween(250, easing = androidx.compose.animation.core.EaseOutCubic)
                ) + androidx.compose.animation.scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(250, easing = androidx.compose.animation.core.EaseOutCubic)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { 20 },
                    animationSpec = tween(200, easing = androidx.compose.animation.core.EaseInCubic)
                ) + androidx.compose.animation.scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(200, easing = androidx.compose.animation.core.EaseInCubic)
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 80.dp)
                    .graphicsLayer {
                        translationY = anchorY - 20
                    }
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 200.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF5F5F5),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        appearanceOptions.forEach { (key, label, icon) ->
                            val isSelected = key == selected
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Transparent)
                                    .clickable {
                                        onSelect(key)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = TextPrimary
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                            color = TextPrimary
                                        )
                                    }

                                    // 选中打勾 - 带动画
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isSelected,
                                        enter = androidx.compose.animation.scaleIn(
                                            initialScale = 0f,
                                            animationSpec = tween(200, easing = androidx.compose.animation.core.EaseOutBack)
                                        ),
                                        exit = androidx.compose.animation.scaleOut(
                                            targetScale = 0f,
                                            animationSpec = tween(150)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AccentColorMenu(
    visible: Boolean,
    selected: String,
    anchorY: Float,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(100)),
        exit = fadeOut(animationSpec = tween(100))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = tween(250, easing = androidx.compose.animation.core.EaseOutCubic)
                ) + androidx.compose.animation.scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(250, easing = androidx.compose.animation.core.EaseOutCubic)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { 20 },
                    animationSpec = tween(200, easing = androidx.compose.animation.core.EaseInCubic)
                ) + androidx.compose.animation.scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(200, easing = androidx.compose.animation.core.EaseInCubic)
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 80.dp)
                    .graphicsLayer {
                        translationY = anchorY - 20
                    }
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 200.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF5F5F5),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        accentColorOptions.forEach { (key, color) ->
                            val isSelected = key == selected
                            val isPurple = key == "purple"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Transparent)
                                    .clickable {
                                        onSelect(key)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 颜色圆点
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )

                                        Text(
                                            text = key.replaceFirstChar { it.uppercase() },
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                            color = TextPrimary
                                        )

                                        // Plus 标签
                                        if (isPurple) {
                                            Text(
                                                text = "· Plus",
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    // 选中打勾 - 带动画
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isSelected,
                                        enter = androidx.compose.animation.scaleIn(
                                            initialScale = 0f,
                                            animationSpec = tween(200, easing = androidx.compose.animation.core.EaseOutBack)
                                        ),
                                        exit = androidx.compose.animation.scaleOut(
                                            targetScale = 0f,
                                            animationSpec = tween(150)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = when (key) {
                                                "default" -> Color(0xFF6B7280)
                                                "blue" -> Color(0xFF3B82F6)
                                                "green" -> Color(0xFF22C55E)
                                                "yellow" -> Color(0xFFCA8A04)
                                                "pink" -> Color(0xFFEC4899)
                                                "orange" -> Color(0xFFF97316)
                                                "purple" -> Color(0xFFA855F7)
                                                else -> TextPrimary
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
