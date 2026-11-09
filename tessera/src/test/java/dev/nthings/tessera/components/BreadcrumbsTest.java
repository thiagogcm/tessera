package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Breadcrumbs")
class BreadcrumbsTest {

  @Test
  @DisplayName("only the last crumb is marked as the current page")
  void marksOnlyLastCrumbAsCurrent() {
    var page =
        renderMain(Breadcrumbs.of(new Link("/", "Home"), new Link("/components", "Components")));

    assertThat(page.element("nav[aria-label=Breadcrumb] li:first-child a"))
        .doesNotHaveAttribute("aria-current");
    assertThat(page.element("nav[aria-label=Breadcrumb] li:last-child a"))
        .hasAttribute("aria-current");
  }
}
