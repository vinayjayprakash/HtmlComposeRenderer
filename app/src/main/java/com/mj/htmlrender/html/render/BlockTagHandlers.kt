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
 * Strategy for rendering one block-level element tag. [renderChildBlocks] recurses back into
 * [HtmlRenderer] for any nested block content a handler needs to delegate (list handlers
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

private val horizontalRuleTagHandler: BlockTagHandler = { _, context, _ ->
    val hr = context.style.horizontalRule
    HorizontalDivider(color = hr.color, thickness = hr.thickness, modifier = Modifier.padding(vertical = hr.spacing))
}

private val unorderedListTagHandler: BlockTagHandler = { element, context, _ ->
    RenderList(element, context.style.unorderedList, context, depth = 0)
    Spacer(Modifier.height(context.style.blockSpacing))
}

private val orderedListTagHandler: BlockTagHandler = { element, context, _ ->
    RenderList(element, context.style.orderedList, context, depth = 0)
    Spacer(Modifier.height(context.style.blockSpacing))
}

/** Covers `<h1>`-`<h4>`: same rendering, just a different [SpanStyle] picked out of the config.
 * A plain function (not a `val`), so it is safe to reference before its own declaration point. */
private fun headingTagHandler(selectStyle: (HtmlStyleConfig) -> SpanStyle): BlockTagHandler =
    { element, context, _ ->
        HtmlParagraph(inlineAnnotatedString(selectStyle(context.style), element.children, context.style), context.onLinkClick)
        Spacer(Modifier.height(context.style.blockSpacing))
    }

/**
 * Supported block tags. Adding a new one means adding one handler and one entry here -
 * [HtmlRenderer] itself never has to change (Open/Closed).
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
    "hr" to horizontalRuleTagHandler,
)
