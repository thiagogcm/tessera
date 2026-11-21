package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.blockquote;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.support.CssBlocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("layout components")
class LayoutComponentsTest {

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  @DisplayName("a grid rejects a non-positive column count")
  void gridRejectsNonPositiveColumnCount(int columns) {
    assertThatIllegalArgumentException().isThrownBy(() -> Grid.columns(columns));
  }

  @Test
  @DisplayName("the scroll progress bar is hidden from assistive technology")
  void scrollProgressBarIsHidden() {
    assertThat(renderMain(ScrollProgressBar.of()).element("main > *"))
        .hasAttribute("aria-hidden", "true");
  }

  @Test
  @DisplayName("blockquote factories render native blockquote elements styled by Prose")
  void blockquoteRendersAndMatchesProseStyling() {
    var page =
        renderMain(
            Prose.of(
                blockquote().child(p("Empty-factory quote")),
                blockquote("Plain text <quote>"),
                section().child(blockquote(p("First paragraph"), p("Second paragraph")))));

    assertThat(page.elements("blockquote").eachText())
        .containsExactly(
            "Empty-factory quote", "Plain text <quote>", "First paragraph Second paragraph");
    assertThat(page.elements("section > blockquote > p").eachText())
        .containsExactly("First paragraph", "Second paragraph");
    assertThat(CssBlocks.block(page.css(), ":scope > blockquote, :scope section blockquote{"))
        .contains("border-inline-start:3px solid var(--accent-primary)");
  }
}
