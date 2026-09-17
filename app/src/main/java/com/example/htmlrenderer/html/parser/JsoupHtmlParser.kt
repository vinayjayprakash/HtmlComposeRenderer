package com.example.htmlrenderer.html.parser

import com.example.htmlrenderer.html.model.HtmlNode
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * [HtmlParser] backed by Jsoup. This is the only file in the module allowed to import
 * `org.jsoup.*` - everything else works with the neutral [HtmlNode] model.
 */
class JsoupHtmlParser : HtmlParser {
    override fun parse(html: String): List<HtmlNode> {
        val body = Jsoup.parseBodyFragment(html).body()
        return body.childNodes().mapNotNull { it.toHtmlNode() }
    }

    private fun Node.toHtmlNode(): HtmlNode? = when (this) {
        is TextNode -> HtmlNode.Text(text())
        is Element -> HtmlNode.Element(
            tag = tagName().lowercase(),
            attributes = attributes().associate { it.key to it.value },
            children = childNodes().mapNotNull { it.toHtmlNode() },
        )
        // Comments, doctype declarations, etc. carry no renderable content.
        else -> null
    }
}
