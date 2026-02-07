package com.zionchat.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AccentPalette(
    val key: String,
    val actionColor: Color,
    val bubbleColor: Color,
    val bubbleTextColor: Color,
    val bubbleColorSecondary: Color? = null,
    val thinkingPulseColor: Color = actionColor
)

private val defaultAccentPalette =
    AccentPalette(
        key = "default",
        actionColor = Color(0xFF9CA3AF),
        bubbleColor = Color(0xFFEFF2F6),
        bubbleTextColor = Color(0xFF1E2733)
    )

private val accentPaletteMap =
    mapOf(
        "default" to defaultAccentPalette,
        "blue" to
            AccentPalette(
                key = "blue",
                actionColor = Color(0xFF3B82F6),
                bubbleColor = Color(0xFFE8F1FF),
                bubbleTextColor = Color(0xFF173A66)
            ),
        "green" to
            AccentPalette(
                key = "green",
                actionColor = Color(0xFF22C55E),
                bubbleColor = Color(0xFFE7F8EE),
                bubbleTextColor = Color(0xFF174227)
            ),
        "yellow" to
            AccentPalette(
                key = "yellow",
                actionColor = Color(0xFFEAB308),
                bubbleColor = Color(0xFFFFF6DE),
                bubbleTextColor = Color(0xFF5E4700)
            ),
        "pink" to
            AccentPalette(
                key = "pink",
                actionColor = Color(0xFFEC4899),
                bubbleColor = Color(0xFFFFEBF5),
                bubbleTextColor = Color(0xFF5F1F42)
            ),
        "orange" to
            AccentPalette(
                key = "orange",
                actionColor = Color(0xFFF97316),
                bubbleColor = Color(0xFFFFEFE4),
                bubbleTextColor = Color(0xFF663410)
            ),
        "purple" to
            AccentPalette(
                key = "purple",
                actionColor = Color(0xFFA855F7),
                bubbleColor = Color(0xFFF2E9FF),
                bubbleTextColor = Color(0xFF45246E)
            ),
        "black" to
            AccentPalette(
                key = "black",
                actionColor = Color(0xFF111214),
                bubbleColor = Color(0xFF2C2D31),
                bubbleTextColor = Color(0xFFF7F7F8),
                bubbleColorSecondary = Color(0xFF16171A),
                thinkingPulseColor = Color(0xFF6B8BFF)
            )
    )

fun accentPaletteForKey(key: String?): AccentPalette {
    return accentPaletteMap[key?.trim()?.lowercase()] ?: defaultAccentPalette
}
