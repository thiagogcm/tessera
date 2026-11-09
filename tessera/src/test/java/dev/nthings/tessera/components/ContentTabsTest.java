package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.compiler.CompilationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ContentTabs")
class ContentTabsTest {

  @Test
  @DisplayName("links each tab to its panel and selects the first")
  void linksTabsToPanels() {
    var page =
        renderMain(
            ContentTabs.of("Stages")
                .id("stages")
                .tab("Java", p().text("Builders"))
                .tab("HTML", p().text("Markup")));

    assertThat(page.element("[role=tablist]")).hasAttribute("aria-label", "Stages");
    assertThat(page.elementById("stages--tab-0"))
        .hasAttribute("aria-selected", "true")
        .hasAttribute("aria-controls", "stages--panel-0");
    assertThat(page.elementById("stages--panel-1"))
        .hasAttribute("aria-labelledby", "stages--tab-1")
        .hasAttribute("hidden");
  }

  @Test
  @DisplayName("tabs without any tab are reported")
  void reportsEmptyTabs() {
    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() -> renderMain(ContentTabs.of("Empty")))
        .withMessageContaining("empty-content-tabs");
  }
}
