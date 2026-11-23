package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ReducedMotion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Motion")
class MotionIT {

  /// Animations that move or resize something, as `description: target` lines. Color and opacity
  /// fades are not motion; keyframe animations and transitions of position, size, or transform are.
  private static final String MOVING =
      """
      () => {
        const moving = new RegExp('^(transform|translate|rotate|scale|inset|top|left|right|bottom|'
          + 'width|height|inline-size|block-size|margin|padding|background-position)');
        return document.getAnimations()
          .filter((a) => a.playState === 'running')
          .filter((a) => !(a instanceof CSSTransition) || moving.test(a.transitionProperty))
          .map((a) => (a.transitionProperty ?? a.animationName) + ': '
            + (a.effect?.target?.outerHTML.slice(0, 80) ?? '?') + (a.effect?.pseudoElement ?? ''));
      }
      """;

  /// Tiles on the motion page that animate in as they scroll into view.
  private static final List<String> TILES =
      List.of("Fade in", "Fade up", "Scale in", "Slide in", "Item 1", "Item 8");

  @BrowserTest
  @DisplayName("with reduced motion, opening and toggling controls moves nothing")
  void reducedMotionMovesNothing(Session session) {
    var page = session.page();
    page.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.REDUCE));

    session.open("/overlays/");
    session.button("Open modal").click();
    assertStill(page, "opening a modal");
    page.keyboard().press("Escape");
    session.button("Open start drawer").click();
    assertStill(page, "opening a drawer");
    page.keyboard().press("Escape");

    session.open("/navigation/");
    page.locator("summary")
        .filter(new Locator.FilterOptions().setHasText("Can several items be open?"))
        .click();
    assertStill(page, "switching accordion items");
    session.role(AriaRole.TAB, "HTML").click();
    assertStill(page, "switching tabs");
    var track = session.role(AriaRole.REGION, "Platform highlights");
    track.focus();
    page.keyboard().press("ArrowRight");
    assertStill(page, "scrolling a carousel");

    session.open("/forms/");
    session.role(AriaRole.SWITCH, "Weekly digest").click();
    assertStill(page, "flipping a switch");

    session.open("/motion/");
    var spring = session.text("Bouncy spring");
    spring.scrollIntoViewIfNeeded();
    spring.hover();
    assertStill(page, "hovering a spring tile");
    assertThat(translate(spring)).isEqualTo("0px -12px");

    session.open("/docs/");
    page.locator("summary").filter(new Locator.FilterOptions().setHasText("On this page")).click();
    assertStill(page, "collapsing the table of contents");
    page.locator("summary").filter(new Locator.FilterOptions().setHasText("On this page")).click();
    assertStill(page, "expanding the table of contents");
  }

  @BrowserTest
  @DisplayName("with reduced motion, every tile is visible without scrolling to it")
  void reducedMotionShowsTilesAtOnce(Session session) {
    session
        .page()
        .emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.REDUCE));
    session.open("/motion/");

    for (var tile : tiles(session)) {
      assertThat(opacity(tile)).as(tile.textContent()).isEqualTo(1);
    }
  }

  @BrowserTest
  @DisplayName("tiles that animate in end fully visible once scrolled into view")
  void tilesEndVisible(Session session) {
    var page = session.open("/motion/");

    for (var element : tiles(session)) {
      element.evaluate("el => el.scrollIntoView({ block: 'center' })");
      page.waitForFunction(
          "el => parseFloat(getComputedStyle(el).opacity) > 0.99", element.elementHandle());
      assertThat(element).isInViewport();
    }
  }

  @BrowserTest
  @DisplayName("hovering a spring tile moves it along the compiled linear() curve")
  void springTileFollowsItsCurve(Session session) {
    var page = session.open("/motion/");
    var tile = session.text("Snappy spring");
    tile.scrollIntoViewIfNeeded();

    tile.hover();

    var timing =
        (Map<?, ?>)
            tile.evaluate(
                """
                el => {
                  const [transition] = el.getAnimations();
                  const timing = transition.effect.getTiming();
                  return {
                    property: transition.transitionProperty,
                    easing: timing.easing,
                    duration: timing.duration,
                  };
                }
                """);
    assertThat(timing.get("property")).isEqualTo("translate");
    assertThat(String.valueOf(timing.get("easing"))).startsWith("linear(0");
    assertThat(Session.number(timing.get("duration"))).isEqualTo(697);
    page.waitForFunction("el => el.getAnimations().length === 0", tile.elementHandle());
    assertThat(translate(tile)).isEqualTo("0px -12px");
  }

  @BrowserTest
  @DisplayName("the reading progress bar fills from the start of the line")
  void progressBarFillsFromTheStart(Session session) {
    var page = session.open("/docs/");
    page.evaluate("scrollTo(0, (document.documentElement.scrollHeight - innerHeight) / 2)");
    page.waitForFunction("() => scrollY > 0");
    var width = Session.DESKTOP_WIDTH;

    var start = Screenshots.meanColor(page, width * 0.05, 0, width * 0.15, 2);
    var end = Screenshots.meanColor(page, width * 0.8, 0, width * 0.15, 2);
    var top = Screenshots.meanColor(page, width * 0.8, 6, width * 0.15, 2);

    assertThat(Screenshots.difference(start, end))
        .as("filled start against empty end")
        .isGreaterThan(40);
    assertThat(Screenshots.difference(end, top)).as("the empty end shows the header").isLessThan(8);
  }

  private static void assertStill(Page page, String action) {
    assertThat((List<?>) page.evaluate(MOVING)).as("moving animations after %s", action).isEmpty();
  }

  private static List<Locator> tiles(Session session) {
    return TILES.stream().flatMap(label -> session.text(label).all().stream()).toList();
  }

  private static String translate(Locator element) {
    return String.valueOf(element.evaluate("el => getComputedStyle(el).translate"));
  }

  private static double opacity(Locator element) {
    return Session.number(element.evaluate("el => parseFloat(getComputedStyle(el).opacity)"));
  }
}
