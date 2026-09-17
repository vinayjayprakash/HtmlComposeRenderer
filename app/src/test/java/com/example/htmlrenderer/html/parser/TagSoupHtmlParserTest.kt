package com.example.htmlrenderer.html.parser

import com.example.htmlrenderer.html.model.HtmlNode
import org.junit.Assert.assertEquals
import org.junit.Test

class TagSoupHtmlParserTest {
    private val parser = TagSoupHtmlParser()

    @Test
    fun `parses a plain paragraph`() {
        val nodes = parser.parse("<p>Hello world</p>")
        assertEquals(
            listOf(HtmlNode.Element("p", children = listOf(HtmlNode.Text("Hello world")))),
            nodes,
        )
    }

    @Test
    fun `parses inline formatting tags`() {
        val nodes = parser.parse("<p>This is <b>bold</b> and <i>italic</i>.</p>")
        val paragraph = nodes.single() as HtmlNode.Element
        assertEquals("p", paragraph.tag)
        assertEquals(
            listOf("This is ", "b", " and ", "i", "."),
            paragraph.children.map { if (it is HtmlNode.Element) it.tag else (it as HtmlNode.Text).value },
        )
    }

    @Test
    fun `parses link attributes`() {
        val nodes = parser.parse("""<p><a href="https://example.com">link</a></p>""")
        val paragraph = nodes.single() as HtmlNode.Element
        val link = paragraph.children.single() as HtmlNode.Element
        assertEquals("a", link.tag)
        assertEquals("https://example.com", link.attributes["href"])
        assertEquals("link", (link.children.single() as HtmlNode.Text).value)
    }

    @Test
    fun `parses self-closing br as an empty element`() {
        val nodes = parser.parse("<p>line one<br/>line two</p>")
        val paragraph = nodes.single() as HtmlNode.Element
        val br = paragraph.children[1] as HtmlNode.Element
        assertEquals("br", br.tag)
        assertEquals(emptyList<HtmlNode>(), br.children)
    }

    @Test
    fun `parses nested unordered lists`() {
        val html = """
            <ul>
                <li>First item</li>
                <li>Second item
                    <ul>
                        <li>Nested item</li>
                    </ul>
                </li>
            </ul>
        """.trimIndent()
        val list = parser.parse(html).single() as HtmlNode.Element
        assertEquals("ul", list.tag)
        val items = list.children.filterIsInstance<HtmlNode.Element>()
        assertEquals(2, items.size)
        val nestedList = items[1].children.filterIsInstance<HtmlNode.Element>().single { it.tag == "ul" }
        assertEquals("li", nestedList.children.filterIsInstance<HtmlNode.Element>().single().tag)
    }

    @Test
    fun `matches JsoupHtmlParser output for the full supported-tag sample`() {
        val html = """
            <h1>Welcome</h1>
            <p>This is a <b>bold</b> word, an <i>italic</i> word, and an <u>underlined</u> word.</p>
            <p>Here is a <a href="https://www.anthropic.com">link to Anthropic</a>.<br/>This line follows a br tag.</p>
            <h2>Unordered list</h2>
            <ul>
                <li>First item</li>
                <li>Second item with <strong>strong</strong> text</li>
                <li>Third item
                    <ul>
                        <li>Nested item A</li>
                        <li>Nested item B</li>
                    </ul>
                </li>
            </ul>
            <h3>Ordered list</h3>
            <ol>
                <li>Step one</li>
                <li>Step two</li>
            </ol>
        """.trimIndent()

        assertEquals(
            JsoupHtmlParser().parse(html).map { it.normalized() },
            parser.parse(html).map { it.normalized() },
        )
    }

    /**
     * Drops whitespace-only text nodes recursively. Jsoup and TagSoup place a different amount
     * of insignificant whitespace between block tags (a collapsed single space vs. the raw
     * newline/indentation) - [HtmlBlockRenderer][com.example.htmlrenderer.html.render.HtmlBlockRenderer]
     * already ignores blank text nodes, so this reflects the tree the renderer actually sees.
     */
    private fun HtmlNode.normalized(): HtmlNode = when (this) {
        is HtmlNode.Text -> this
        is HtmlNode.Element -> copy(
            children = children
                .filterNot { it is HtmlNode.Text && it.value.isBlank() }
                .map { it.normalized() },
        )
    }
}
