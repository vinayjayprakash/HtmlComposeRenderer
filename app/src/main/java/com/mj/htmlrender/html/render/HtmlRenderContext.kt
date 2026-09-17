package com.mj.htmlrender.html.render

import com.mj.htmlrender.html.style.HtmlStyleConfig

/** Everything a tag handler needs besides the node it is rendering. */
data class HtmlRenderContext(
    val style: HtmlStyleConfig,
    val onLinkClick: (String) -> Unit,
)
