package com.zionchat.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest

@Composable
fun AssetIcon(
    assetFileName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    error: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    val model = remember(assetFileName, context) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/icons/$assetFileName")
            .build()
    }
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        error = { error?.invoke() ?: Unit }
    )
}
