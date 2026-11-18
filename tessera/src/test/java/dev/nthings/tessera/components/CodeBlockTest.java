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
}
