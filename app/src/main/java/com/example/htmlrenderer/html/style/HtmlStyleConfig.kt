package com.example.htmlrenderer.html.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Per-tag style configuration for `HtmlText`. Override only the tags you care about; everything
 * else keeps a sensible default.
 */
data class HtmlStyleConfig(
    val paragraph: SpanStyle = SpanStyle(fontSize = 16.sp, color = Color.Black),
    val bold: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
    val italic: SpanStyle = SpanStyle(fontStyle = FontStyle.Italic),
    val underline: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline),
    val link: SpanStyle = SpanStyle(color = Color(0xFF1565C0), textDecoration = TextDecoration.Underline),
    val h1: SpanStyle = SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    val h2: SpanStyle = SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    val h3: SpanStyle = SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    val h4: SpanStyle = SpanStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
    val blockSpacing: Dp = 8.dp,
    val unorderedList: ListStyle = ListStyle(marker = ListMarker.Bullet()),
    val orderedList: ListStyle = ListStyle(marker = ListMarker.Number()),
)

/** Styling for a `<ul>`/`<ol>` block: marker look, spacing between items, and indent per nesting level. */
data class ListStyle(
    val marker: ListMarker,
    val itemTextStyle: SpanStyle = SpanStyle(fontSize = 16.sp),
    val itemSpacing: Dp = 4.dp,
    val indentPerLevel: Dp = 20.dp,
    val markerWidth: Dp = 24.dp,
)

/** What to draw in front of each `<li>`. */
sealed class ListMarker {
    data class Bullet(
        val symbol: String = "•",
        val color: Color = Color.DarkGray,
        val fontSize: TextUnit? = null,
    ) : ListMarker()

    data class Number(
        val color: Color = Color.DarkGray,
        val fontSize: TextUnit? = null,
        val format: (Int) -> String = { "$it." },
    ) : ListMarker()
}
