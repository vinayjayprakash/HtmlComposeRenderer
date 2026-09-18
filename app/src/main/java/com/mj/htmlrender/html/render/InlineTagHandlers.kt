package com.mj.htmlrender.html.render

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import com.mj.htmlrender.html.model.HtmlNode
import com.mj.htmlrender.html.style.HtmlStyleConfig

internal const val LINK_ANNOTATION_TAG = "URL"

/**
 * Strategy for rendering one inline element tag into an [AnnotatedString.Builder] (the receiver).
 * [renderChildren] recurses back into [HtmlInlineRenderer] so a handler never needs to know how
 * its own children get rendered - it only wraps them in the right style.
 */
internal typealias InlineTagHandler = AnnotatedString.Builder.(
    element: HtmlNode.Element,
    style: HtmlStyleConfig,
    renderChildren: (List<HtmlNode>) -> Unit,
) -> Unit

private val lineBreakTagHandler: InlineTagHandler = { _, _, _ -> append("\n") }

private val linkTagHandler: InlineTagHandler = { element, style, renderChildren ->
    val href = element.attributes["href"].orEmpty()
    val start = length
    withStyle(style.link) { renderChildren(element.children) }
    addStringAnnotation(tag = LINK_ANNOTATION_TAG, annotation = href, start = start, end = length)
}

/** Covers every purely cosmetic inline tag (bold, italic, underline, strikethrough, subscript,
 * superscript): wrap the children in a single [SpanStyle] picked out of the config.
 * A plain function (not a `val`), so it is safe to reference before its own declaration point. */
private fun styledTagHandler(selectStyle: (HtmlStyleConfig) -> SpanStyle): InlineTagHandler =
    { element, style, renderChildren -> withStyle(selectStyle(style)) { renderChildren(element.children) } }

/**
 * Supported inline tags. Adding a new one means adding one handler and one entry here -
 * [HtmlInlineRenderer] itself never has to change (Open/Closed).
 */
internal val defaultInlineTagHandlers: Map<String, InlineTagHandler> = mapOf(
    "b" to styledTagHandler { it.bold },
    "strong" to styledTagHandler { it.bold },
    "i" to styledTagHandler { it.italic },
    "em" to styledTagHandler { it.italic },
    "u" to styledTagHandler { it.underline },
    "s" to styledTagHandler { it.strikethrough },
    "strike" to styledTagHandler { it.strikethrough },
    "del" to styledTagHandler { it.strikethrough },
    "sub" to styledTagHandler { it.subscript },
    "sup" to styledTagHandler { it.superscript },
    "br" to lineBreakTagHandler,
    "a" to linkTagHandler,
)
