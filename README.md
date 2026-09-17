# HTML Compose Renderer

A small Android app that renders an HTML-tagged string as native Jetpack Compose UI (no
`WebView`), with per-tag style customization.

## Supported tags

`i`, `em`, `b`, `strong`, `u`, `a`, `br`, `p`, `ul`, `ol`, `li`, `h1`–`h4`.

Unknown tags fall back to rendering their text content inline instead of failing.

## Features

- Pass an HTML string to `HtmlText(html = ...)` and get back real Compose `Text`/`Row`/`Column`
  content — fully themeable, accessible, and measured like any other Compose UI.
- Per-tag styling via `HtmlStyleConfig`: font size/weight/color for bold/italic/underline/links
  and headings, independently of every other tag.
- Custom bullet/number color, symbol, and spacing for `<ul>`/`<ol>` via `ListStyle`/`ListMarker`
  (nested lists are supported and indent automatically).
- `<a>` clicks are exposed via an `onLinkClick(url)` callback.
- The HTML parsing library (Jsoup by default) is swappable without touching any rendering code.

## Project structure

```
app/src/main/java/com/example/htmlrenderer/
├── MainActivity.kt            - demo screen with sample HTML + custom style
└── html/
    ├── HtmlText.kt             - public @Composable entry point, wires the layers together
    ├── model/HtmlNode.kt       - library-agnostic parse tree (Text / Element)
    ├── parser/
    │   ├── HtmlParser.kt       - parsing abstraction
    │   └── JsoupHtmlParser.kt  - Jsoup-backed implementation (only file that imports Jsoup)
    ├── style/HtmlStyleConfig.kt- HtmlStyleConfig, ListStyle, ListMarker
    └── render/
        ├── HtmlRenderContext.kt
        ├── InlineTagHandlers.kt - b/strong/i/em/u/a/br strategies + dispatcher
        ├── BlockTagHandlers.kt  - p/h1-h4/ul/ol/br strategies + dispatcher
        └── ListTagHandlers.kt   - ul/ol/li rendering: markers, nesting
```

## Architecture

The renderer is a layered pipeline (**parse → model → style → render**) with a **Strategy
pattern** for per-tag behavior: each tag has its own small handler, and adding a new tag never
requires touching the dispatch logic (Open/Closed). `HtmlText` depends on the `HtmlParser`
interface rather than Jsoup directly (Dependency Inversion), so the parsing library can be
swapped by passing a different `parser` argument.

### UML class diagram
[README.md](../VideoBlocker/README.md)
```mermaid
classDiagram
    class HtmlNode {
        <<sealed interface>>
    }
    class Text {
        +String value
    }
    class Element {
        +String tag
        +AttributeMap attributes
        +List~HtmlNode~ children
    }
    HtmlNode <|-- Text
    HtmlNode <|-- Element

    class HtmlParser {
        <<interface>>
        +parse(String) List~HtmlNode~
    }
    class JsoupHtmlParser {
        +parse(String) List~HtmlNode~
    }
    HtmlParser <|.. JsoupHtmlParser
    JsoupHtmlParser ..> HtmlNode : builds

    class HtmlStyleConfig {
        +SpanStyle paragraph
        +SpanStyle bold
        +SpanStyle italic
        +SpanStyle underline
        +SpanStyle link
        +SpanStyle h1
        +SpanStyle h2
        +SpanStyle h3
        +SpanStyle h4
        +Dp blockSpacing
        +ListStyle unorderedList
        +ListStyle orderedList
    }
    class ListStyle {
        +ListMarker marker
        +SpanStyle itemTextStyle
        +Dp itemSpacing
        +Dp indentPerLevel
        +Dp markerWidth
    }
    class ListMarker {
        <<sealed class>>
    }
    class Bullet {
        +String symbol
        +Color color
        +TextUnit fontSize
    }
    class Number {
        +Color color
        +TextUnit fontSize
        +format(Int) String
    }
    ListMarker <|-- Bullet
    ListMarker <|-- Number
    HtmlStyleConfig --> ListStyle : unorderedList / orderedList
    ListStyle --> ListMarker

    class HtmlRenderContext {
        +HtmlStyleConfig style
        +onLinkClick(String) Unit
    }
    HtmlRenderContext --> HtmlStyleConfig

    class InlineTagHandler {
        <<typealias, function type>>
    }
    class HtmlInlineRenderer {
        <<object>>
        +render(builder, nodes, style, handlers)
    }
    HtmlInlineRenderer ..> InlineTagHandler : dispatches via tag lookup
    HtmlInlineRenderer ..> HtmlNode

    class BlockTagHandler {
        <<typealias, composable function type>>
    }
    class HtmlBlockRenderer {
        <<object>>
        +RenderAll(nodes, context, handlers)
    }
    HtmlBlockRenderer ..> BlockTagHandler : dispatches via tag lookup
    HtmlBlockRenderer ..> HtmlNode
    HtmlBlockRenderer ..> HtmlRenderContext
    HtmlBlockRenderer ..> HtmlInlineRenderer : renders inline content

    class ListTagHandlers {
        <<file>>
        +RenderList(element, listStyle, context, depth)
    }
    BlockTagHandler ..> ListTagHandlers : ul / ol
    ListTagHandlers ..> ListStyle
    ListTagHandlers ..> HtmlInlineRenderer

    class HtmlText {
        <<composable function>>
        +HtmlText(html, modifier, style, onLinkClick, parser)
    }
    HtmlText --> HtmlParser : parses html
    HtmlText --> HtmlBlockRenderer : renders parsed tree
    HtmlText --> HtmlStyleConfig
```

### Data flow diagram

```mermaid
flowchart TD
    A["HTML string (input)"] --> B["HtmlParser.parse(html)\n(JsoupHtmlParser by default)"]
    B --> C["List&lt;HtmlNode&gt; (neutral parse tree)"]

    STYLE["HtmlStyleConfig / ListStyle / ListMarker\n(caller-supplied)"] -.-> D
    LINK["onLinkClick callback\n(caller-supplied)"] -.-> D

    C --> D["HtmlBlockRenderer.RenderAll"]
    D --> E{"Node type?"}

    E -->|"bare Text"| F["Render as paragraph"]
    E -->|"Element, known tag\n(p / h1-h4 / ul / ol / br)"| G["Look up BlockTagHandler in map"]
    E -->|"Element, unknown tag"| F

    G -->|"p / h1-h4"| H["Build paragraph:\nSpanStyle + inline content"]
    G -->|"ul / ol"| R["RenderList: walk &lt;li&gt; children"]
    G -->|"br"| SP["Emit vertical Spacer"]

    R --> RI["RenderListItem"]
    RI --> RM["ListMarkerText\n(bullet/number colored via ListStyle.marker)"]
    RI --> H
    RI -->|"nested ul/ol"| R

    H --> I["HtmlInlineRenderer.render"]
    F --> I
    I --> J{"Inline node type?"}
    J -->|"Text"| K["Append plain text\nto AnnotatedString.Builder"]
    J -->|"Element, known tag\n(b/strong/i/em/u/a/br)"| L["Look up InlineTagHandler in map"]
    J -->|"Element, unknown tag"| I

    L --> M["Handler wraps children\n with SpanStyle (bold/italic/underline/link)\nor inserts a line break"]
    M --> I

    K --> N["Finished AnnotatedString"]
    RM --> N
    N --> O["ClickableText composable"]
    O --> P["Rendered on screen"]
    O -->|"tap inside a URL span"| LINK
```

## Customizing styles

```kotlin
val customStyle = HtmlStyleConfig(
    bold = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFD32F2F)),
    unorderedList = ListStyle(
        marker = ListMarker.Bullet(symbol = "●", color = Color(0xFFF57C00)), // orange circle
    ),
    orderedList = ListStyle(
        marker = ListMarker.Number(color = Color(0xFF6A1B9A)),
    ),
)

HtmlText(
    html = "<p>Hello <b>world</b></p><ul><li>One</li><li>Two</li></ul>",
    style = customStyle,
    onLinkClick = { url -> /* open it, e.g. via LocalUriHandler */ },
)
```

## Extending: adding a new tag

Adding a tag never requires touching `HtmlInlineRenderer`/`HtmlBlockRenderer`:

1. Write a handler function matching `InlineTagHandler` or `BlockTagHandler`.
2. Add one entry to `defaultInlineTagHandlers` or `defaultBlockTagHandlers`.

## Building

```
JAVA_HOME=<Android Studio's bundled JBR>
ANDROID_HOME=<your Android SDK>
gradle assembleDebug
```

Or simply open the project folder in Android Studio and run — it will generate the Gradle
wrapper jar automatically on sync.
