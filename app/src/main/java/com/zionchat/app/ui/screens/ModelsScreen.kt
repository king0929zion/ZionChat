package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun ModelsScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectAll by remember { mutableStateOf(false) }
    var showAddModal by remember { mutableStateOf(false) }

    val models = remember {
        mutableStateListOf(
            Model("deepseek-chat", "DeepSeek", true),
            Model("gpt-4o", "GPT-4o", false),
            Model("gpt-4o-mini", "GPT-4o Mini", false),
            Model("claude-3-sonnet", "Claude 3 Sonnet", false),
            Model("claude-3-opus", "Claude 3 Opus", false),
            Model("gemini-pro", "Gemini Pro", false)
        )
    }

    var selectedCount by remember { mutableIntStateOf(models.count { it.enabled }) }

    Box(modifier = Modifier.fillMaxSize()) {
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

            // Search Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
                    .background(GrayLight, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Name or ID",
                    fontSize = 17.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Select All Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
                    .background(GrayLight, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Select all",
                    fontSize = 17.sp,
                    color = TextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "$selectedCount / ${models.size} selected",
                        fontSize = 15.sp,
                        color = TextSecondary
                    )

                    // Toggle Switch
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(32.dp)
                            .background(
                                if (selectAll) Color(0xFF34C759) else GrayLight,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(2.dp)
                            .clickable {
                                selectAll = !selectAll
                                models.forEach { it.enabled = selectAll }
                                selectedCount = if (selectAll) models.size else 0
                            },
                        contentAlignment = if (selectAll) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Surface, CircleShape)
                        )
                    }
                }
            }

            // Model List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                models.forEach { model ->
                    ModelItem(
                        model = model,
                        onToggle = {
                            model.enabled = !model.enabled
                            selectedCount = models.count { it.enabled }
                            selectAll = selectedCount == models.size
                        },
                        onClick = {
                            navController.navigate("model_config?id=${model.id}")
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Add Model Modal
        if (showAddModal) {
            AddModelModal(
                onDismiss = { showAddModal = false },
                onAdd = { id, name ->
                    models.add(Model(id, name, false))
                    showAddModal = false
                }
            )
        }
    }
}

@Composable
fun ModelItem(
    model: Model,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(GrayLight, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = model.name,
            fontSize = 17.sp,
            color = TextPrimary
        )

        // Toggle Switch
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(32.dp)
                .background(
                    if (model.enabled) TextPrimary else Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
                .border(2.dp, if (model.enabled) TextPrimary else TextSecondary, RoundedCornerShape(16.dp))
                .padding(2.dp)
                .clickable { onToggle() },
            contentAlignment = if (model.enabled) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (model.enabled) Surface else TextSecondary,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun AddModelModal(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var modelId by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
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

data class Model(
    val id: String,
    val name: String,
    var enabled: Boolean
)
