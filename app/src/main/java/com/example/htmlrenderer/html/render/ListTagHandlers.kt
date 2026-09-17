package com.example.htmlrenderer.html.render

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.htmlrenderer.html.model.HtmlNode
import com.example.htmlrenderer.html.style.ListMarker
import com.example.htmlrenderer.html.style.ListStyle

@Composable
internal fun RenderList(
    listElement: HtmlNode.Element,
    listStyle: ListStyle,
    context: HtmlRenderContext,
    depth: Int,
) {
    Column(modifier = Modifier.padding(start = listStyle.indentPerLevel * depth)) {
        var number = 0
        listElement.children
            .filterIsInstance<HtmlNode.Element>()
            .filter { it.tag == "li" }
            .forEach { li ->
                number++
                RenderListItem(li, number, listStyle, context, depth)
            }
    }
}

@Composable
private fun RenderListItem(
    li: HtmlNode.Element,
    number: Int,
    listStyle: ListStyle,
    context: HtmlRenderContext,
    depth: Int,
) {
    // Nested <ul>/<ol> inside this <li> are rendered below the item's own text, not inline with it.
    val nestedLists = li.children.filterIsInstance<HtmlNode.Element>().filter { it.tag == "ul" || it.tag == "ol" }
    val ownContent = li.children.filterNot { it is HtmlNode.Element && (it.tag == "ul" || it.tag == "ol") }

    Column(modifier = Modifier.padding(bottom = listStyle.itemSpacing)) {
        Row {
            ListMarkerText(listStyle.marker, number, listStyle)
            HtmlParagraph(inlineAnnotatedString(listStyle.itemTextStyle, ownContent, context.style), context.onLinkClick)
        }
        nestedLists.forEach { nested ->
            val nestedStyle = if (nested.tag == "ol") context.style.orderedList else context.style.unorderedList
            RenderList(nested, nestedStyle, context, depth + 1)
        }
    }
}

@Composable
private fun ListMarkerText(marker: ListMarker, number: Int, listStyle: ListStyle) {
    when (marker) {
        is ListMarker.Bullet -> Text(
            text = marker.symbol,
            color = marker.color,
            fontSize = marker.fontSize ?: listStyle.itemTextStyle.fontSize,
            modifier = Modifier.width(listStyle.markerWidth),
        )

        is ListMarker.Number -> Text(
            text = marker.format(number),
            color = marker.color,
            fontSize = marker.fontSize ?: listStyle.itemTextStyle.fontSize,
            modifier = Modifier.width(listStyle.markerWidth),
        )
    }
}
