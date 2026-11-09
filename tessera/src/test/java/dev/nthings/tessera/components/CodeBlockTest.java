package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CodeBlock")
class CodeBlockTest {

  private static RenderedPage diff;

  @BeforeAll
  static void renderDiffExample() {
    diff =
        renderMain(
            CodeBlock.ofLines(
                    "public class Tessera {",
                    "  public static void main(String[] args) {",
                    "+   System.out.println(\"Hello 2026\");",
                    "-   System.out.println(\"Hello legacy\");",
                    "  }",
                    "}")
                .filename("Tessera.java")
                .language("java")
                .diffAutoDetect()
                .highlightLines(2, 3));
  }

  @Test
  @DisplayName("scrollable code is keyboard focusable")
  void codeIsKeyboardFocusable() {
    assertThat(diff.element("pre.code-pre")).hasAttribute("tabindex", "0");
  }

  @Test
  @DisplayName("renders one span per source line")
  void rendersOneSpanPerSourceLine() {
    assertThat(diff.elements("span.code-line")).hasSize(6);
  }

  @Test
  @DisplayName("marks highlighted lines")
  void marksHighlightedLines() {
    var lines = diff.elements("span.code-line");

    assertThat(lines.get(1)).hasClasses("is-highlighted");
  }

  @Test
  @DisplayName("diff markers win over highlighting")
  void diffMarkersWinOverHighlighting() {
    var lines = diff.elements("span.code-line");

    assertThat(lines.get(2)).hasClasses("is-add").doesNotHaveClasses("is-highlighted");
    assertThat(lines.get(3)).hasClasses("is-del");
  }

  @Test
  @DisplayName("header shows the filename and the language as a pill badge")
  void headerShowsFilenameAndLanguagePill() {
    var page =
        renderMain(
            CodeBlock.ofLines("const greeting = 'hello';")
                .filename("index.js")
                .language("javascript"));

    assertThat(page.element(".code-header .code-filename")).hasText("index.js");
    assertThat(page.element(".code-header .badge.is-pill")).hasText("javascript");
  }
}
