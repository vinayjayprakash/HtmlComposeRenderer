package com.example.htmlrenderer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.htmlrenderer.html.HtmlText
import com.example.htmlrenderer.html.style.HtmlStyleConfig
import com.example.htmlrenderer.html.style.ListMarker
import com.example.htmlrenderer.html.style.ListStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DemoScreen()
                }
            }
        }
    }
}

// Covers every scenario the renderer needs to handle: bare top-level text with no wrapping tag
// (both leading and trailing), all four heading levels, every inline tag (b/strong, i/em, u,
// s/strike/del, sub, sup, a, br - both <br/> and unclosed <br>), an <hr> rule, an unsupported tag
// falling back to plain text, and unordered/ordered lists with nested lists and inline formatting
// inside list items.
private val sampleHtml = """
    This is a <b>bold</b> word, an <i>italic</i> word, and an <u>underlined</u> word, with no
    wrapping tag at all - just like real-world content that starts directly with text.

    <h1>Welcome</h1>
    <p>This is a <b>bold</b> word, an <i>italic</i> word, and an <u>underlined</u> word.</p>
    <p><strong>strong</strong> and <em>em</em> are styled the same as <b>b</b> and <i>i</i>.</p>
    <p>Strikethrough works via <s>s</s>, <strike>strike</strike>, or <del>del</del> - and
    formulas can use <sub>subscript</sub> and <sup>superscript</sup>, e.g. H<sub>2</sub>O or
    x<sup>2</sup>.</p>
    <p>Here is a <a href="https://www.anthropic.com">link to Anthropic</a>.<br/>This line follows
    a self-closed br tag.<br>This line follows an unclosed br tag.</p>
    <p>An unsupported tag like <mark>this highlighted phrase</mark> still renders its text, just
    without special styling, instead of failing.</p>
    <hr/>
    <h2>Unordered list</h2>
    <ul>
        <li>First item</li>
        <li>Second item with <strong>strong</strong> text and a <a href="https://developer.android.com">link</a></li>
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
    <hr>
    <h4>Loose text after a list</h4>
    Bare text can also follow other block content, mixing <b>bold</b> and <i>italic</i> words
    without a wrapping paragraph.
""".trimIndent()

// Demonstrates per-tag custom styling: bold gets a bigger red font, bullets get an orange marker,
// ordered-list numbers get a purple marker.
private val customHtmlStyle = HtmlStyleConfig(
    bold = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFD32F2F)),
    unorderedList = ListStyle(
        marker = ListMarker.Bullet(symbol = "●", color = Color(0xFFF57C00)),
    ),
    orderedList = ListStyle(
        marker = ListMarker.Number(color = Color(0xFF6A1B9A)),
    ),
)

@Composable
private fun DemoScreen() {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        HtmlText(
            html = sampleHtml,
            style = customHtmlStyle,
            onLinkClick = { url -> uriHandler.openUri(url) },
        )
    }
}
