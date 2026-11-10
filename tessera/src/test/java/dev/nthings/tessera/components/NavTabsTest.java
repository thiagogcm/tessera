package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NavTabs")
class NavTabsTest {

  @Test
  @DisplayName("marks only the active route with aria-current=page and an underline anchor")
  void marksOnlyCurrentRoute() {
    var page =
        renderMain(
            NavTabs.of(
                    "Sections",
                    List.of(new Link("/", "Home"), new Link("/components", "Components")))
                .id("main-tabs")
                .active("/components"));

    var nav = page.element("[data-scope-id=main-tabs]");
    assertThat(nav).hasTagName("nav").hasAttribute("aria-label", "Sections");
    assertThat(nav.select("a"))
        .extracting(a -> a.attr("href") + ":" + a.text())
        .containsExactly("/:Home", "/components:Components");
    assertThat(nav.select("a[aria-current=page]")).hasSize(1);
    assertThat(nav.selectFirst("a[aria-current=page]")).hasAttribute("href", "/components");
    assertThat(nav.select("li.active > a")).hasSize(1);
    assertThat(nav.selectFirst("li.active > a"))
        .hasAttribute("href", "/components")
        .hasAttribute("aria-current", "page");
    assertThat(page.scopeBody(nav))
        .contains("anchor-name:--tessera-active-nav")
        .contains("position-anchor:--tessera-active-nav");
  }
}
