package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.LocalAppRepository
import com.zionchat.app.data.DEFAULT_PROVIDER_PRESETS
import com.zionchat.app.data.ProviderConfig
import com.zionchat.app.data.ProviderPreset
import com.zionchat.app.data.resolveProviderIconAsset
import com.zionchat.app.ui.components.AssetIcon
import com.zionchat.app.ui.components.PageTopBar
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ModelServicesScreen(navController: NavController) {
    val repository = LocalAppRepository.current
    val scope = rememberCoroutineScope()

    val configuredProviders by repository.providersFlow.collectAsState(initial = emptyList())
    val oauthPresetIds = remember { setOf("codex", "iflow", "antigravity") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        PageTopBar(
            title = "Model services",
            onBack = { navController.navigateUp() },
            trailing = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Surface, CircleShape)
                        .pressableScale(pressedScale = 0.95f) {
                            navController.navigate("add_provider?preset=&providerId=")
                        },
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
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            configuredProviders.forEach { provider ->
                SwipeableConfiguredProviderItem(
                    provider = provider,
                    iconAsset = resolveProviderIconAsset(provider),
                    onClick = {
                        val oauthProvider = provider.oauthProvider?.trim()?.lowercase().orEmpty()
                        if (oauthProvider.isNotBlank()) {
                            navController.navigate("add_oauth_provider?provider=$oauthProvider&providerId=${provider.id}")
                        } else {
                            navController.navigate("add_provider?preset=&providerId=${provider.id}")
                        }
                    },
                    onDelete = { scope.launch { repository.deleteProviderAndModels(provider.id) } }
                )
            }

            if (configuredProviders.isNotEmpty() && DEFAULT_PROVIDER_PRESETS.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = GrayLight, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
            }

            DEFAULT_PROVIDER_PRESETS.forEach { provider ->
                ProviderItem(
                    provider = provider,
                    oauthBadge = oauthPresetIds.contains(provider.id),
                    onClick = {
                        if (oauthPresetIds.contains(provider.id)) {
                            navController.navigate("add_oauth_provider?provider=${provider.id}&providerId=")
                        } else {
                            navController.navigate("add_provider?preset=${provider.id}&providerId=")
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProviderItem(
    provider: ProviderPreset,
    oauthBadge: Boolean = false,
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
            modifier = Modifier.size(28.dp)
        )

        Text(
            text = provider.name,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = SourceSans3,
            color = TextPrimary,
            maxLines = 1
        )

        Spacer(modifier = Modifier.weight(1f))
        if (oauthBadge) OAuthBadge()
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun SwipeableConfiguredProviderItem(
    provider: ProviderConfig,
    iconAsset: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val itemScope = rememberCoroutineScope()
    val actionWidth = 72.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val anchors = remember(actionWidthPx) { mapOf(0f to 0, -actionWidthPx to 1) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(actionWidth)
                .align(Alignment.CenterEnd),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDelete()
                        itemScope.launch { swipeableState.animateTo(0) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Trash,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
                .background(GrayLight, RoundedCornerShape(14.dp))
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
                    orientation = Orientation.Horizontal
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (swipeableState.currentValue != 0) {
                        itemScope.launch { swipeableState.animateTo(0) }
                    } else {
                        onClick()
                    }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProviderIcon(
                iconAsset = iconAsset,
                contentDescription = provider.name,
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = provider.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = SourceSans3,
                color = TextPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            if (!provider.oauthProvider.isNullOrBlank()) {
                OAuthBadge()
            }
        }
    }
}

@Composable
private fun OAuthBadge() {
    Box(
        modifier = Modifier
            .height(22.dp)
            .background(Color(0xFFEDEDED), RoundedCornerShape(11.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "OAuth",
            fontSize = 12.sp,
            fontFamily = SourceSans3,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            maxLines = 1
        )
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
            contentScale = ContentScale.Fit,
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
