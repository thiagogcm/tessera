package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import java.util.List;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Tabs, accordions and carousels")
class DisclosureIT {

  @BrowserTest
  @DisplayName("content tabs switch panels by click and keyboard, with one tab stop")
  void contentTabs(Session session) {
    var page = session.open("/navigation/");
    var java = tab(session, "Java");
    var html = tab(session, "HTML");
    var javaPanel = session.text("Components are plain Java records and builders.");
    var htmlPanel = session.text("Output is semantic, validated HTML.");
    assertThat(javaPanel).isVisible();
    assertThat(htmlPanel).isHidden();

    html.click();
    assertThat(page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setSelected(true)))
        .hasText("HTML");
    assertThat(htmlPanel).isVisible();
    assertThat(javaPanel).isHidden();

    page.keyboard().press("ArrowLeft");
    assertThat(java).isFocused();
    assertThat(javaPanel).isVisible();
    page.keyboard().press("End");
    assertThat(tab(session, "CSS")).isFocused();
    assertThat(session.text("Styles compile into one layered stylesheet.")).isVisible();
    page.keyboard().press("Home");
    assertThat(java).isFocused();

    page.keyboard().press("Tab");
    assertThat(session.role(AriaRole.TABPANEL, "Java")).isFocused();
    page.keyboard().press("Shift+Tab");
    assertThat(java).isFocused();
  }

  @BrowserTest
  @DisplayName("in right-to-left content, arrow keys follow the visual order")
  void tabsInRightToLeft(Session session) {
    var page = session.open("/fixtures/rtl/");
    var first = tab(session, "First");
    first.focus();

    page.keyboard().press("ArrowLeft");
    assertThat(tab(session, "Second")).isFocused();
    assertThat(session.text("Second stage.")).isVisible();
    page.keyboard().press("ArrowRight");
    assertThat(first).isFocused();
    assertUnderlined(page, first, tab(session, "Second"));
  }

  @BrowserTest
  @DisplayName("each tab bar underlines its own active item")
  void tabBarsUnderlineTheirActiveItem(Session session) {
    var page = session.open("/navigation/");
    var primary = session.role(AriaRole.NAVIGATION, "Primary navigation");
    assertUnderlined(page, link(primary, "Navigation"), link(primary, "Forms"));
    var section = session.role(AriaRole.NAVIGATION, "Section navigation");
    section.scrollIntoViewIfNeeded();
    assertUnderlined(page, link(section, "Navigation"), link(section, "Layout"));

    var java = tab(session, "Java");
    var html = tab(session, "HTML");
    java.scrollIntoViewIfNeeded();
    assertUnderlined(page, java, html);
    html.click();
    page.waitForFunction(
        "el => el.closest('[role=tablist]').getAnimations({ subtree: true }).length === 0",
        html.elementHandle());
    assertUnderlined(page, html, java);
  }

  @BrowserTest
  @DisplayName("an exclusive accordion opens one item at a time, by click or keyboard")
  void exclusiveAccordion(Session session) {
    var page = session.open("/navigation/");
    var first = session.text("No. Disclosure is native HTML.");
    var second = session.text("Leave out exclusive() for that.");
    assertThat(first).isVisible();
    assertThat(second).isHidden();

    summary(session, "Can several items be open?").click();
    assertThat(second).isVisible();
    assertThat(first).isHidden();
    page.waitForFunction("el => el.getBoundingClientRect().height > 0", second.elementHandle());

    var question = summary(session, "Does it need JavaScript?");
    question.focus();
    page.keyboard().press("Enter");
    assertThat(first).isVisible();
    assertThat(second).isHidden();
    page.keyboard().press("Enter");
    assertThat(first).isHidden();
  }

  @BrowserTest
  @DisplayName("two FAQs built with the same id stay independent")
  void accordionsWithTheSameIdStayIndependent(Session session) {
    session.open("/navigation/");
    var gettingStarted = session.text("No. Disclosure is native HTML.");
    var styling = session.text("Yes, through the design tokens.");
    assertThat(gettingStarted).isVisible();
    assertThat(styling).isVisible();

    summary(session, "Does it animate?").click();

    assertThat(session.text("Only when motion is allowed.")).isVisible();
    assertThat(styling).isHidden();
    assertThat(gettingStarted).isVisible();
  }

  @BrowserTest
  @DisplayName("on a phone, a carousel scrolls to the next slide from the keyboard")
  void carouselScrollsFromKeyboard(Session session) {
    var page = session.width(Session.PHONE_WIDTH).open("/navigation/");
    var track = session.role(AriaRole.REGION, "Platform highlights");
    track.scrollIntoViewIfNeeded();
    var second =
        track.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Scroll markers"));

    track.focus();
    page.keyboard().press("ArrowRight");

    page.waitForFunction("el => el.scrollLeft > 0", track.elementHandle());
    assertThat(second).isInViewport();
  }

  @BrowserTest(Engine.CHROMIUM)
  @DisplayName("each carousel's previous and next buttons scroll only their own track")
  void carouselButtonsScrollTheirOwnTrack(Session session) {
    var page = session.open("/navigation/");
    var platform = session.role(AriaRole.REGION, "Platform highlights");
    var specials = session.role(AriaRole.REGION, "Café specials");
    specials.evaluate("el => el.scrollIntoView({ block: 'center' })");

    for (var track : List.of(specials, platform)) {
      var next = scrollButton(track, "inline-end");
      assertThat(inside(next, Session.box(track))).as("next button on its track").isTrue();
      page.mouse().click(next[0], next[1]);
      page.waitForFunction("el => el.scrollLeft > 0", track.elementHandle());
    }
    page.mouse()
        .click(
            scrollButton(specials, "inline-start")[0], scrollButton(specials, "inline-start")[1]);
    page.waitForFunction("el => el.scrollLeft === 0", specials.elementHandle());
    assertThat(Session.number(platform.evaluate("el => el.scrollLeft")))
        .as("the other track")
        .isPositive();
  }

  @BrowserTest(Engine.CHROMIUM)
  @DisplayName("in right-to-left content, carousel buttons sit on the track and scroll it")
  void carouselButtonsInRightToLeft(Session session) {
    var page = session.open("/fixtures/rtl/");
    var track = session.role(AriaRole.REGION, "Highlights");
    track.evaluate("el => el.scrollIntoView({ block: 'center' })");
    var box = Session.box(track);
    var previous = scrollButton(track, "inline-start");
    var next = scrollButton(track, "inline-end");

    assertThat(inside(previous, box)).as("previous button on the track").isTrue();
    assertThat(inside(next, box)).as("next button on the track").isTrue();
    assertThat(next[0]).as("next button on the left in right-to-left").isLessThan(previous[0]);
    page.mouse().click(next[0], next[1]);
    page.waitForFunction("el => el.scrollLeft < 0", track.elementHandle());
    assertThat(track.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Slide 2")))
        .isInViewport();
  }

  private static Locator tab(Session session, String name) {
    return session.role(AriaRole.TAB, name);
  }

  private static Locator link(Locator nav, String name) {
    return nav.getByRole(
        AriaRole.LINK, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }

  private static Locator summary(Session session, String question) {
    return session
        .page()
        .locator("summary")
        .filter(new Locator.FilterOptions().setHasText(question));
  }

  /// Fails unless a line is drawn under `active` and not under `other`.
  static void assertUnderlined(Page page, Locator active, Locator other) {
    var under = Screenshots.bottomEdge(page, active);
    var notUnder = Screenshots.bottomEdge(page, other);
    assertThat(Screenshots.difference(under, notUnder))
        .as("line under %s and not under %s", active, other)
        .isGreaterThan(40);
  }

  /// The window coordinates of the centre of the carousel's `::scroll-button(<direction>)`.
  private static double[] scrollButton(Locator track, String direction) {
    var center =
        (List<?>)
            track.evaluate(
                """
                (el, direction) => {
                  const s = getComputedStyle(el, `::scroll-button(${direction})`);
                  // Offsets are from the padding box of the nearest positioned ancestor, or from
                  // the document when there is none.
                  let block = el.parentElement;
                  while (block && getComputedStyle(block).position === 'static') {
                    block = block.parentElement;
                  }
                  const box = block?.getBoundingClientRect();
                  const x = block ? box.left + block.clientLeft : -scrollX;
                  const y = block ? box.top + block.clientTop : -scrollY;
                  // The laid-out box, moved by its translate: "<x> <y>", each in px or %.
                  const width = parseFloat(s.width), height = parseFloat(s.height);
                  const shift = (value, size) =>
                    value?.endsWith('%') ? (parseFloat(value) / 100) * size : parseFloat(value) || 0;
                  const [tx, ty] = s.translate === 'none' ? [] : s.translate.split(' ');
                  return [
                    x + parseFloat(s.left) + width / 2 + shift(tx, width),
                    y + parseFloat(s.top) + height / 2 + shift(ty, height),
                  ];
                }
                """,
                direction);
    return new double[] {Session.number(center.get(0)), Session.number(center.get(1))};
  }

  private static boolean inside(double[] point, BoundingBox box) {
    return point[0] > box.x
        && point[0] < box.x + box.width
        && point[1] > box.y
        && point[1] < box.y + box.height;
  }
}
