package com.mj.htmlrender.html.parser

import com.mj.htmlrender.html.model.HtmlNode
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
    fun `parses bare top-level text with no wrapping block tag`() {
        val html = "This is a <b>bold</b> word, an <i>italic</i> word, and an <u>underlined</u> word."
        val nodes = parser.parse(html)
        assertEquals(
            listOf("This is a ", "b", " word, an ", "i", " word, and an ", "u", " word."),
            nodes.map { if (it is HtmlNode.Element) it.tag else (it as HtmlNode.Text).value },
        )
    }

    @Test
    fun `parses an unrecognized HTML5 tag as a generic element, not dropped`() {
        val html = "<p>An unsupported tag like <mark>this highlighted phrase</mark> still renders its text.</p>"
        val paragraph = parser.parse(html).single() as HtmlNode.Element
        val mark = paragraph.children.filterIsInstance<HtmlNode.Element>().single()
        assertEquals("mark", mark.tag)
        assertEquals("this highlighted phrase", (mark.children.single() as HtmlNode.Text).value)
    }

    @Test
    fun `parses an unclosed void br tag the same as a self-closed one`() {
        val html = "<p>line one<br>line two</p>"
        val selfClosed = parser.parse("<p>line one<br/>line two</p>")
        val unclosed = parser.parse(html)
        assertEquals(selfClosed, unclosed)
    }

    @Test
    fun `parses strikethrough and its aliases`() {
        val html = "<p><s>a</s> <strike>b</strike> <del>c</del></p>"
        val paragraph = parser.parse(html).single() as HtmlNode.Element
        val tags = paragraph.children.filterIsInstance<HtmlNode.Element>().map { it.tag }
        assertEquals(listOf("s", "strike", "del"), tags)
    }

    @Test
    fun `parses subscript and superscript`() {
        val html = "<p>H<sub>2</sub>O and x<sup>2</sup></p>"
        val paragraph = parser.parse(html).single() as HtmlNode.Element
        val elements = paragraph.children.filterIsInstance<HtmlNode.Element>()
        assertEquals(listOf("sub", "sup"), elements.map { it.tag })
        assertEquals("2", (elements[0].children.single() as HtmlNode.Text).value)
        assertEquals("2", (elements[1].children.single() as HtmlNode.Text).value)
    }

    @Test
    fun `parses an unclosed hr the same as a self-closed one`() {
        val selfClosed = parser.parse("<p>above</p><hr/><p>below</p>")
        val unclosed = parser.parse("<p>above</p><hr><p>below</p>")
        assertEquals(selfClosed, unclosed)
        val hr = selfClosed[1] as HtmlNode.Element
        assertEquals("hr", hr.tag)
        assertEquals(emptyList<HtmlNode>(), hr.children)
    }

    @Test
    fun `parses the full supported-tag sample into the expected structure`() {
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

        val top = parser.parse(html).map { it.normalized() }.filterIsInstance<HtmlNode.Element>()
        assertEquals(listOf("h1", "p", "p", "h2", "ul", "h3", "ol"), top.map { it.tag })

        assertEquals("Welcome", (top[0].children.single() as HtmlNode.Text).value)

        assertEquals(
            listOf("This is a ", "b", " word, an ", "i", " word, and an ", "u", " word."),
            top[1].children.map { if (it is HtmlNode.Element) it.tag else (it as HtmlNode.Text).value },
        )

        val link = top[2].children.filterIsInstance<HtmlNode.Element>().single { it.tag == "a" }
        assertEquals("https://www.anthropic.com", link.attributes["href"])
        assertEquals(1, top[2].children.filterIsInstance<HtmlNode.Element>().count { it.tag == "br" })

        val unorderedItems = top[4].children.filterIsInstance<HtmlNode.Element>()
        assertEquals(3, unorderedItems.size)
        val nestedList = unorderedItems[2].children.filterIsInstance<HtmlNode.Element>().single { it.tag == "ul" }
        assertEquals(2, nestedList.children.filterIsInstance<HtmlNode.Element>().size)

        val orderedItems = top[6].children.filterIsInstance<HtmlNode.Element>()
        assertEquals(2, orderedItems.size)
    }

    /**
     * Drops whitespace-only text nodes recursively - TagSoup preserves the raw newline/indentation
     * between block tags as its own text node, but [HtmlBlockRenderer][com.mj.htmlrender.html.render.HtmlBlockRenderer]
     * ignores blank text nodes, so this reflects the tree the renderer actually sees.
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
