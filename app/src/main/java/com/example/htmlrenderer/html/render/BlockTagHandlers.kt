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

/** Walks a top-level [HtmlNode] list, dispatching each element to its registered block handler. */
internal object HtmlBlockRenderer {
    @Composable
    fun RenderAll(
        nodes: List<HtmlNode>,
        context: HtmlRenderContext,
        handlers: Map<String, BlockTagHandler> = defaultBlockTagHandlers,
    ) {
        nodes.forEach { node -> RenderNode(node, context, handlers) }
    }

    @Composable
    private fun RenderNode(node: HtmlNode, context: HtmlRenderContext, handlers: Map<String, BlockTagHandler>) {
        when (node) {
            is HtmlNode.Text -> {
                if (node.value.isNotBlank()) {
                    val annotated = inlineAnnotatedString(context.style.paragraph, listOf(node), context.style)
                    HtmlParagraph(annotated, context.onLinkClick)
                }
            }

            is HtmlNode.Element -> {
                val handler = handlers[node.tag]
                if (handler != null) {
                    handler(node, context) { children -> RenderAll(children, context, handlers) }
                } else {
                    // Unknown tag: fall back to rendering its text content as a paragraph.
                    val annotated = inlineAnnotatedString(context.style.paragraph, node.children, context.style)
                    if (annotated.isNotEmpty()) HtmlParagraph(annotated, context.onLinkClick)
                }
            }
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
