package com.zionchat.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.R
import com.zionchat.app.ui.components.rememberResourceDrawablePainter
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons

@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = { SettingsTopBar(navController) },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // User Profile Section
            UserProfileSection()

            // My ChatGPT 分组
            SettingsGroup(title = "My ChatGPT", itemCount = 2) {
                SettingsItem(
                    icon = { Icon(AppIcons.Personalization, null, Modifier.size(22.dp), tint = Color.Unspecified) },
                    label = "Personalization",
                    showDivider = true,
                    onClick = { }
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
                SettingsItem(
                    icon = { Icon(AppIcons.Appearance, null, Modifier.size(22.dp), tint = Color.Unspecified) },
                    label = "Appearance",
                    value = "Light",
                    showChevron = true,
                    showDivider = true,
                    onClick = { }
                )
                SettingsItem(
                    icon = { Icon(AppIcons.Accent, null, Modifier.size(22.dp), tint = Color.Unspecified) },
                    label = "Accent color",
                    value = "Default",
                    showChevron = true,
                    onClick = { }
                )
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
                    value = "GPT-4o",
                    showChevron = true,
                    showDivider = true,
                    onClick = { }
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
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
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
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun UserProfileSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 用户头像
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(GrayLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.User,
                contentDescription = "Avatar",
                tint = TextSecondary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 用户名
        Text(
            text = "Kendall Williamson",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        // Handle
        Text(
            text = "zizipz",
            fontSize = 15.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 编辑资料按钮
        Button(
            onClick = { },
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
