package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NavTabs")
class NavTabsTest {

  @Test
  @DisplayName("nav tabs mark only the current route with aria-current=page")
  void navTabsMarkOnlyCurrentRoute() {
    var page =
        renderMain(
            NavTabs.of(
                    "Sections",
                    List.of(new Link("/", "Home"), new Link("/components", "Components")))
                .id("main-tabs")
                .active("/components"));

    assertThat(page.elements("[data-scope-id=main-tabs] a[aria-current=page]"))
        .extracting(e -> e.attr("href"))
        .containsExactly("/components");
  }
}
