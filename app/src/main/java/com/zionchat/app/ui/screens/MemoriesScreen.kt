package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.ui.components.FloatingTopBar
import com.zionchat.app.ui.components.TopFadeScrim
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.*

private data class MemoryItemUi(
    val content: String,
    val from: String
)

@Composable
fun MemoriesScreen(navController: NavController) {
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

    val contentTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 86.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = contentTopPadding)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

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
                    fontFamily = SourceSans3,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                )
                TextButton(onClick = { items = emptyList() }) {
                    Text(
                        text = "Clear All",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = SourceSans3,
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
                items.forEach { item ->
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

        TopFadeScrim(
            color = Background,
            height = 64.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
        )

        FloatingTopBar(
            title = "Memories",
            onBack = { navController.navigateUp() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
