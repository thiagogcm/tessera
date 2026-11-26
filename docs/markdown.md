# Markdown

`tessera-markdown` turns Markdown pages into document content. It parses CommonMark with the GitHub Flavored Markdown extensions through [commonmark-java](https://github.com/commonmark/commonmark-java), then builds Tessera elements from the syntax tree. Headings join the page outline, links are checked by site link validation, and code, alerts, and footnotes use the matching Tessera components.

```xml
<dependency>
    <groupId>dev.nthings</groupId>
    <artifactId>tessera-markdown</artifactId>
    <version>${tessera.version}</version>
</dependency>
```

On the module path, require `dev.nthings.tessera.markdown`. It requires `dev.nthings.tessera.compiler` transitively.

## Parse a page

```java
var markdown = Markdown.create();
var page = markdown.parse(Files.readString(Path.of("content/guide.md")));

var title = page.frontMatter().value("title").orElseThrow();
var document =
    PageDocument.of(title, DocLayout.of(page.body()))
        .withDescription(page.frontMatter().value("description").orElseThrow());
var route = SiteRoute.of("/guide", document);
```

`parse` never fails. It returns a `MarkdownPage` with the front matter and the body as `DocumentContent`. The body holds document content, so place it in a document container such as `DocLayout`, a `DocumentElement`, or the page body itself. Portable containers such as `Prose` do not accept it.

## Read front matter

Front matter is YAML between `---` lines at the top of the file.

| Call | Returns |
| --- | --- |
| `frontMatter().value("title")` | The first value of a key, or empty |
| `frontMatter().list("tags")` | Every value of a key, such as a YAML list |
| `frontMatter().values()` | All keys with their values as strings |
| `frontMatter().yaml()` | The raw YAML, for a YAML library when you need nested maps or typed values |

A page without front matter returns `FrontMatter.NONE`.

## Supported syntax

| Markdown | Output |
| --- | --- |
| Headings | `Heading` with a GitHub-style id (`setup-mvn`, then `setup-mvn-1`), added to the page outline |
| Paragraphs, emphasis, strong, inline code, line breaks | Native elements |
| `~~strikethrough~~` | `<del>` |
| Links, autolinks (`https://`, `www.`, email) | `<a href>`, checked by site link validation |
| Images, with optional `{width=640 height=480}` | `Image` with dimensions, or `unknownDimensions()` without them |
| Bullet and ordered lists, task lists `- [x]` | Lists; tight items drop their `<p>`; tasks are disabled, labelled checkboxes |
| Tables with `:---:` alignment | Native `<table>` with `th scope="col"` and aligned cells |
| Fenced and indented code | `CodeBlock` with the fence's language |
| `> [!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, `[!CAUTION]` | `Callout`; text after the marker becomes the title; alerts may nest |
| `[^label]` footnotes and inline `^[notes]` | `Footnote` references and the page's footnotes list |
| Raw HTML blocks and inline tags | Emitted as written with `Documents.rawHtml` |
| Blockquotes, thematic breaks | `<blockquote>`, `<hr>` |

## Use raw HTML

Raw HTML passes through unchanged, which suits tables with merged cells or custom classes:

```markdown
<table class="matrix">
  <tr><th colspan="2">Engine</th></tr>
  <tr><td>Chromium</td><td>Firefox</td></tr>
</table>

Press <kbd>Ctrl</kbd>+<kbd>K</kbd> to search.
```

Raw HTML bypasses escaping and validation, so parse only Markdown you trust. Headings, alerts, and footnotes hold portable content: raw HTML, a footnote reference, or fence-renderer output inside one reports `markdown-unplaceable-content` with its line number when the page compiles.

## Render fences with components

`withFence(language, renderer)` replaces the `CodeBlock` for one fence language. The renderer receives a `Fence` with the language, the whole info string, and the code:

```java
var markdown =
    Markdown.create()
        .withFence("mermaid", fence -> DocumentContent.of(
            Elements.custom("mermaid-diagram").text(fence.code())));
```

Languages match case-insensitively on the first word of the info string. Register the element's script with a document component as described in [Documents](documents.md#contribute-a-page-script).

## Rewrite links

Markdown often links to other `.md` files. `withUrlResolver` rewrites every link and image URL before it is placed, so site link validation checks the final URL:

```java
var markdown =
    Markdown.create()
        .withUrlResolver(url -> url.replaceFirst("\\.md(#|$)", "/$1"));
```
