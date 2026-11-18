package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.options.AriaRole;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;

/// Component styles of mounted roots on the home page, under the full policy.
@DisplayName("Styles")
class StylesIT {
  private static final String PAGE_STYLES =
      """
      () => [document.body, document.querySelector('h1')].map(node => {
        const style = getComputedStyle(node);
        return ['margin-top', 'font-family', 'font-size', 'font-weight', 'color',
                'background-color'].map(name => style.getPropertyValue(name)).join(' ');
      })
      """;

  @BothEngines
  @DisplayName("mounting styles the components without restyling the page, and survives unmounts")
  void rootsStyleOnlyTheirComponents(Session session) {
    var page = session.page();
    page.route("**/*.wasm", route -> route.abort());
    session.expectError("");
    session.open("/");
    assertThat(session.field("Fallback notes")).isVisible();
    var unmounted = page.evaluate(PAGE_STYLES);
    page.unrouteAll();

    session.open("/");
    var status = page.getByRole(AriaRole.STATUS);
    assertThat(status).hasText("Count: 0");
    Assertions.assertThat(page.evaluate(PAGE_STYLES)).isEqualTo(unmounted);
    assertThat(session.field("Name")).hasCSS("border-top-style", "solid");
    assertThat(status).hasCSS("font-weight", "600");
    session.button("Increment").click();
    assertThat(status).hasCSS("font-weight", "400");
    session.button("Show badge").click();
    assertThat(session.text("Saved")).hasCSS("white-space", "nowrap");

    session.button("Unmount").click();
    assertThat(session.field("Name")).hasCount(0);
    assertThat(session.field("Notes")).hasCSS("border-top-style", "solid");
    assertThat(session.button("Second increment")).hasCSS("cursor", "pointer");
    Assertions.assertThat(page.evaluate(PAGE_STYLES)).isEqualTo(unmounted);
  }
}
