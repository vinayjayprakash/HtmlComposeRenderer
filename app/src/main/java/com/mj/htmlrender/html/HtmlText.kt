package com.mj.htmlrender.html

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mj.htmlrender.html.parser.HtmlParser
import com.mj.htmlrender.html.parser.TagSoupHtmlParser
import com.mj.htmlrender.html.render.HtmlBlockRenderer
import com.mj.htmlrender.html.render.HtmlRenderContext
import com.mj.htmlrender.html.style.HtmlStyleConfig

private val defaultHtmlParser: HtmlParser = TagSoupHtmlParser()

/**
 * Renders a small HTML fragment (i, em, b, strong, u, s, strike, del, sub, sup, a, br, hr, p, ul,
 * ol, li, h1-h4) as native Compose content. Unsupported tags fall back to rendering their text
 * content inline.
 *
 * This composable only wires three independent layers together:
 * - [parser] turns the HTML string into a neutral [com.mj.htmlrender.html.model.HtmlNode]
 *   tree ([TagSoupHtmlParser] by default - swap it for any other [HtmlParser] implementation).
 * - [style] configures how each tag looks.
 * - [com.mj.htmlrender.html.render.HtmlBlockRenderer] walks the tree and draws it,
 *   dispatching each tag to a registered handler (see `BlockTagHandlers.kt` / `InlineTagHandlers.kt`).
 */
@Composable
fun HtmlText(
    htmlString: String,
    modifier: Modifier = Modifier,
    style: HtmlStyleConfig = HtmlStyleConfig(),
    onLinkClick: (String) -> Unit = {},
    parser: HtmlParser = defaultHtmlParser,
) {
    val nodes = remember(htmlString, parser) { parser.parse(htmlString) }
    Column(modifier = modifier) {
        HtmlBlockRenderer.RenderAll(nodes, HtmlRenderContext(style, onLinkClick))
    }
}
