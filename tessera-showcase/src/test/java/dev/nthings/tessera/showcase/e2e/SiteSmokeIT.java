package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import dev.nthings.tessera.showcase.ShowcaseSite;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Every published page")
class SiteSmokeIT {

  /// WCAG 1.4.12 text spacing: the largest spacing a reader's style sheet may apply.
  private static final String WIDE_TEXT_SPACING =
      """
      * {
        line-height: 1.5 !important;
        letter-spacing: 0.12em !important;
        word-spacing: 0.16em !important;
      }
      p { margin-block-end: 2em !important; }
      """;

  /// A route seen at one window width, optionally with wide text spacing.
  record View(String route, int width, boolean wideTextSpacing) {
    @Override
    public String toString() {
      return route + " at " + width + "px" + (wideTextSpacing ? " with wide text spacing" : "");
    }
  }

  /// The documentation routes, in navigation order.
  static List<String> routes() {
    return ShowcaseSite.createDefinition().routes().stream()
        .map(route -> route.path())
        .filter(path -> !path.startsWith("/stories/"))
        .toList();
  }

  static Stream<View> views() {
    return routes().stream()
        .flatMap(route ->
            Stream.of(
                new View(route, Session.PHONE_WIDTH, false),
                new View(route, Session.DESKTOP_WIDTH, false),
                // 320px is a 1280px window at 400% zoom (WCAG 1.4.10 reflow).
                new View(route, 320, true)));
  }

  @BrowserTest(cases = "views")
  @DisplayName("loads completely, shows its heading and images, and fits the window")
  void loadsCompletelyAndFitsTheWindow(Session session, View view) {
    var page = session.width(view.width()).open(view.route());
    if (view.wideTextSpacing()) {
      page.addStyleTag(new Page.AddStyleTagOptions().setContent(WIDE_TEXT_SPACING));
    }

    assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1)))
        .isVisible();
    for (var image : page.locator("img").all()) {
      image.scrollIntoViewIfNeeded();
      assertThat(image).hasJSProperty("complete", true);
      assertThat(Session.number(image.evaluate("el => el.naturalWidth")))
          .as("decoded width of %s", image.getAttribute("src"))
          .isPositive();
    }
    assertThat(session.fitsViewport()).as("%s fits the window", view).isTrue();
  }
}
