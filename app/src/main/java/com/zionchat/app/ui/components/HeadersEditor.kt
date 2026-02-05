package com.zionchat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.GrayLighter
import com.zionchat.app.ui.theme.SourceSans3
import com.zionchat.app.ui.theme.Surface
import com.zionchat.app.ui.theme.TextPrimary
import com.zionchat.app.ui.theme.TextSecondary
import androidx.compose.material3.Surface as M3Surface

data class EditableHeader(
    val key: String,
    val value: String
)

@Composable
fun HeadersEditorCard(
    headers: SnapshotStateList<EditableHeader>,
    modifier: Modifier = Modifier,
    title: String = "Custom Headers"
) {
    M3Surface(
        modifier = modifier.fillMaxWidth(),
        color = Surface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontFamily = SourceSans3,
                    color = TextSecondary
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GrayLighter, CircleShape)
                        .pressableScale(pressedScale = 0.95f) { headers.add(EditableHeader("", "")) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Plus,
                        contentDescription = "Add Header",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (headers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No custom headers",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    headers.forEachIndexed { index, header ->
                        HeaderItem(
                            header = header,
                            onKeyChange = { headers[index] = header.copy(key = it) },
                            onValueChange = { headers[index] = header.copy(value = it) },
                            onRemove = { headers.removeAt(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderItem(
    header: EditableHeader,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayLighter, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Header Key",
                fontSize = 11.sp,
                fontFamily = SourceSans3,
                color = TextSecondary
            )
            TextField(
                value = header.key,
                onValueChange = onKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "e.g. Authorization",
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = TextPrimary
                ),
                singleLine = true
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(Color(0xFFD1D1D6))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Header Value",
                fontSize = 11.sp,
                fontFamily = SourceSans3,
                color = TextSecondary
            )
            TextField(
                value = header.value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "e.g. Bearer token",
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = TextPrimary
                ),
                singleLine = true
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .pressableScale(pressedScale = 0.95f, onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Close,
                contentDescription = "Remove",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
