package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.LocalAppRepository
import com.zionchat.app.data.DEFAULT_PROVIDER_PRESETS
import com.zionchat.app.data.ProviderConfig
import com.zionchat.app.data.ProviderPreset
import com.zionchat.app.data.resolveProviderIconAsset
import com.zionchat.app.ui.components.AssetIcon
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons

@Composable
fun ModelServicesScreen(navController: NavController) {
    val repository = LocalAppRepository.current
    var searchQuery by remember { mutableStateOf("") }

    val configuredProviders by repository.providersFlow.collectAsState(initial = emptyList())

    val filteredConfiguredProviders = remember(configuredProviders, searchQuery) {
        if (searchQuery.isBlank()) configuredProviders
        else configuredProviders.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val filteredPresetProviders = remember(searchQuery) {
        if (searchQuery.isBlank()) DEFAULT_PROVIDER_PRESETS
        else DEFAULT_PROVIDER_PRESETS.filter { it.name.contains(searchQuery, ignoreCase = true) }
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
                        .pressableScale(pressedScale = 0.95f) { navController.navigate("add_provider?preset=&providerId=") }
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
                    placeholder = {
                        Text(
                            text = "Enter provider name",
                            fontSize = 17.sp,
                            color = TextSecondary
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = TextPrimary
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
                filteredConfiguredProviders.forEach { provider ->
                    ConfiguredProviderItem(
                        provider = provider,
                        iconAsset = resolveProviderIconAsset(provider),
                        onClick = {
                            navController.navigate("add_provider?preset=&providerId=${provider.id}")
                        }
                    )
                }

                if (filteredConfiguredProviders.isNotEmpty() && filteredPresetProviders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = GrayLight, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                }

                filteredPresetProviders.forEach { provider ->
                    ProviderItem(
                        provider = provider,
                        onClick = {
                            navController.navigate("add_provider?preset=${provider.id}&providerId=")
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProviderItem(
    provider: ProviderPreset,
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
        ProviderIcon(
            iconAsset = provider.iconAsset,
            contentDescription = provider.name,
            modifier = Modifier.size(36.dp)
        )

        Text(
            text = provider.name,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun ConfiguredProviderItem(
    provider: ProviderConfig,
    iconAsset: String?,
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
        ProviderIcon(
            iconAsset = iconAsset,
            contentDescription = provider.name,
            modifier = Modifier.size(36.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = provider.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = provider.apiUrl,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProviderIcon(
    iconAsset: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (!iconAsset.isNullOrBlank()) {
        AssetIcon(
            assetFileName = iconAsset,
            contentDescription = contentDescription,
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
        return
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AppIcons.ChatGPTLogo,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
