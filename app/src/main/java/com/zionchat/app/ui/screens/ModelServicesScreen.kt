package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.ui.icons.AppIcons

// Provider data class
data class Provider(
    val id: String,
    val name: String,
    val iconEmoji: String
)

@Composable
fun ModelServicesScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }

    val providers = listOf(
        Provider("qwen", "Qwen", "Q"),
        Provider("doubao", "Doubao", "D"),
        Provider("openai", "ChatGPT", "C"),
        Provider("anthropic", "Anthropic", "A"),
        Provider("google", "Google", "G"),
        Provider("ollama", "Ollama", "O"),
        Provider("deepseek", "DeepSeek", "D"),
        Provider("yi", "Yi", "Y"),
        Provider("moonshot", "Moonshot", "M"),
        Provider("gemini", "Gemini", "G"),
        Provider("grok", "Grok", "G"),
        Provider("mistral", "Mistral", "M"),
        Provider("groq", "Groq", "G"),
        Provider("azure", "Azure", "A"),
        Provider("claude", "Claude", "C"),
        Provider("baichuan", "Baichuan", "B"),
        Provider("zhipu", "Zhipu", "Z"),
        Provider("minimax", "MiniMax", "M"),
        Provider("stepfun", "StepFun", "S"),
        Provider("hunyuan", "Hunyuan", "H"),
        Provider("silicon", "Silicon", "S"),
        Provider("together", "Together", "T"),
        Provider("perplexity", "Perplexity", "P"),
        Provider("openrouter", "OpenRouter", "O"),
        Provider("fireworks", "Fireworks", "F"),
        Provider("cerebras", "Cerebras", "C"),
        Provider("meta", "Meta", "M")
    )

    val filteredProviders = providers.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background.copy(alpha = 0.95f))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // 返回按钮
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Surface, CircleShape)
                        .clickable { navController.navigateUp() }
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
                    text = "Model Services",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )

                // 添加按钮
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Surface, CircleShape)
                        .clickable { navController.navigate("add_provider") }
                        .align(Alignment.CenterEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Plus,
                        contentDescription = "Add Provider",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜索框
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(48.dp)
                    .background(GrayLight, RoundedCornerShape(12.dp))
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
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Enter provider name", fontSize = 17.sp, color = TextPrimary) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // Provider List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredProviders.forEach { provider ->
                    ProviderItem(
                        provider = provider,
                        onClick = {
                            navController.navigate("add_provider?preset=${provider.id}")
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ProviderItem(
    provider: Provider,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(GrayLight, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Provider Icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Surface, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = provider.iconEmoji,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Provider Name
        Text(
            text = provider.name,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            color = TextPrimary
        )
    }
}
