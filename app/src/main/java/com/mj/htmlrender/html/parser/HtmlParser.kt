package com.mj.htmlrender.html.parser

import com.mj.htmlrender.html.model.HtmlNode

/**
 * Parses an HTML fragment string into a neutral [HtmlNode] tree.
 *
 * The rendering layer depends only on this abstraction (Dependency Inversion), so the parsing
 * library backing it - TagSoup today - can be swapped for another implementation without touching
 * any rendering code.
 */
interface HtmlParser {
    fun parse(html: String): List<HtmlNode>
}
