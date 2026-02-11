package com.zionchat.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.zionchat.app.ui.theme.GrayLight
import com.zionchat.app.ui.theme.Surface as ZionSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    enableDragToDismiss: Boolean = true,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableFloatStateOf(0f) }
    
    // 使用 spring 动画实现拖拽后的回弹/关闭效果
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dragOffset"
    )
    
    // 根据拖拽距离计算透明度
    val alpha by animateFloatAsState(
        targetValue = if (dragOffset > 300f) 0.7f else 1f,
        animationSpec = tween(150),
        label = "sheetAlpha"
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = ZionSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { 
            AppSheetDragHandle(
                dragOffset = animatedOffset,
                onDrag = { delta ->
                    if (enableDragToDismiss) {
                        dragOffset = (dragOffset + delta).coerceAtLeast(0f)
                    }
                },
                onDragEnd = {
                    if (dragOffset > 200f) {
                        scope.launch { 
                            dragOffset = 0f
                            sheetState.hide()
                            onDismissRequest()
                        }
                    } else {
                        dragOffset = 0f
                    }
                }
            ) 
        },
        modifier = modifier
            .graphicsLayer {
                translationY = animatedOffset
                this.alpha = alpha
            }
    ) {
        content()
    }
}

@Composable
fun AppSheetDragHandle(
    dragOffset: Float = 0f,
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    backgroundColor: Color = ZionSurface,
    handleColor: Color = GrayLight
) {
    val scope = rememberCoroutineScope()
    
    // 拖拽时的缩放效果
    val scale by animateFloatAsState(
        targetValue = if (dragOffset > 0) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "handleScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    },
                    onDragEnd = {
                        onDragEnd()
                    }
                )
            }
            .padding(top = 12.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .width(40.dp)
                .height(4.dp)
                .background(handleColor, RoundedCornerShape(2.dp))
        )
    }
}
