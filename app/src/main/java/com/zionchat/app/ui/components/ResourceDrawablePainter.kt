package com.zionchat.app.ui.components

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

@Composable
fun rememberResourceDrawablePainter(@DrawableRes resId: Int): Painter {
    val context = LocalContext.current
    val drawable = remember(resId, context) { ContextCompat.getDrawable(context, resId)?.mutate() }
    return remember(drawable) { AndroidDrawablePainter(drawable) }
}

private class AndroidDrawablePainter(
    private val drawable: Drawable?
) : Painter() {
    private var resolvedAlpha: Int = 255
    private var resolvedColorFilter: android.graphics.ColorFilter? = null

    override val intrinsicSize: Size
        get() {
            val width = drawable?.intrinsicWidth ?: -1
            val height = drawable?.intrinsicHeight ?: -1
            return if (width > 0 && height > 0) Size(width.toFloat(), height.toFloat()) else Size.Unspecified
        }

    override fun DrawScope.onDraw() {
        val target = drawable ?: return
        target.alpha = resolvedAlpha
        target.colorFilter = resolvedColorFilter
        target.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
        drawIntoCanvas { canvas ->
            target.draw(canvas.nativeCanvas)
        }
    }

    override fun applyAlpha(alpha: Float): Boolean {
        resolvedAlpha = (alpha * 255).roundToInt().coerceIn(0, 255)
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        resolvedColorFilter = colorFilter?.asAndroidColorFilter()
        return true
    }
}

