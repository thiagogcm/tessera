package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.components.Tabs.NavItem;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tabs")
class TabsTest {

  @Test
  @DisplayName("nav tabs mark only the current route with aria-current=page")
  void navTabsMarkOnlyCurrentRoute() {
    var page =
        renderMain(
            Tabs.navTabs(
                "main-tabs",
                "Sections",
                "/components",
                List.of(new NavItem("/", "Home"), new NavItem("/components", "Components"))));

    assertThat(page.elements("[data-view-id=main-tabs] a[aria-current=page]"))
        .extracting(e -> e.attr("href"))
        .containsExactly("/components");
  }
}
