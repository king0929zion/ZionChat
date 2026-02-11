package com.zionchat.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun Modifier.pressableScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 使用 spring 动画实现更丝滑的效果
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
            visibilityThreshold = 0.001f
        ),
        label = "pressableScale"
    )
    
    // 添加轻微的透明度变化
    val alpha by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressableAlpha"
    )
    
    // 添加轻微的 Y 轴位移，模拟按压感
    val translationY by animateFloatAsState(
        targetValue = if (enabled && isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressableTranslationY"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
            this.translationY = translationY
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

// 增强版按压效果，适合卡片式布局
@Composable
fun Modifier.pressableCard(
    enabled: Boolean = true,
    pressedScale: Float = 0.98f,
    pressedElevation: Float = 2f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
            visibilityThreshold = 0.001f
        ),
        label = "cardScale"
    )
    
    // 阴影动画
    val shadowElevation by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedElevation else 8f,
        animationSpec = tween(durationMillis = 150),
        label = "cardElevation"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            shadowElevation = shadowElevation
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

// 列表项专用的按压效果
@Composable
fun Modifier.pressableListItem(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "listItemScale"
    )
    
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.05f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "listItemBackground"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}
