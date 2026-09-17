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

private val sampleHtml = """
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
