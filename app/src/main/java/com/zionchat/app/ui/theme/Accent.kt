package com.zionchat.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AccentPalette(
    val key: String,
    val actionColor: Color,
    val bubbleColor: Color,
    val bubbleTextColor: Color
)

private val defaultAccentPalette =
    AccentPalette(
        key = "default",
        actionColor = Color(0xFF5E6876),
        bubbleColor = Color(0xFFE9EDF3),
        bubbleTextColor = Color(0xFF1E2733)
    )

private val accentPaletteMap =
    mapOf(
        "default" to defaultAccentPalette,
        "blue" to
            AccentPalette(
                key = "blue",
                actionColor = Color(0xFF5D7FA6),
                bubbleColor = Color(0xFFE9F0F8),
                bubbleTextColor = Color(0xFF1F2D3D)
            ),
        "green" to
            AccentPalette(
                key = "green",
                actionColor = Color(0xFF5D8A75),
                bubbleColor = Color(0xFFEAF4EF),
                bubbleTextColor = Color(0xFF1F3229)
            ),
        "yellow" to
            AccentPalette(
                key = "yellow",
                actionColor = Color(0xFF9A8553),
                bubbleColor = Color(0xFFF5F1E8),
                bubbleTextColor = Color(0xFF3B3120)
            ),
        "pink" to
            AccentPalette(
                key = "pink",
                actionColor = Color(0xFF97657F),
                bubbleColor = Color(0xFFF5EBF1),
                bubbleTextColor = Color(0xFF352230)
            ),
        "orange" to
            AccentPalette(
                key = "orange",
                actionColor = Color(0xFFA07351),
                bubbleColor = Color(0xFFF6EEE8),
                bubbleTextColor = Color(0xFF392A20)
            ),
        "purple" to
            AccentPalette(
                key = "purple",
                actionColor = Color(0xFF74669C),
                bubbleColor = Color(0xFFEEEAF7),
                bubbleTextColor = Color(0xFF2B2540)
            )
    )

fun accentPaletteForKey(key: String?): AccentPalette {
    return accentPaletteMap[key?.trim()?.lowercase()] ?: defaultAccentPalette
}

