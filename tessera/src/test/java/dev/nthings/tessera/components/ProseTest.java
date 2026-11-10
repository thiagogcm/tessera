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
  @DisplayName("applies the custom measure as max-inline-size on the canvas")
  void appliesCustomMeasure() {
    var page = renderMain(Prose.of(p().text("Text")).maxMeasure(CssLength.ch(60)));
    var canvas = page.element(".prose-canvas");

    assertThat(page.elements(".prose-canvas > p")).hasSize(1);
    assertThat(page.element(".prose-canvas > p")).hasText("Text");
    assertThat(page.scopeBody(canvas))
        .contains("max-inline-size:60ch")
        .doesNotContain("max-inline-size:75ch");
  }

  @Test
  @DisplayName("defaults to a 75ch measure")
  void defaultsTo75ch() {
    var page = renderMain(Prose.of(p().text("Text")));

    assertThat(page.scopeBody(page.element(".prose-canvas"))).contains("max-inline-size:75ch");
  }
}
