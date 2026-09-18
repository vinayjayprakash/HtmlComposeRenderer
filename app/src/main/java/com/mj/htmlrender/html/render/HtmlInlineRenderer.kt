package com.mj.htmlrender.html.render

import androidx.compose.ui.text.AnnotatedString
import com.mj.htmlrender.html.model.HtmlNode
import com.mj.htmlrender.html.style.HtmlStyleConfig

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
                if (handler != null) handler.render(builder, node, style, renderChildren) else renderChildren(node.children)
            }
        }
    }
}
