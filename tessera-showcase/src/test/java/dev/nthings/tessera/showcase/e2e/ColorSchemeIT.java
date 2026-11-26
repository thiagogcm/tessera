package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ColorScheme;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Color scheme")
class ColorSchemeIT {

  private static final Pattern TOGGLE = Pattern.compile("^Color scheme");

  @BrowserTest
  @DisplayName("the toggle cycles system, light and dark, and the choice survives navigation")
  void cyclesAndPersists(Session session) {
    var page = session.page();
    page.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(ColorScheme.LIGHT));
    session.open("/");
    var toggle = toggle(page);
    assertThat(toggle).hasAccessibleName("Color scheme: system");
    assertThat(scheme(page)).isEqualTo("light");

    toggle.click();
    assertThat(toggle).hasAccessibleName("Color scheme: light");
    toggle.click();
    assertThat(toggle).hasAccessibleName("Color scheme: dark");
    assertThat(page.locator("html")).hasAttribute("data-color-scheme", "dark");
    assertThat(scheme(page)).isEqualTo("dark");

    session.open("/forms/");
    assertThat(page.locator("html")).hasAttribute("data-color-scheme", "dark");
    assertThat(scheme(page)).isEqualTo("dark");
    assertThat(toggle(page)).hasAccessibleName("Color scheme: dark");

    toggle(page).click();
    assertThat(toggle(page)).hasAccessibleName("Color scheme: system");
    assertThat(page.locator("html")).not().hasAttribute("data-color-scheme", "dark");
    assertThat(scheme(page)).isEqualTo("light");
  }

  private static Locator toggle(Page page) {
    return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(TOGGLE));
  }

  /// The scheme the root's `color-scheme` resolves `light-dark()` to.
  private static String scheme(Page page) {
    return (String)
        page.evaluate(
            "() => { const probe = document.createElement('i');"
                + " probe.style.color = 'light-dark(rgb(0, 0, 1), rgb(0, 0, 2))';"
                + " document.body.append(probe);"
                + " const color = getComputedStyle(probe).color; probe.remove();"
                + " return color === 'rgb(0, 0, 2)' ? 'dark' : 'light'; }");
  }
}
