package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatCode;

import dev.nthings.tessera.site.SiteCompiler;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ShowcaseSite")
class ShowcaseSiteTest {

  @Test
  @DisplayName("compiles: every link, anchor, a11y and transition check passes")
  void compilesWithEveryCheckPassing() {
    assertThatCode(() -> SiteCompiler.compile(ShowcaseSite.createDefinition()))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("every page links the shared stylesheet")
  void everyPageLinksSharedStylesheet() {
    var site = SiteCompiler.compile(ShowcaseSite.createDefinition());

    assertThat(site.pagesByRoute().values())
        .allSatisfy(
            page ->
                assertThat(RenderedPage.of(page).element("head > link[rel=stylesheet]"))
                    .hasAttribute("href", site.sharedCssPath()));
  }
}
