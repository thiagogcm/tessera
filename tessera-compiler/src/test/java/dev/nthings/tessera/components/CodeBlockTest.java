package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.List;
import java.util.stream.Stream;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("CodeBlock")
class CodeBlockTest {

  static Stream<Arguments> sources() {
    return Stream.of(
        argumentSet("LF", CodeBlock.of("a\nb"), List.of("a", "b")),
        argumentSet("CRLF and lone CR", CodeBlock.of("a\r\nb\rc"), List.of("a", "b", "c")),
        argumentSet("trailing newline", CodeBlock.of("a\n"), List.of("a", "")),
        argumentSet("blank line", CodeBlock.of("a\n\nb"), List.of("a", "", "b")),
        argumentSet("embedded newline", CodeBlock.ofLines("a\nb", "c"), List.of("a\nb", "c")));
  }

  @ParameterizedTest
  @MethodSource("sources")
  @DisplayName("renders one line per source line")
  void splitsSourceIntoLines(CodeBlock block, List<String> lines) {
    var code = renderMain(block).element("pre > code");

    assertThat(code.children()).extracting(Element::text).containsExactlyElementsOf(lines);
  }

  @Test
  @DisplayName("scrollable code is keyboard focusable")
  void codeIsKeyboardFocusable() {
    assertThat(renderMain(CodeBlock.of("x")).element("pre")).hasAttribute("tabindex", "0");
  }

  @Test
  @DisplayName("the header shows the filename and the language above the code")
  void headerShowsFilenameAndLanguage() {
    var page =
        renderMain(
            CodeBlock.ofLines("const greeting = 'hello';")
                .filename("index.js")
                .language("javascript"));

    assertThat(page.element("main > *").text())
        .containsSubsequence("index.js", "javascript", "const greeting = 'hello';");
  }

  @Test
  @DisplayName("source markup is shown as text, never parsed")
  void escapesSourceMarkup() {
    var page = renderMain(CodeBlock.ofLines("<script>alert('x')</script>", "a & b"));

    assertThat(page.element("pre").select("script")).isEmpty();
    assertThat(page.element("pre > code").children())
        .extracting(Element::text)
        .containsExactly("<script>alert('x')</script>", "a & b");
  }

  /// `source` in `language`, each colored span as `kind:text`.
  private static List<String> tokens(String language, String source) {
    return renderMain(CodeBlock.of(source).language(language))
        .elements("pre [class^=tok-]")
        .stream()
        .map(span -> span.className().substring(4) + ":" + span.text())
        .toList();
  }

  static Stream<Arguments> highlighted() {
    return Stream.of(
        argumentSet(
            "Java",
            "java",
            "@Override\npublic String name() { return \"x\" + 1; } // done",
            List.of(
                "meta:@Override",
                "keyword:public",
                "type:String",
                "function:name",
                "keyword:return",
                "string:\"x\"",
                "constant:1",
                "comment:// done")),
        argumentSet(
            "TypeScript",
            "ts",
            "const n: number = `a${b}`; // c",
            List.of("keyword:const", "keyword:number", "string:`a${b}`", "comment:// c")),
        argumentSet(
            "JSON",
            "json",
            "{\"a\": [1, true, \"b\"]}",
            List.of("property:\"a\"", "constant:1", "constant:true", "string:\"b\"")),
        argumentSet(
            "CSS",
            "css",
            "a:hover { color: rgb(0 0 0); margin: 2px !important; }",
            List.of(
                "property:color",
                "function:rgb",
                "constant:0",
                "constant:0",
                "constant:0",
                "property:margin",
                "constant:2px",
                "keyword:!important")),
        argumentSet(
            "HTML",
            "html",
            "<!doctype html><a href=\"/x\" hidden>Go &amp; see</a><!-- c -->",
            List.of(
                "meta:<!doctype html>",
                "tag:<a",
                "property:href",
                "string:\"/x\"",
                "property:hidden",
                "tag:>",
                "constant:&amp;",
                "tag:</a",
                "tag:>",
                "comment:<!-- c -->")),
        argumentSet(
            "shell",
            "bash",
            "if [ -n \"$HOME\" ]; then echo $USER # hi\nfi",
            List.of(
                "keyword:if",
                "string:\"$HOME\"",
                "keyword:then",
                "constant:$USER",
                "comment:# hi",
                "keyword:fi")),
        argumentSet(
            "YAML",
            "yaml",
            "---\ntitle: \"Hi\"\ntags:\n  - a\ndraft: false # no\ncount: 3",
            List.of(
                "meta:---",
                "property:title",
                "string:\"Hi\"",
                "property:tags",
                "property:draft",
                "constant:false",
                "comment:# no",
                "property:count",
                "constant:3")),
        argumentSet(
            "Python",
            "py",
            "@cache\ndef f(x):\n    return None  # n",
            List.of(
                "meta:@cache",
                "keyword:def",
                "function:f",
                "keyword:return",
                "constant:None",
                "comment:# n")),
        argumentSet(
            "SQL",
            "sql",
            "SELECT count(*) FROM t WHERE name = 'o''k'; -- c",
            List.of(
                "keyword:SELECT",
                "function:count",
                "keyword:FROM",
                "keyword:WHERE",
                "string:'o''k'",
                "comment:-- c")));
  }

  @ParameterizedTest
  @MethodSource("highlighted")
  @DisplayName("a known language colors the source's syntax")
  void highlightsSyntax(String language, String source, List<String> expected) {
    assertThat(tokens(language, source)).containsExactlyElementsOf(expected);
  }

  @Test
  @DisplayName("a block comment that spans lines is colored on each line, and the text is kept")
  void spansCrossLines() {
    var code =
        renderMain(CodeBlock.of("/* a\n\n b */ int x;").language("Java")).element("pre > code");

    assertThat(code.children())
        .extracting(Element::wholeText)
        .containsExactly("/* a", " ", " b */ int x;");
    assertThat(code.select(".tok-comment"))
        .extracting(Element::wholeText)
        .containsExactly("/* a", " b */");
    assertThat(code.select(".tok-keyword")).extracting(Element::text).containsExactly("int");
  }

  @Test
  @DisplayName("an unknown language or no language leaves the source plain")
  void unknownLanguageStaysPlain() {
    assertThat(tokens("brainfuck", "+[>+<-]")).isEmpty();
    assertThat(renderMain(CodeBlock.of("int x;")).elements("pre [class^=tok-]")).isEmpty();
  }

  @Test
  @DisplayName("changed lines keep their diff color over the syntax colors")
  void changedLinesInheritColor() {
    var page = renderMain(CodeBlock.of("+int x;").language("java").diffAutoDetect());

    assertThat(page.css()).contains(".code-line:is(.is-add, .is-del) [class^=\"tok-\"]");
  }
}
