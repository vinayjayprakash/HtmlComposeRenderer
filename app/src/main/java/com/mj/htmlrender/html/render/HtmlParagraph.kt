package com.mj.htmlrender.html.render

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.mj.htmlrender.html.model.HtmlNode
import com.mj.htmlrender.html.style.HtmlStyleConfig

/** Renders inline-styled text as a single clickable block, resolving taps back to [onLinkClick] via [LINK_ANNOTATION_TAG]. */
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
