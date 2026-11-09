package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.css.CssLength;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Prose")
class ProseTest {

  @Test
  @DisplayName("applies a custom reading measure to its canvas")
  void appliesCustomMeasureToCanvas() {
    var page = renderMain(Prose.of(p().text("Text")).maxMeasure(CssLength.ch(60)));

    assertThat(page.elements(".prose-canvas > p")).isNotEmpty();
    assertThat(page.css()).contains("max-inline-size:60ch");
  }
}
