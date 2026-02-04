package com.zionchat.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zionchat.app.ui.theme.AccentBlue
import com.zionchat.app.ui.theme.GrayLight
import com.zionchat.app.ui.theme.GrayLighter
import com.zionchat.app.ui.theme.TextPrimary
import com.zionchat.app.ui.theme.TextSecondary
import coil3.compose.AsyncImage
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as MarkdownTextNode
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = TextPrimary),
    linkColor: Color = AccentBlue
) {
    val parser = remember { Parser.builder().build() }
    val document = remember(markdown) { parser.parse(markdown) as Document }

    Column(modifier = modifier, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        renderChildren(document, textStyle, linkColor)
    }
}

@Composable
private fun renderChildren(
    parent: Node,
    textStyle: TextStyle,
    linkColor: Color,
    spacing: Dp = 8.dp
) {
    var child = parent.firstChild
    if (child == null) return
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing)) {
        while (child != null) {
            MarkdownBlock(child, textStyle, linkColor)
            child = child.next
        }
    }
}

@Composable
private fun MarkdownBlock(
    node: Node,
    textStyle: TextStyle,
    linkColor: Color
) {
    when (node) {
        is Paragraph -> MarkdownParagraph(node, textStyle, linkColor)
        is Heading -> MarkdownHeading(node, textStyle, linkColor)
        is BulletList -> MarkdownBulletList(node, textStyle, linkColor)
        is OrderedList -> MarkdownOrderedList(node, textStyle, linkColor)
        is FencedCodeBlock -> MarkdownCodeBlock(node.literal, textStyle)
        is IndentedCodeBlock -> MarkdownCodeBlock(node.literal, textStyle)
        is BlockQuote -> MarkdownBlockQuote(node, textStyle, linkColor)
        is ThematicBreak -> Divider(color = GrayLight, thickness = 1.dp)
        else -> {
            val literal = (node as? MarkdownTextNode)?.literal.orEmpty()
            if (literal.isNotBlank()) {
                Text(text = literal, style = textStyle)
            }
        }
    }
}

@Composable
private fun MarkdownParagraph(
    paragraph: Paragraph,
    textStyle: TextStyle,
    linkColor: Color
) {
    val images = remember(paragraph) { collectInlineImages(paragraph) }
    val isOnlyImages = remember(paragraph) { isParagraphOnlyImages(paragraph) }
    val inline = buildInlineAnnotatedString(paragraph, linkColor)

    if (!isOnlyImages && inline.text.isNotBlank()) {
        Text(text = inline, style = textStyle)
    }

    if (images.isNotEmpty()) {
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            images.forEach { img ->
                val url = img.destination.orEmpty()
                if (url.isNotBlank()) {
                    val modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                    val dataBitmap = remember(url) { decodeBitmapFromDataUrl(url) }
                    if (dataBitmap != null) {
                        Image(
                            bitmap = dataBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = modifier,
                            contentScale = ContentScale.FillWidth
                        )
                    } else {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = modifier,
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }
}

private fun decodeBitmapFromDataUrl(dataUrl: String): Bitmap? {
    val url = dataUrl.trim()
    if (!url.startsWith("data:image", ignoreCase = true)) return null
    val commaIndex = url.indexOf(',')
    if (commaIndex < 0) return null
    val meta = url.substring(0, commaIndex)
    if (!meta.contains(";base64", ignoreCase = true)) return null
    val payload = url.substring(commaIndex + 1)
    if (payload.isBlank()) return null
    val decoded = runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull() ?: return null
    if (decoded.isEmpty()) return null
    return runCatching { BitmapFactory.decodeByteArray(decoded, 0, decoded.size) }.getOrNull()
}

@Composable
private fun MarkdownHeading(
    heading: Heading,
    textStyle: TextStyle,
    linkColor: Color
) {
    val size = when (heading.level) {
        1 -> 22.sp
        2 -> 20.sp
        3 -> 18.sp
        4 -> 17.sp
        else -> 16.sp
    }
    Text(
        text = buildInlineAnnotatedString(heading, linkColor),
        style = textStyle.copy(fontSize = size, fontWeight = FontWeight.SemiBold)
    )
}

@Composable
private fun MarkdownCodeBlock(
    code: String,
    textStyle: TextStyle
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayLighter, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = code.trimEnd(),
            style = textStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        )
    }
}

@Composable
private fun MarkdownBlockQuote(
    quote: BlockQuote,
    textStyle: TextStyle,
    linkColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Spacer(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(GrayLight, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            renderChildren(quote, textStyle.copy(color = TextSecondary), linkColor, spacing = 6.dp)
        }
    }
}

@Composable
private fun MarkdownBulletList(
    list: BulletList,
    textStyle: TextStyle,
    linkColor: Color
) {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        var item = list.firstChild
        while (item != null) {
            if (item is ListItem) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "\u2022", style = textStyle.copy(color = TextSecondary))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        renderChildren(item, textStyle, linkColor, spacing = 6.dp)
                    }
                }
            }
            item = item.next
        }
    }
}

@Composable
private fun MarkdownOrderedList(
    list: OrderedList,
    textStyle: TextStyle,
    linkColor: Color
) {
    var index = list.startNumber
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        var item = list.firstChild
        while (item != null) {
            if (item is ListItem) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "$index.", style = textStyle.copy(color = TextSecondary))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        renderChildren(item, textStyle, linkColor, spacing = 6.dp)
                    }
                }
                index += 1
            }
            item = item.next
        }
    }
}

private fun isParagraphOnlyImages(paragraph: Paragraph): Boolean {
    var child = paragraph.firstChild
    if (child == null) return false
    var hasImage = false
    while (child != null) {
        when (child) {
            is Image -> hasImage = true
            is SoftLineBreak, is HardLineBreak -> Unit
            else -> return false
        }
        child = child.next
    }
    return hasImage
}

private fun collectInlineImages(parent: Node): List<Image> {
    val out = mutableListOf<Image>()
    fun walk(node: Node?) {
        var child = node?.firstChild
        while (child != null) {
            when (child) {
                is Image -> out.add(child)
                else -> walk(child)
            }
            child = child.next
        }
    }
    walk(parent)
    return out
}

private fun buildInlineAnnotatedString(parent: Node, linkColor: Color): AnnotatedString {
    val builder = AnnotatedString.Builder()

    fun appendInline(node: Node) {
        when (node) {
            is MarkdownTextNode -> builder.append(node.literal)
            is SoftLineBreak -> builder.append('\n')
            is HardLineBreak -> builder.append('\n')
            is Image -> {
                var child = node.firstChild
                while (child != null) {
                    appendInline(child)
                    child = child.next
                }
            }
            is Emphasis -> {
                val start = builder.length
                var child = node.firstChild
                while (child != null) {
                    appendInline(child)
                    child = child.next
                }
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, builder.length)
            }
            is StrongEmphasis -> {
                val start = builder.length
                var child = node.firstChild
                while (child != null) {
                    appendInline(child)
                    child = child.next
                }
                builder.addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, builder.length)
            }
            is Code -> {
                val start = builder.length
                builder.append(node.literal)
                builder.addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = GrayLighter
                    ),
                    start,
                    builder.length
                )
            }
            is Link -> {
                val start = builder.length
                var child = node.firstChild
                while (child != null) {
                    appendInline(child)
                    child = child.next
                }
                val end = builder.length
                builder.addStyle(
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    start,
                    end
                )
                if (!node.destination.isNullOrBlank()) {
                    builder.addStringAnnotation("URL", node.destination, start, end)
                }
            }
            else -> {
                var child = node.firstChild
                while (child != null) {
                    appendInline(child)
                    child = child.next
                }
            }
        }
    }

    appendInline(parent)
    return builder.toAnnotatedString()
}
