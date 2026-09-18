package com.mj.htmlrender.html.render

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import com.mj.htmlrender.html.model.HtmlNode
import com.mj.htmlrender.html.style.HtmlStyleConfig

internal const val LINK_ANNOTATION_TAG = "URL"

/**
 * Strategy for rendering one inline element tag into an [AnnotatedString.Builder]. Implement
 * [render] in an object (or class) and register it in [defaultInlineTagHandlers].
 * [renderChildren] recurses back into [HtmlInlineRenderer] so a handler never needs to know how
 * its own children get rendered - it only wraps them in the right style.
 */
internal interface InlineTagHandler {
    fun render(
        builder: AnnotatedString.Builder,
        element: HtmlNode.Element,
        style: HtmlStyleConfig,
        renderChildren: (List<HtmlNode>) -> Unit,
    )
}

/** Covers every purely cosmetic inline tag (bold, italic, underline, strikethrough, subscript,
 * superscript): wrap the children in a single [SpanStyle] picked out of the config. */
private class StyledTagHandler(private val selectStyle: (HtmlStyleConfig) -> SpanStyle) : InlineTagHandler {
    override fun render(builder: AnnotatedString.Builder, element: HtmlNode.Element, style: HtmlStyleConfig, renderChildren: (List<HtmlNode>) -> Unit) {
        builder.withStyle(selectStyle(style)) { renderChildren(element.children) }
    }
}

private object LineBreakTagHandler : InlineTagHandler {
    override fun render(builder: AnnotatedString.Builder, element: HtmlNode.Element, style: HtmlStyleConfig, renderChildren: (List<HtmlNode>) -> Unit) {
        builder.append("\n")
    }
}

private object LinkTagHandler : InlineTagHandler {
    override fun render(builder: AnnotatedString.Builder, element: HtmlNode.Element, style: HtmlStyleConfig, renderChildren: (List<HtmlNode>) -> Unit) {
        val href = element.attributes["href"].orEmpty()
        val start = builder.length
        builder.withStyle(style.link) { renderChildren(element.children) }
        builder.addStringAnnotation(tag = LINK_ANNOTATION_TAG, annotation = href, start = start, end = builder.length)
    }
}

/**
 * Supported inline tags. Adding a new one means adding one handler and one entry here -
 * [HtmlInlineRenderer] itself never has to change (Open/Closed).
 */
internal val defaultInlineTagHandlers: Map<String, InlineTagHandler> = mapOf(
    "b" to StyledTagHandler { it.bold },
    "strong" to StyledTagHandler { it.bold },
    "i" to StyledTagHandler { it.italic },
    "em" to StyledTagHandler { it.italic },
    "u" to StyledTagHandler { it.underline },
    "s" to StyledTagHandler { it.strikethrough },
    "strike" to StyledTagHandler { it.strikethrough },
    "del" to StyledTagHandler { it.strikethrough },
    "sub" to StyledTagHandler { it.subscript },
    "sup" to StyledTagHandler { it.superscript },
    "br" to LineBreakTagHandler,
    "a" to LinkTagHandler,
)
