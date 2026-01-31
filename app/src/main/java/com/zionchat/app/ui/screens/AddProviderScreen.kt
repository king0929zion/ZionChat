package com.zionchat.app.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.ui.theme.*

@Composable
fun AddProviderScreen(
    navController: NavController,
    preset: String? = null
) {
    var providerName by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var apiUrl by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("openai") }
    var showAvatarModal by remember { mutableStateOf(false) }
    var selectedAvatar by remember { mutableStateOf("") }

    // Preset data
    val presetData = remember(preset) {
        when (preset) {
            "openai" -> PresetData("ChatGPT", "openai", "https://api.openai.com/v1")
            "anthropic" -> PresetData("Anthropic", "anthropic", "https://api.anthropic.com/v1")
            "google" -> PresetData("Google", "google", "https://generativelanguage.googleapis.com/v1beta")
            else -> null
        }
    }

    LaunchedEffect(presetData) {
        presetData?.let {
            providerName = it.name
            apiUrl = it.apiUrl
            selectedType = it.type
        }
    }

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
                    text = "Add Provider",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Save Button
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterEnd)
                        .background(Surface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = TextPrimary
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Selection
                Column(
                    modifier = Modifier.padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(GrayLighter, RoundedCornerShape(16.dp))
                            .clickable { showAvatarModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedAvatar.isNotEmpty()) {
                            Text(text = selectedAvatar, fontSize = 32.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Select Avatar",
                                tint = TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap to change avatar",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                // Form Fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Provider Name
                    FormField(
                        label = "Provider Name",
                        value = providerName,
                        onValueChange = { providerName = it },
                        placeholder = "Enter provider name"
                    )

                    // Provider Type
                    Column {
                        Text(
                            text = "Provider Type",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GrayLighter, RoundedCornerShape(20.dp))
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TypeOption(
                                text = "OpenAI",
                                selected = selectedType == "openai",
                                onClick = { selectedType = "openai" },
                                modifier = Modifier.weight(1f)
                            )
                            TypeOption(
                                text = "Anthropic",
                                selected = selectedType == "anthropic",
                                onClick = { selectedType = "anthropic" },
                                modifier = Modifier.weight(1f)
                            )
                            TypeOption(
                                text = "Google",
                                selected = selectedType == "google",
                                onClick = { selectedType = "google" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // API Key
                    FormField(
                        label = "API Key",
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        placeholder = "Enter API key"
                    )

                    // API URL
                    FormField(
                        label = "API URL",
                        value = apiUrl,
                        onValueChange = { apiUrl = it },
                        placeholder = "https://api.example.com/v1"
                    )

                    // Models Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface, RoundedCornerShape(20.dp))
                            .clickable { navController.navigate("models") }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Models",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Configure models",
                                fontSize = 17.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Navigate",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Avatar Selection Modal
        if (showAvatarModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showAvatarModal = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
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
                            text = "Select Avatar",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Avatar Grid
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val avatars = listOf(
                                "🔴", "🟣", "🔵", "🟢", "🟡",
                                "🦙", "🔷", "🌙", "💎", "🤖",
                                "🌀", "⚡", "🔶", "🟠", "🟤"
                            )

                            for (row in 0 until 3) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    for (col in 0 until 5) {
                                        val index = row * 5 + col
                                        if (index < avatars.size) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(GrayLighter, RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        selectedAvatar = avatars[index]
                                                        showAvatarModal = false
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = avatars[index],
                                                    fontSize = 24.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Import from Gallery
                        Button(
                            onClick = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GrayLighter
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Import",
                                tint = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Import from Gallery",
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = { showAvatarModal = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 16.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

data class PresetData(
    val name: String,
    val type: String,
    val apiUrl: String
)

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
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
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 17.sp,
                    color = TextPrimary
                ),
                singleLine = true
            )
        }
    }
}

@Composable
fun TypeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .background(
                if (selected) TextPrimary else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Surface else TextPrimary
        )
    }
}
