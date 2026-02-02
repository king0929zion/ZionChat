package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons

private data class MemoryItemUi(
    val content: String,
    val from: String
)

@Composable
fun MemoriesScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var items by remember {
        mutableStateOf(
            listOf(
                MemoryItemUi(
                    content = "User works as a software engineer and prefers concise answers.",
                    from = "From conversation on Jan 15, 2026"
                ),
                MemoryItemUi(
                    content = "User is interested in AI technology and machine learning.",
                    from = "From conversation on Jan 14, 2026"
                ),
                MemoryItemUi(
                    content = "User likes to use bullet points for lists and explanations.",
                    from = "From conversation on Jan 12, 2026"
                ),
                MemoryItemUi(
                    content = "User prefers Kotlin and Jetpack Compose for Android development.",
                    from = "From conversation on Jan 10, 2026"
                ),
                MemoryItemUi(
                    content = "User prefers to read technical documentation in English.",
                    from = "From conversation on Jan 8, 2026"
                ),
                MemoryItemUi(
                    content = "User is learning about Android app development with Kotlin.",
                    from = "From conversation on Jan 5, 2026"
                ),
                MemoryItemUi(
                    content = "User enjoys science fiction movies and books.",
                    from = "From conversation on Jan 3, 2026"
                ),
                MemoryItemUi(
                    content = "User prefers dark mode for coding but light mode for reading.",
                    from = "From conversation on Dec 28, 2025"
                ),
                MemoryItemUi(
                    content = "User uses VS Code as primary IDE and loves keyboard shortcuts.",
                    from = "From conversation on Dec 25, 2025"
                ),
                MemoryItemUi(
                    content = "User is building a ChatGPT clone app called ZionChat.",
                    from = "From conversation on Dec 20, 2025"
                ),
                MemoryItemUi(
                    content = "User prefers examples and practical code snippets.",
                    from = "From conversation on Dec 18, 2025"
                ),
                MemoryItemUi(
                    content = "User likes clean UI and smooth animations.",
                    from = "From conversation on Dec 15, 2025"
                )
            )
        )
    }

    val filtered = remember(items, query) {
        if (query.isBlank()) items
        else items.filter { it.content.contains(query, ignoreCase = true) || it.from.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        MemoriesTopBar(navController)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 32.dp),
                        placeholder = {
                            Text(
                                text = "Search memories...",
                                fontSize = 16.sp,
                                color = Color(0xFFC7C7CC)
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = TextPrimary),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = TextPrimary
                        )
                    )

                    androidx.compose.material3.Icon(
                        imageVector = AppIcons.Search,
                        contentDescription = null,
                        tint = Color(0xFFC7C7CC),
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.CenterEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Count + Clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${items.size} memories",
                    fontSize = 15.sp,
                    color = TextSecondary
                )
                TextButton(
                    onClick = { items = emptyList() }
                ) {
                    Text(
                        text = "Clear All",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF3B30)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filtered.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressableScale(pressedScale = 0.98f, onClick = { }),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.content,
                                    fontSize = 16.sp,
                                    color = TextPrimary,
                                    lineHeight = 22.sp
                                )
                                Text(
                                    text = item.from,
                                    fontSize = 13.sp,
                                    color = Color(0xFFB0B0B5),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        items = items.filterNot { it === item }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = AppIcons.Trash,
                                    contentDescription = "Delete",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(20.dp)
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
private fun MemoriesTopBar(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background.copy(alpha = 0.95f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Surface, CircleShape)
                .pressableScale(pressedScale = 0.95f) { navController.navigateUp() }
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = AppIcons.Back,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "Memories",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

