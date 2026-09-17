package com.example.htmlrenderer.html.parser

import com.example.htmlrenderer.html.model.HtmlNode
import java.io.StringReader
import org.ccil.cowan.tagsoup.Parser as TagSoupSaxParser
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

/**
 * [HtmlParser] backed by TagSoup, a SAX-based HTML parser. This is the only file in the module
 * allowed to import `org.ccil.cowan.tagsoup.*` / `org.xml.sax.*` - everything else works with the
 * neutral [HtmlNode] model.
 *
 * Unlike Jsoup, TagSoup only exposes a streaming SAX [org.xml.sax.ContentHandler] API rather than
 * a DOM - it never hands back a tree to walk. [TreeBuildingHandler] reconstructs one from the SAX
 * callbacks using a stack of in-progress elements. TagSoup also always normalizes its input into
 * a full document (`<html><head/><body>...</body></html>`), even for a bare fragment, so this
 * parser returns only `<body>`'s children to match [JsoupHtmlParser]'s fragment behavior.
 */
class TagSoupHtmlParser : HtmlParser {
    override fun parse(html: String): List<HtmlNode> {
        val handler = TreeBuildingHandler()
        val reader = TagSoupSaxParser()
        // TagSoup's schema otherwise injects default attribute values that were never in the
        // source markup (e.g. shape="rect" on <a>, clear="none" on <br>) - suppress that so the
        // parsed tree reflects only what was actually authored, matching JsoupHtmlParser.
        reader.setFeature("http://www.ccil.org/~cowan/tagsoup/features/default-attributes", false)
        reader.contentHandler = handler
        reader.parse(InputSource(StringReader(html)))
        return handler.bodyChildren()
    }
}

/** Matches HTML's normal whitespace-collapsing rendering rule: a run of whitespace (including a
 * newline from source formatting) becomes a single space. Jsoup applies the same normalization
 * internally via `TextNode.text()`; TagSoup's raw SAX `characters()` callback does not, so it is
 * done here instead. */
private val whitespaceRun = Regex("\\s+")

private class TreeBuildingHandler : DefaultHandler() {
    private class Frame(val tag: String, val attributes: Map<String, String>) {
        val children = mutableListOf<HtmlNode>()
    }

    private val stack = ArrayDeque<Frame>()
    private val text = StringBuilder()
    private var body: HtmlNode.Element? = null

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        flushText()
        val tag = localName.ifEmpty { qName }.lowercase()
        val attrs = (0 until attributes.length).associate { i ->
            val name = attributes.getLocalName(i).ifEmpty { attributes.getQName(i) }
            name to attributes.getValue(i)
        }
        stack.addLast(Frame(tag, attrs))
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        flushText()
        val frame = stack.removeLast()
        val element = HtmlNode.Element(frame.tag, frame.attributes, frame.children)
        if (frame.tag == "body") body = element
        val parent = stack.lastOrNull()
        if (parent != null) parent.children.add(element)
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        text.append(ch, start, length)
    }

    private fun flushText() {
        if (text.isEmpty()) return
        val value = whitespaceRun.replace(text, " ")
        text.clear()
        stack.lastOrNull()?.children?.add(HtmlNode.Text(value))
    }

    fun bodyChildren(): List<HtmlNode> = body?.children ?: emptyList()
}
