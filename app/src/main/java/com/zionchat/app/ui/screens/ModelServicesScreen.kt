package com.zionchat.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.R
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons

// Provider data class
data class Provider(
    val id: String,
    val name: String,
    val iconRes: Int
)

@Composable
fun ModelServicesScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }

    val providers = listOf(
        Provider("qwen", "Qwen", R.drawable.llm_qwen),
        Provider("doubao", "Doubao", R.drawable.llm_doubao),
        Provider("openai", "ChatGPT", R.drawable.llm_openai),
        Provider("anthropic", "Anthropic", R.drawable.llm_anthropic),
        Provider("google", "Google", R.drawable.llm_google),
        Provider("ollama", "Ollama", R.drawable.llm_ollama),
        Provider("deepseek", "DeepSeek", R.drawable.llm_deepseek),
        Provider("yi", "Yi", R.drawable.llm_yi),
        Provider("moonshot", "Moonshot", R.drawable.llm_moonshot),
        Provider("gemini", "Gemini", R.drawable.llm_gemini),
        Provider("grok", "Grok", R.drawable.llm_grok),
        Provider("mistral", "Mistral", R.drawable.llm_mistral),
        Provider("groq", "Groq", R.drawable.llm_groq),
        Provider("azure", "Azure", R.drawable.llm_azure),
        Provider("claude", "Claude", R.drawable.llm_claude),
        Provider("baichuan", "Baichuan", R.drawable.llm_baichuan),
        Provider("zhipu", "Zhipu", R.drawable.llm_zhipu),
        Provider("minimax", "MiniMax", R.drawable.llm_minimax),
        Provider("stepfun", "StepFun", R.drawable.llm_stepfun),
        Provider("hunyuan", "Hunyuan", R.drawable.llm_hunyuan),
        Provider("silicon", "Silicon", R.drawable.llm_silicon),
        Provider("together", "Together", R.drawable.llm_together),
        Provider("perplexity", "Perplexity", R.drawable.llm_perplexity),
        Provider("openrouter", "OpenRouter", R.drawable.llm_openrouter),
        Provider("fireworks", "Fireworks", R.drawable.llm_fireworks),
        Provider("cerebras", "Cerebras", R.drawable.llm_cerebras),
        Provider("meta", "Meta", R.drawable.llm_meta)
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
                        .clip(CircleShape)
                        .background(Surface, CircleShape)
                        .pressableScale(pressedScale = 0.95f) { navController.navigate("add_provider") }
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
                .windowInsetsPadding(WindowInsets.navigationBars)
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
            .clip(RoundedCornerShape(14.dp))
            .pressableScale(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Provider Icon
        Image(
            painter = painterResource(id = provider.iconRes),
            contentDescription = provider.name,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )

        // Provider Name
        Text(
            text = provider.name,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            color = TextPrimary
        )
    }
}
