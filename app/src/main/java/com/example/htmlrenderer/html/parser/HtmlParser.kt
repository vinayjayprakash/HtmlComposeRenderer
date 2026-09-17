package com.example.htmlrenderer.html.parser

import com.example.htmlrenderer.html.model.HtmlNode

/**
 * Parses an HTML fragment string into a neutral [HtmlNode] tree.
 *
 * The rendering layer depends only on this abstraction (Dependency Inversion), so the parsing
 * library backing it - Jsoup today - can be swapped for another implementation without touching
 * any rendering code.
 */
interface HtmlParser {
    fun parse(html: String): List<HtmlNode>
}
