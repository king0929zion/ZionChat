package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.ui.theme.*

@Composable
fun SettingsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top Navigation Bar
        SettingsTopBar(navController)

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // User Profile Section
            UserProfileSection()

            // Settings Groups
            SettingsGroup(title = "MY CHATGPT") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Personalization",
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.Apps,
                    title = "Apps",
                    onClick = { }
                )
            }

            SettingsGroup(title = "APPEARANCE") {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                    trailing = { Text("Light", fontSize = 15.sp, color = TextSecondary) },
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.ColorLens,
                    title = "Accent color",
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF9CA3AF), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Default", fontSize = 15.sp, color = TextSecondary)
                        }
                    },
                    onClick = { }
                )
            }

            SettingsGroup(title = "GENERAL") {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    trailing = { Text("English", fontSize = 15.sp, color = TextSecondary) },
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    onClick = { }
                )
            }

            SettingsGroup(title = "AI MODEL") {
                SettingsItem(
                    icon = Icons.Default.SmartToy,
                    title = "Default model",
                    trailing = { Text("GPT-4o", fontSize = 15.sp, color = TextSecondary) },
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.Cloud,
                    title = "Model services",
                    onClick = { navController.navigate("model_services") }
                )
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "MCP Tools",
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
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Back Button
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterStart)
                .background(Surface, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }

        // Title
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
        // Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(GrayLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Avatar",
                tint = TextSecondary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Username
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

        // Edit Profile Button
        Button(
            onClick = { },
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(18.dp),
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
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Group Title
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        // Group Content
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        trailing?.invoke()

        if (trailing == null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = GrayLight,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = GrayLight,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
