package com.zionchat.app.ui.screens.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Image processing utilities for ChatScreen
 */

/**
 * Encode an image attachment to a data URL
 */
internal suspend fun encodeImageAttachmentToDataUrl(
    context: Context,
    attachment: PendingImageAttachment,
    maxDimension: Int = 1024,
    jpegQuality: Int = 85
): String? {
    return withContext(Dispatchers.IO) {
        val bitmap =
            attachment.bitmap
                ?: attachment.uri?.let { uri -> decodeBitmapFromUri(context, uri, maxDimension) }
                ?: return@withContext null

        val scaled = scaleBitmapDown(bitmap, maxDimension)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(1, 100), out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        "data:image/jpeg;base64,$b64"
    }
}

/**
 * Decode a bitmap from a URI with size constraints
 */
internal fun decodeBitmapFromUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    val resolver = context.contentResolver
    val bounds =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

    resolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
    val opts =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            this.inSampleSize = inSampleSize
        }

    return resolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, opts)
    }
}

/**
 * Calculate inSampleSize for bitmap decoding
 */
internal fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    val maxDim = maxOf(width, height)
    if (maxDim <= maxDimension) return 1
    var sample = 1
    while (maxDim / sample > maxDimension) {
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

/**
 * Scale a bitmap down to fit within max dimension
 */
internal fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width.coerceAtLeast(1)
    val height = bitmap.height.coerceAtLeast(1)
    val maxDim = maxOf(width, height)
    if (maxDim <= maxDimension) return bitmap
    val scale = maxDimension.toFloat() / maxDim.toFloat()
    val newWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val newHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}
