package dev.nthings.tessera.markdown;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.Documents;
import java.util.List;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Markdown")
class MarkdownTest {

  private static final Markdown MARKDOWN = Markdown.of();

  private static Document render(Markdown markdown, String source) {
    return render(markdown.parse(source).body());
  }

  private static Document render(String source) {
    return render(MARKDOWN, source);
  }

  private static Document render(DocumentContent body) {
    var page = Compiler.forPage().compile(PageDocument.of("Test", inMain(body)));
    return Jsoup.parse(page.html());
  }

  private static DocumentContent inMain(DocumentContent body) {
    return DocumentElement.of(main()).child(body);
  }

  private static Element first(Document dom, String selector) {
    var found = dom.selectFirst(selector);
    assertThat(found).as(selector).isNotNull();
    return found;
  }

  @Nested
  @DisplayName("front matter")
  class FrontMatterTest {

    @Test
    @DisplayName("reads scalars and lists, and keeps the raw YAML")
    void readsFrontMatter() {
      var page =
          MARKDOWN.parse(
              """
              ---
              title: Hello
              tags:
                - java
                - web
              ---
              Body
              """);

      assertThat(page.frontMatter().value("title")).hasValue("Hello");
      assertThat(page.frontMatter().list("tags")).containsExactly("java", "web");
      assertThat(page.frontMatter().value("missing")).isEmpty();
      assertThat(page.frontMatter().values().keySet()).containsExactly("title", "tags");
      assertThat(page.frontMatter().yaml()).isEqualTo("title: Hello\ntags:\n  - java\n  - web\n");
      assertThat(render(page.body()).select("main > p").text()).isEqualTo("Body");
    }

    @Test
    @DisplayName("is empty without a front matter block")
    void noFrontMatter() {
      assertThat(MARKDOWN.parse("# Title").frontMatter()).isEqualTo(FrontMatter.NONE);
    }
  }

  @Nested
  @DisplayName("headings")
  class Headings {

    @Test
    @DisplayName("get GitHub-style ids, deduplicated, and join the page outline")
    void outlinesHeadings() {
      var body = MARKDOWN.parse("# Hello *world*\n\n## Setup `mvn`\n\n## Hello world\n").body();
      var outline =
          Documents.late(context ->
              DocumentContent.of(
                  p(context.headings().stream()
                          .map(h -> h.level() + ":" + h.title())
                          .collect(Collectors.joining("|")))
                      .attr("class", "outline")));

      var dom = render(Documents.fragment(body, outline));

      assertThat(dom.select("main > h1, main > h2").eachAttr("id"))
          .containsExactly("hello-world", "setup-mvn", "hello-world-1");
      assertThat(first(dom, "h1").html()).isEqualTo("Hello <em>world</em>");
      assertThat(first(dom, ".outline").text())
          .isEqualTo("1:Hello world|2:Setup mvn|2:Hello world");
    }
  }

  @Nested
  @DisplayName("inline content")
  class Inlines {

    @Test
    @DisplayName("maps emphasis, code, strikethrough, breaks and autolinks to elements")
    void mapsInlines() {
      var dom = render("*a* **b** `c` ~~d~~ https://example.com\nnext\\\nbreak");

      var paragraph = first(dom, "main > p");
      assertThat(paragraph.select("em").text()).isEqualTo("a");
      assertThat(paragraph.select("strong").text()).isEqualTo("b");
      assertThat(paragraph.select("code").text()).isEqualTo("c");
      assertThat(paragraph.select("del").text()).isEqualTo("d");
      assertThat(paragraph.select("a").attr("href")).isEqualTo("https://example.com");
      assertThat(paragraph.select("br")).hasSize(1);
    }

    @Test
    @DisplayName("passes link and image URLs through the resolver")
    void resolvesUrls() {
      var markdown = Markdown.of().withUrlResolver(url -> url.replaceFirst("\\.md(#|$)", "/$1"));

      var dom = render(markdown, "[Guide](guide.md#install \"Install\") ![Logo](logo.svg)");

      assertThat(first(dom, "a").attr("href")).isEqualTo("guide/#install");
      assertThat(first(dom, "a").attr("title")).isEqualTo("Install");
      assertThat(first(dom, "img").attr("src")).isEqualTo("logo.svg");
    }

    @Test
    @DisplayName("sizes images from attributes, and otherwise marks dimensions unknown")
    void sizesImages() {
      var dom = render("![Chart](chart.png){width=640 height=480}\n\n![Photo](photo.jpg \"Sea\")");

      var images = dom.select("img");
      assertThat(images.get(0).attr("width")).isEqualTo("640");
      assertThat(images.get(0).attr("height")).isEqualTo("480");
      assertThat(images.get(0).attr("alt")).isEqualTo("Chart");
      assertThat(images.get(1).hasAttr("width")).isFalse();
      assertThat(images.get(1).attr("title")).isEqualTo("Sea");
    }
  }

  @Nested
  @DisplayName("blocks")
  class Blocks {

    @Test
    @DisplayName("renders tight lists without paragraphs and keeps an ordered list's start")
    void rendersLists() {
      var dom = render("- one\n- two\n\n3. three\n4. four\n\n- loose\n\n- items\n");

      assertThat(dom.select("main > ul").first().select("li").eachText())
          .containsExactly("one", "two");
      assertThat(dom.select("main > ul").first().select("p")).isEmpty();
      assertThat(first(dom, "ol").attr("start")).isEqualTo("3");
      assertThat(dom.select("main > ul").get(1).select("li > p")).hasSize(2);
    }

    @Test
    @DisplayName("renders task list items as labelled, disabled checkboxes")
    void rendersTasks() {
      var dom = render("- [ ] todo\n- [x] done\n");

      var boxes = dom.select("li > input[type=checkbox]");
      assertThat(boxes).hasSize(2);
      assertThat(boxes.get(0).hasAttr("checked")).isFalse();
      assertThat(boxes.get(1).hasAttr("checked")).isTrue();
      assertThat(boxes.eachAttr("aria-label")).containsExactly("Not done", "Done");
      assertThat(boxes.get(0).hasAttr("disabled")).isTrue();
    }

    @Test
    @DisplayName("renders tables with header scope and column alignment")
    void rendersTables() {
      var dom = render("| Left | Center | Right |\n| :--- | :---: | ---: |\n| a | **b** | c |\n");

      assertThat(dom.select("thead th").eachText()).containsExactly("Left", "Center", "Right");
      assertThat(dom.select("thead th").eachAttr("scope")).containsOnly("col");
      assertThat(dom.select("tbody td").eachText()).containsExactly("a", "b", "c");
      assertThat(first(dom, "tbody td:nth-child(2) strong").text()).isEqualTo("b");
      var centerClass = first(dom, "thead th:nth-child(2)").className();
      assertThat(centerClass).isNotBlank();
      assertThat(first(dom, "tbody td:nth-child(2)").className()).isEqualTo(centerClass);
    }

    @Test
    @DisplayName("renders blockquotes and thematic breaks")
    void rendersQuotesAndRules() {
      var dom = render("> quoted\n\n---\n");

      assertThat(first(dom, "main > blockquote > p").text()).isEqualTo("quoted");
      assertThat(dom.select("main > hr")).hasSize(1);
    }

    @Test
    @DisplayName("renders alerts as callouts, with custom titles")
    void rendersAlerts() {
      var dom = render("> [!WARNING] Careful\n> Body.\n\n> [!TIP]\n> Hint.\n");

      var callouts = dom.select("aside.callout");
      assertThat(callouts).hasSize(2);
      assertThat(callouts.get(0).hasClass("callout--warning")).isTrue();
      assertThat(callouts.get(0).select(".callout-title").text()).isEqualTo("Careful");
      assertThat(callouts.get(0).select(".callout-content p").text()).isEqualTo("Body.");
      assertThat(callouts.get(1).hasClass("callout--tip")).isTrue();
    }
  }

  @Nested
  @DisplayName("code")
  class CodeTest {

    @Test
    @DisplayName("renders fenced and indented code as code blocks with the language")
    void rendersCodeBlocks() {
      var dom = render("```java\nclass A {}\n```\n\n    plain\n");

      var blocks = dom.select(".code-block-wrapper");
      assertThat(blocks).hasSize(2);
      assertThat(blocks.get(0).select(".badge-label").text()).isEqualTo("java");
      assertThat(blocks.get(0).select(".code-line").eachText()).containsExactly("class A {}");
      assertThat(blocks.get(1).select(".code-line").eachText()).containsExactly("plain");
    }

    @Test
    @DisplayName("hands fences of a registered language to its renderer")
    void rendersRegisteredFences() {
      var markdown =
          Markdown.of()
              .withFence(
                  "Mermaid",
                  fence ->
                      Documents.rawHtml(
                          "<pre class=\"mermaid\" data-info=\""
                              + fence.info()
                              + "\">"
                              + fence.code()
                              + "</pre>"));

      var dom = render(markdown, "```mermaid theme=dark\ngraph TD\n```\n");

      assertThat(first(dom, "pre.mermaid").text()).isEqualTo("graph TD");
      assertThat(first(dom, "pre.mermaid").attr("data-info")).isEqualTo("mermaid theme=dark");
      assertThat(dom.select(".code-block-wrapper")).isEmpty();
    }

    @Test
    @DisplayName("rejects a blank fence language")
    void rejectsBlankFenceLanguage() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> Markdown.of().withFence(" ", fence -> Documents.fragment()));
    }
  }

  @Nested
  @DisplayName("footnotes")
  class Footnotes {

    @Test
    @DisplayName("number references and list each note once")
    void rendersFootnotes() {
      var dom = render("One[^a], two^[inline note], again[^a].\n\n[^a]: The note.\n");

      assertThat(dom.select("main a[role=doc-noteref]").eachText())
          .containsExactly("[1]", "[2]", "[1]");
      assertThat(dom.select("[role=doc-footnote]")).hasSize(2);
      assertThat(dom.select("[role=doc-footnote] > span").eachText())
          .containsExactly("The note.", "inline note");
    }

    @Test
    @DisplayName("keep an undefined reference as text")
    void keepsUndefinedReferences() {
      assertThat(first(render("See [^missing]."), "main > p").text()).isEqualTo("See [^missing].");
    }
  }

  @Nested
  @DisplayName("raw HTML")
  class RawHtml {

    @Test
    @DisplayName("passes HTML blocks and inline tags through as written")
    void passesHtmlThrough() {
      var dom =
          render(
              """
              <table class="matrix"><tr><td colspan="2">wide</td></tr></table>

              Press <kbd>Ctrl</kbd> and <span class="hint">*go*</span>.
              """);

      assertThat(first(dom, "table.matrix td").attr("colspan")).isEqualTo("2");
      assertThat(first(dom, "main > p kbd").text()).isEqualTo("Ctrl");
      assertThat(first(dom, "main > p span.hint em").text()).isEqualTo("go");
    }

    @Test
    @DisplayName("reports raw HTML inside a heading, which holds portable content only")
    void reportsHtmlInHeadings() {
      var body = MARKDOWN.parse("Intro\n\n# Title <small>beta</small>\n").body();

      var page = PageDocument.of("Test", inMain(body));

      List<Diagnostic> diagnostics =
          assertThatExceptionOfType(CompilationException.class)
              .isThrownBy(() -> Compiler.forPage().compile(page))
              .actual()
              .diagnostics();
      assertThat(diagnostics).extracting(Diagnostic::rule).contains("markdown-unplaceable-content");
      assertThat(diagnostics).extracting(Diagnostic::message).anyMatch(m -> m.contains("line 3"));
    }
  }

  @Nested
  @DisplayName("typography options")
  class TypographyTest {

    @Test
    @DisplayName("permalinks link every heading but the title to itself")
    void addsHeadingPermalinks() {
      var dom = render(MARKDOWN.withHeadingPermalinks(), "# Title\n\n## Install\n\n### Run it\n");

      assertThat(dom.select("h1 a")).isEmpty();
      assertThat(first(dom, "h2 a").attr("href")).isEqualTo("#install");
      assertThat(first(dom, "h2 a").attr("aria-label")).isEqualTo("Link to Install");
      assertThat(first(dom, "h3 a").attr("href")).isEqualTo("#run-it");
    }

    @Test
    @DisplayName("smart punctuation curls quotes and joins dashes and dots outside code")
    void convertsPunctuation() {
      var dom =
          render(
              MARKDOWN.withSmartPunctuation(),
              """
              "Hi," she said -- it's 1990--1995... and *that*'s all --- `"raw" -- ...`

              ('quoted' text)
              """);

      var paragraphs = dom.select("main > p");
      assertThat(paragraphs.get(0).text())
          .isEqualTo(
              "\u201cHi,\u201d she said \u2013 it\u2019s 1990\u20131995\u2026 and"
                  + " that\u2019s all \u2014 \"raw\" -- ...");
      assertThat(paragraphs.get(1).text()).isEqualTo("(\u2018quoted\u2019 text)");
    }

    @Test
    @DisplayName("smart headings keep ids from the plain text")
    void keepsPlainHeadingIds() {
      var dom = render(MARKDOWN.withSmartPunctuation(), "## Don't panic\n");

      assertThat(first(dom, "h2").text()).isEqualTo("Don\u2019t panic");
      assertThat(first(dom, "h2").id()).isEqualTo("dont-panic");
    }
  }
}
