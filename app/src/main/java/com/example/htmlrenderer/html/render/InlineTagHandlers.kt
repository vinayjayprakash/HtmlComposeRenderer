package com.example.htmlrenderer.html.render

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.htmlrenderer.html.model.HtmlNode
import com.example.htmlrenderer.html.style.HtmlStyleConfig

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

private val boldTagHandler: InlineTagHandler = { element, style, renderChildren ->
    withStyle(style.bold) { renderChildren(element.children) }
}

private val italicTagHandler: InlineTagHandler = { element, style, renderChildren ->
    withStyle(style.italic) { renderChildren(element.children) }
}

private val underlineTagHandler: InlineTagHandler = { element, style, renderChildren ->
    withStyle(style.underline) { renderChildren(element.children) }
}

private val lineBreakTagHandler: InlineTagHandler = { _, _, _ -> append("\n") }

private val linkTagHandler: InlineTagHandler = { element, style, renderChildren ->
    val href = element.attributes["href"].orEmpty()
    val start = length
    withStyle(style.link) { renderChildren(element.children) }
    addStringAnnotation(tag = LINK_ANNOTATION_TAG, annotation = href, start = start, end = length)
}

/**
 * Supported inline tags. Adding a new one (e.g. `<sup>`) means adding one handler and one entry
 * here - [HtmlInlineRenderer] itself never has to change (Open/Closed).
 */
internal val defaultInlineTagHandlers: Map<String, InlineTagHandler> = mapOf(
    "b" to boldTagHandler,
    "strong" to boldTagHandler,
    "i" to italicTagHandler,
    "em" to italicTagHandler,
    "u" to underlineTagHandler,
    "br" to lineBreakTagHandler,
    "a" to linkTagHandler,
)

/** Walks an [HtmlNode] tree, dispatching each element to its registered inline handler. */
internal object HtmlInlineRenderer {
    fun render(
        builder: AnnotatedString.Builder,
        nodes: List<HtmlNode>,
        style: HtmlStyleConfig,
        handlers: Map<String, InlineTagHandler> = defaultInlineTagHandlers,
    ) {
        nodes.forEach { node -> renderNode(builder, node, style, handlers) }
    }

    private fun renderNode(
        builder: AnnotatedString.Builder,
        node: HtmlNode,
        style: HtmlStyleConfig,
        handlers: Map<String, InlineTagHandler>,
    ) {
        when (node) {
            is HtmlNode.Text -> builder.append(node.value)
            is HtmlNode.Element -> {
                val renderChildren: (List<HtmlNode>) -> Unit = { children -> render(builder, children, style, handlers) }
                val handler = handlers[node.tag]
                if (handler != null) builder.handler(node, style, renderChildren) else renderChildren(node.children)
            }
        }
    }
}
