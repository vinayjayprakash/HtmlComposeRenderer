package com.example.htmlrenderer.html.render

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.htmlrenderer.html.model.HtmlNode
import com.example.htmlrenderer.html.style.HtmlStyleConfig

/**
 * Strategy for rendering one block-level element tag. [renderChildBlocks] recurses back into
 * [HtmlBlockRenderer] for any nested block content a handler needs to delegate (list handlers
 * use this for nested `<ul>`/`<ol>`).
 */
internal typealias BlockTagHandler = @Composable (
    element: HtmlNode.Element,
    context: HtmlRenderContext,
    renderChildBlocks: @Composable (List<HtmlNode>) -> Unit,
) -> Unit

private val paragraphTagHandler: BlockTagHandler = { element, context, _ ->
    HtmlParagraph(inlineAnnotatedString(context.style.paragraph, element.children, context.style), context.onLinkClick)
    Spacer(Modifier.height(context.style.blockSpacing))
}

private val lineBreakBlockTagHandler: BlockTagHandler = { _, context, _ ->
    Spacer(Modifier.height(context.style.blockSpacing / 2))
}

private val unorderedListTagHandler: BlockTagHandler = { element, context, _ ->
    RenderList(element, context.style.unorderedList, context, depth = 0)
    Spacer(Modifier.height(context.style.blockSpacing))
}

private val orderedListTagHandler: BlockTagHandler = { element, context, _ ->
    RenderList(element, context.style.orderedList, context, depth = 0)
    Spacer(Modifier.height(context.style.blockSpacing))
}

/** A plain function (not a `val`), so it is safe to reference before its own declaration point. */
private fun headingTagHandler(headingStyle: (HtmlStyleConfig) -> SpanStyle): BlockTagHandler =
    { element, context, _ ->
        HtmlParagraph(inlineAnnotatedString(headingStyle(context.style), element.children, context.style), context.onLinkClick)
        Spacer(Modifier.height(context.style.blockSpacing))
    }

/**
 * Supported block tags. Adding a new one means adding one handler and one entry here -
 * [HtmlBlockRenderer] itself never has to change (Open/Closed).
 */
internal val defaultBlockTagHandlers: Map<String, BlockTagHandler> = mapOf(
    "p" to paragraphTagHandler,
    "h1" to headingTagHandler { it.h1 },
    "h2" to headingTagHandler { it.h2 },
    "h3" to headingTagHandler { it.h3 },
    "h4" to headingTagHandler { it.h4 },
    "ul" to unorderedListTagHandler,
    "ol" to orderedListTagHandler,
    "br" to lineBreakBlockTagHandler,
)

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
internal object HtmlBlockRenderer {
    @Composable
    fun RenderAll(
        nodes: List<HtmlNode>,
        context: HtmlRenderContext,
        handlers: Map<String, BlockTagHandler> = defaultBlockTagHandlers,
    ) {
        groupIntoChunks(nodes, handlers).forEach { chunk ->
            when (chunk) {
                is RenderChunk.Block -> {
                    val handler = handlers.getValue(chunk.element.tag)
                    handler(chunk.element, context) { children -> RenderAll(children, context, handlers) }
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

@Composable
internal fun HtmlParagraph(annotated: AnnotatedString, onLinkClick: (String) -> Unit) {
    ClickableText(
        text = annotated,
        onClick = { offset ->
            annotated.getStringAnnotations(LINK_ANNOTATION_TAG, offset, offset).firstOrNull()?.let { onLinkClick(it.item) }
        },
    )
}

internal fun inlineAnnotatedString(base: SpanStyle, nodes: List<HtmlNode>, style: HtmlStyleConfig): AnnotatedString =
    buildAnnotatedString {
        withStyle(base) {
            HtmlInlineRenderer.render(this, nodes, style)
        }
    }
