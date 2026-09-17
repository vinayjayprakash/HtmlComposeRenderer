package com.mj.htmlrender.html.model

/**
 * A tag-agnostic representation of a parsed HTML fragment. Nothing outside the `parser`
 * package should know which library (TagSoup or otherwise) produced this tree.
 */
sealed interface HtmlNode {
    data class Text(val value: String) : HtmlNode

    data class Element(
        val tag: String,
        val attributes: Map<String, String> = emptyMap(),
        val children: List<HtmlNode> = emptyList(),
    ) : HtmlNode
}
