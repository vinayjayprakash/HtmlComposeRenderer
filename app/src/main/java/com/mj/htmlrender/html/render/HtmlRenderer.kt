package com.mj.htmlrender.html.render

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mj.htmlrender.html.model.HtmlNode

/**
 * One piece of top-level content, grouped for rendering: either a node whose tag has its own
 * [BlockTagHandler], or a run of consecutive siblings with no block tag of their own (bare text
 * and/or inline tags like `<b>`/`<a>` with no wrapping `<p>`) that must be rendered together as
 * one implicit paragraph - otherwise each sibling would end up on its own line and inline tags
 * would lose their styling, since [InlineTagHandler]s only apply within a single [HtmlParagraph].
 */
private sealed interface RenderChunk {
    data class Block(val element: HtmlNode.Element) : RenderChunk
    data class Implicit(val nodes: List<HtmlNode>) : RenderChunk
}

private fun groupIntoChunks(nodes: List<HtmlNode>, handlers: Map<String, BlockTagHandler>): List<RenderChunk> {
    val chunks = mutableListOf<RenderChunk>()
    var run = mutableListOf<HtmlNode>()
    fun flushRun() {
        if (run.isNotEmpty()) {
            chunks.add(RenderChunk.Implicit(run))
            run = mutableListOf()
        }
    }
    nodes.forEach { node ->
        if (node is HtmlNode.Element && handlers.containsKey(node.tag)) {
            flushRun()
            chunks.add(RenderChunk.Block(node))
        } else {
            run.add(node)
        }
    }
    flushRun()
    return chunks
}

/** Walks a top-level [HtmlNode] list, dispatching each element to its registered block handler. */
internal object HtmlRenderer {
    @Composable
    fun Render(
        nodes: List<HtmlNode>,
        context: HtmlRenderContext,
        handlers: Map<String, BlockTagHandler> = defaultBlockTagHandlers,
    ) {
        groupIntoChunks(nodes, handlers).forEach { chunk ->
            when (chunk) {
                is RenderChunk.Block -> {
                    val handler = handlers.getValue(chunk.element.tag)
                    handler.Render(chunk.element, context) { children -> Render(children, context, handlers) }
                }
                is RenderChunk.Implicit -> RenderImplicitParagraph(chunk.nodes, context)
            }
        }
    }

    @Composable
    private fun RenderImplicitParagraph(nodes: List<HtmlNode>, context: HtmlRenderContext) {
        val annotated = inlineAnnotatedString(context.style.paragraph, nodes, context.style)
        if (annotated.text.isNotBlank()) {
            HtmlParagraph(annotated, context.onLinkClick)
            Spacer(Modifier.height(context.style.blockSpacing))
        }
    }
}
