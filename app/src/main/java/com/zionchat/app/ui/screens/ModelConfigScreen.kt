package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons

@Composable
fun ModelConfigScreen(navController: NavController) {
    var modelName by remember { mutableStateOf("GPT-4o") }
    var selectedModality by remember { mutableStateOf("text-image") }
    val headers = remember {
        mutableStateListOf<Header>()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Top Navigation Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 16.dp)
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
                text = "Model Configuration",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )

            // Save Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .background(Surface, CircleShape)
                    .pressableScale(pressedScale = 0.95f) { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = "Save",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Model Name
            Column {
                Text(
                    text = "Model Name",
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
                        value = modelName,
                        onValueChange = { modelName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        placeholder = {
                            Text(
                                text = "Enter display name",
                                fontSize = 17.sp,
                                color = TextSecondary
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 17.sp,
                            color = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            }

            // Input Modality
            Column {
                Text(
                    text = "Input Modality",
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
                    ModalityOption(
                        iconVector = AppIcons.TextOnly,
                        text = "Text Only",
                        selected = selectedModality == "text",
                        onClick = { selectedModality = "text" },
                        modifier = Modifier.weight(1f)
                    )
                    ModalityOption(
                        iconVector = AppIcons.TextImage,
                        text = "Text & Image",
                        selected = selectedModality == "text-image",
                        onClick = { selectedModality = "text-image" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Custom Headers
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Surface,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
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

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GrayLighter, CircleShape)
                                .pressableScale(pressedScale = 0.95f) { headers.add(Header("", "")) },
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
                                color = TextSecondary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            headers.forEachIndexed { index, header ->
                                HeaderItem(
                                    header = header,
                                    onKeyChange = { headers[index] = header.copy(key = it) },
                                    onValueChange = { headers[index] = header.copy(value = it) },
                                    onRemove = { headers.removeAt(index) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ModalityOption(
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) TextPrimary else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .pressableScale(pressedScale = 0.95f, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = if (selected) Surface else TextPrimary,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 4.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Surface else TextPrimary
        )
    }
}

@Composable
fun HeaderItem(
    header: Header,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayLighter, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Key
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Header Key",
                fontSize = 11.sp,
                color = TextSecondary
            )
            TextField(
                value = header.key,
                onValueChange = onKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "e.g. Authorization",
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    color = TextPrimary
                ),
                singleLine = true
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(androidx.compose.ui.graphics.Color(0xFFD1D1D6))
        )

        // Value
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Header Value",
                fontSize = 11.sp,
                color = TextSecondary
            )
            TextField(
                value = header.value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "e.g. Bearer token",
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    color = TextPrimary
                ),
                singleLine = true
            )
        }

        // Remove Button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .pressableScale(pressedScale = 0.95f, onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Close,
                contentDescription = "Remove",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

data class Header(
    val key: String,
    val value: String
)
