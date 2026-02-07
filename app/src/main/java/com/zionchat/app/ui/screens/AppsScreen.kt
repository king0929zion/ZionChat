package com.zionchat.app.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.zionchat.app.LocalAppRepository
import com.zionchat.app.R
import com.zionchat.app.data.SavedApp
import com.zionchat.app.ui.components.pressableScale
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.Background
import com.zionchat.app.ui.theme.SourceSans3
import com.zionchat.app.ui.theme.TextPrimary
import com.zionchat.app.ui.theme.TextSecondary
import com.zionchat.app.ui.theme.Surface as SurfaceColor
import androidx.compose.ui.res.stringResource

private data class AppItemUi(
    val name: String,
    val subtitle: String,
    val icon: AppIconStyle,
    val selected: Boolean = false
)

private enum class AppIconStyle {
    Airtable,
    AppleMusic,
    Booking,
    Canva,
    Figma,
    Lovable,
    OpenTable
}

@Composable
fun AppsScreen(navController: NavController) {
    val repository = LocalAppRepository.current
    val savedApps by repository.savedAppsFlow.collectAsState(initial = emptyList())
    val categories = remember { listOf("Featured", "Lifestyle", "Productivity") }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var selectedSavedApp by remember { mutableStateOf<SavedApp?>(null) }
    val featuredApps = remember {
        listOf(
            AppItemUi("Airtable", "Add structured data to ChatGPT", AppIconStyle.Airtable),
            AppItemUi("Apple Music", "Build playlists and find music", AppIconStyle.AppleMusic),
            AppItemUi("Booking.com", "Find hotels, homes and more", AppIconStyle.Booking),
            AppItemUi("Canva", "Search, create, edit designs", AppIconStyle.Canva),
            AppItemUi("Figma", "Make diagrams, slides, assets", AppIconStyle.Figma),
            AppItemUi("Lovable", "Build apps and websites", AppIconStyle.Lovable, selected = true),
            AppItemUi("OpenTable", "Find restaurant reservations", AppIconStyle.OpenTable)
        )
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            AppsTopBar(
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate("chat") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isActive = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isActive) Color(0xFFEDEDF2) else Color.Transparent, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "saved_title") {
                    Text(
                        text = stringResource(R.string.apps_my_apps),
                        fontSize = 13.sp,
                        fontFamily = SourceSans3,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    )
                }

                if (savedApps.isEmpty()) {
                    item(key = "saved_empty") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = SurfaceColor
                        ) {
                            Text(
                                text = stringResource(R.string.apps_empty_saved),
                                fontSize = 14.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                            )
                        }
                    }
                } else {
                    items(items = savedApps, key = { it.id }) { app ->
                        SavedAppRow(
                            app = app,
                            onClick = { selectedSavedApp = app }
                        )
                    }
                }

                item(key = "featured_title") {
                    Text(
                        text = stringResource(R.string.apps_featured),
                        fontSize = 13.sp,
                        fontFamily = SourceSans3,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                items(items = featuredApps, key = { it.name }) { item ->
                    AppItemRow(item = item, onClick = { })
                }
            }
        }
    }

    selectedSavedApp?.let { app ->
        SavedAppPreviewDialog(
            app = app,
            onDismiss = { selectedSavedApp = null }
        )
    }
}

@Composable
private fun AppsTopBar(
    onBack: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceColor, CircleShape)
                .pressableScale(pressedScale = 0.95f, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Back,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.apps),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceColor, CircleShape)
                .pressableScale(pressedScale = 0.95f, onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Plus,
                contentDescription = "Add",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SavedAppRow(
    app: SavedApp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedBackground by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFF1F1F4) else Color.White,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "saved_app_pressed_bg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pressedBackground, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Apps,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.description.ifBlank { stringResource(R.string.apps_saved_from_chat) },
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SavedAppPreviewDialog(
    app: SavedApp,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceColor, CircleShape)
                            .pressableScale(pressedScale = 0.95f, onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Back,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = app.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceColor
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(
                                null,
                                app.html,
                                "text/html",
                                "utf-8",
                                null
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppItemRow(
    item: AppItemUi,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedBackground by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFF1F1F4) else Color.Transparent,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "app_item_pressed_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pressedBackground, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(style = item.icon, selected = item.selected)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary
            )
            Text(
                text = item.subtitle,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AppIcon(
    style: AppIconStyle,
    selected: Boolean
) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            AppIconStyle.Airtable -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "A", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            AppIconStyle.AppleMusic -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(listOf(Color(0xFFFA57C1), Color(0xFFF44336))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "♪", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            AppIconStyle.Booking -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF003B95), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "B.", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            AppIconStyle.Canva -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(listOf(Color(0xFF00C4CC), Color(0xFF7B2CBF))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "C", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            AppIconStyle.Figma -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White, CircleShape)
                        .border(width = 1.dp, color = Color(0xFFE5E5EA), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(28.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopStart)
                                .clip(CircleShape)
                                .background(Color(0xFFF24E1E), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(Color(0xFFFF7262), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.BottomStart)
                                .clip(CircleShape)
                                .background(Color(0xFFA259FF), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.CenterEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF1ABCFE), CircleShape)
                        )
                    }
                }
            }
            AppIconStyle.Lovable -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFFE66D))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "\u2665", fontSize = 20.sp, color = Color.White)
                }
            }
            AppIconStyle.OpenTable -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDA3743), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(20.dp)) {
                        Dot(Modifier.align(Alignment.Center), 6.dp)
                        Dot(Modifier.align(Alignment.TopCenter), 4.dp)
                        Dot(Modifier.align(Alignment.BottomCenter), 4.dp)
                        Dot(Modifier.align(Alignment.CenterStart), 4.dp)
                        Dot(Modifier.align(Alignment.CenterEnd), 4.dp)
                    }
                }
            }
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black, CircleShape)
                    .border(width = 2.dp, color = Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun Dot(modifier: Modifier, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White, CircleShape)
    )
}
