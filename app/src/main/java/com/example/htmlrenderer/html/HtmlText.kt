package com.example.htmlrenderer.html

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.htmlrenderer.html.parser.HtmlParser
import com.example.htmlrenderer.html.parser.JsoupHtmlParser
import com.example.htmlrenderer.html.parser.TagSoupHtmlParser
import com.example.htmlrenderer.html.render.HtmlBlockRenderer
import com.example.htmlrenderer.html.render.HtmlRenderContext
import com.example.htmlrenderer.html.style.HtmlStyleConfig

private val defaultHtmlParser: HtmlParser = TagSoupHtmlParser()

/**
 * Renders a small HTML fragment (i, em, b, strong, u, a, br, p, ul, ol, li, h1-h4) as native
 * Compose content. Unsupported tags fall back to rendering their text content inline.
 *
 * This composable only wires three independent layers together:
 * - [parser] turns the HTML string into a neutral [com.example.htmlrenderer.html.model.HtmlNode]
 *   tree ([JsoupHtmlParser] by default - swap it for any other [HtmlParser] implementation).
 * - [style] configures how each tag looks.
 * - [com.example.htmlrenderer.html.render.HtmlBlockRenderer] walks the tree and draws it,
 *   dispatching each tag to a registered handler (see `BlockTagHandlers.kt` / `InlineTagHandlers.kt`).
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: HtmlStyleConfig = HtmlStyleConfig(),
    onLinkClick: (String) -> Unit = {},
    parser: HtmlParser = defaultHtmlParser,
) {
    val nodes = remember(html, parser) { parser.parse(html) }
    Column(modifier = modifier) {
        HtmlBlockRenderer.RenderAll(nodes, HtmlRenderContext(style, onLinkClick))
    }
}
