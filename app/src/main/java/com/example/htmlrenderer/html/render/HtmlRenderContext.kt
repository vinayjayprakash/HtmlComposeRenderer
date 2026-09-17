package com.example.htmlrenderer.html.render

import com.example.htmlrenderer.html.style.HtmlStyleConfig

/** Everything a tag handler needs besides the node it is rendering. */
data class HtmlRenderContext(
    val style: HtmlStyleConfig,
    val onLinkClick: (String) -> Unit,
)
