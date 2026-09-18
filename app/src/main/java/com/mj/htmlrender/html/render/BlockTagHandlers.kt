package com.mj.htmlrender.html.render

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import com.mj.htmlrender.html.model.HtmlNode
import com.mj.htmlrender.html.style.HtmlStyleConfig

/**
 * Strategy for rendering one block-level element tag. Implement [Render] in an object (or class)
 * and register it in [defaultBlockTagHandlers]. [renderChildBlocks] recurses back into
 * [HtmlRenderer] for any nested block content a handler needs to delegate (list handlers
 * use this for nested `<ul>`/`<ol>`).
 */
internal interface BlockTagHandler {
    @Composable
    fun Render(
        element: HtmlNode.Element,
        context: HtmlRenderContext,
        renderChildBlocks: @Composable (List<HtmlNode>) -> Unit,
    )
}

private object ParagraphTagHandler : BlockTagHandler {
    @Composable
    override fun Render(element: HtmlNode.Element, context: HtmlRenderContext, renderChildBlocks: @Composable (List<HtmlNode>) -> Unit) {
        HtmlParagraph(inlineAnnotatedString(context.style.paragraph, element.children, context.style), context.onLinkClick)
        Spacer(Modifier.height(context.style.blockSpacing))
    }
}

/** Covers `<h1>`-`<h4>`: same rendering, just a different [SpanStyle] picked out of the config. */
private class HeadingTagHandler(private val selectStyle: (HtmlStyleConfig) -> SpanStyle) : BlockTagHandler {
    @Composable
    override fun Render(element: HtmlNode.Element, context: HtmlRenderContext, renderChildBlocks: @Composable (List<HtmlNode>) -> Unit) {
        HtmlParagraph(inlineAnnotatedString(selectStyle(context.style), element.children, context.style), context.onLinkClick)
        Spacer(Modifier.height(context.style.blockSpacing))
    }
}

private object LineBreakBlockTagHandler : BlockTagHandler {
    @Composable
    override fun Render(element: HtmlNode.Element, context: HtmlRenderContext, renderChildBlocks: @Composable (List<HtmlNode>) -> Unit) {
        Spacer(Modifier.height(context.style.blockSpacing / 2))
    }
}

private object HorizontalRuleTagHandler : BlockTagHandler {
    @Composable
    override fun Render(element: HtmlNode.Element, context: HtmlRenderContext, renderChildBlocks: @Composable (List<HtmlNode>) -> Unit) {
        val hr = context.style.horizontalRule
        HorizontalDivider(color = hr.color, thickness = hr.thickness, modifier = Modifier.padding(vertical = hr.spacing))
    }
}

private object UnorderedListTagHandler : BlockTagHandler {
    @Composable
    override fun Render(element: HtmlNode.Element, context: HtmlRenderContext, renderChildBlocks: @Composable (List<HtmlNode>) -> Unit) {
        RenderList(element, context.style.unorderedList, context, depth = 0)
        Spacer(Modifier.height(context.style.blockSpacing))
    }
}

private object OrderedListTagHandler : BlockTagHandler {
    @Composable
    override fun Render(element: HtmlNode.Element, context: HtmlRenderContext, renderChildBlocks: @Composable (List<HtmlNode>) -> Unit) {
        RenderList(element, context.style.orderedList, context, depth = 0)
        Spacer(Modifier.height(context.style.blockSpacing))
    }
}

/**
 * Supported block tags. Adding a new one means adding one handler and one entry here -
 * [HtmlRenderer] itself never has to change (Open/Closed).
 */
internal val defaultBlockTagHandlers: Map<String, BlockTagHandler> = mapOf(
    "p" to ParagraphTagHandler,
    "h1" to HeadingTagHandler { it.h1 },
    "h2" to HeadingTagHandler { it.h2 },
    "h3" to HeadingTagHandler { it.h3 },
    "h4" to HeadingTagHandler { it.h4 },
    "ul" to UnorderedListTagHandler,
    "ol" to OrderedListTagHandler,
    "br" to LineBreakBlockTagHandler,
    "hr" to HorizontalRuleTagHandler,
)
