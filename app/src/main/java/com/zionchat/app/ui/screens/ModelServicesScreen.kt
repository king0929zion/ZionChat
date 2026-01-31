package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.ui.theme.*

// Provider data class
data class Provider(
    val id: String,
    val name: String,
    val icon: String
)

@Composable
fun ModelServicesScreen(navController: NavController) {
    val providers = listOf(
        Provider("qwen", "Qwen", "🔴"),
        Provider("doubao", "Doubao", "🟢"),
        Provider("openai", "ChatGPT", "⚫"),
        Provider("anthropic", "Anthropic", "🟣"),
        Provider("google", "Google", "🔵"),
        Provider("ollama", "Ollama", "🦙"),
        Provider("deepseek", "DeepSeek", "🔷"),
        Provider("yi", "Yi", "🟡"),
        Provider("moonshot", "Moonshot", "🌙"),
        Provider("gemini", "Gemini", "💎"),
        Provider("grok", "Grok", "🤖"),
        Provider("mistral", "Mistral", "🌀"),
        Provider("groq", "Groq", "⚡"),
        Provider("azure", "Azure", "🔷"),
        Provider("claude", "Claude", "🟣"),
        Provider("baichuan", "Baichuan", "🟠"),
        Provider("zhipu", "Zhipu", "🟢"),
        Provider("minimax", "MiniMax", "🔴"),
        Provider("stepfun", "StepFun", "🟡"),
        Provider("hunyuan", "Hunyuan", "🔵"),
        Provider("silicon", "Silicon", "⚪"),
        Provider("together", "Together", "🟤"),
        Provider("perplexity", "Perplexity", "🟢"),
        Provider("openrouter", "OpenRouter", "🔴"),
        Provider("fireworks", "Fireworks", "🎆"),
        Provider("cerebras", "Cerebras", "⚫"),
        Provider("meta", "Meta", "🔵")
    )

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
                text = "Model Services",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )

            // Add Button
            IconButton(
                onClick = { navController.navigate("add_provider") },
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterEnd)
                    .background(Surface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Provider",
                    tint = TextPrimary
                )
            }
        }

        // Search Box
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
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Enter provider name",
                fontSize = 17.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
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
            providers.forEach { provider ->
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
        // Provider Icon (placeholder using emoji)
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = provider.icon,
                fontSize = 28.sp
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
