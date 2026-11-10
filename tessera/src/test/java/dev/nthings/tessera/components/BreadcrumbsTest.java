package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Breadcrumbs")
class BreadcrumbsTest {

  @Test
  @DisplayName("renders crumbs in order and marks only the last as current")
  void marksOnlyLastCrumbAsCurrent() {
    var page =
        renderMain(Breadcrumbs.of(new Link("/", "Home"), new Link("/components", "Components")));

    var crumbs = page.elements("nav[aria-label=Breadcrumb] li > a");
    assertThat(crumbs).extracting(crumb -> crumb.attr("href")).containsExactly("/", "/components");
    assertThat(crumbs).extracting(crumb -> crumb.text()).containsExactly("Home", "Components");
    assertThat(crumbs.get(0)).doesNotHaveAttribute("aria-current");
    assertThat(crumbs.get(1)).hasAttribute("aria-current", "page");
  }

  @Test
  @DisplayName("rejects an empty trail")
  void rejectsEmptyTrail() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Breadcrumbs.of())
        .withMessage("Breadcrumbs requires at least 1 link");
  }
}
