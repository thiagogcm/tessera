package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ReducedMotion;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Navigation")
class NavigationIT {

  /// Records, in every document the context opens, whether it was revealed by a view transition.
  private static final String RECORD_REVEAL =
      """
      addEventListener('pagereveal', (event) => {
        window.revealedWithTransition = event.viewTransition !== null;
      });
      """;

  @BrowserTest
  @DisplayName("the primary navigation and the table of contents reach their targets")
  void navigationReachesItsTargets(Session session) {
    var page = session.open("/");

    nav(session, "Primary navigation").getByRole(AriaRole.LINK, exactly("Forms")).click();
    assertThat(page).hasURL(session.url("/forms/"));

    nav(session, "On this page").getByRole(AriaRole.LINK, exactly("Switch")).click();
    assertThat(page).hasURL(session.url("/forms/#switches"));
    var heading =
        page.getByRole(
            AriaRole.HEADING, new Page.GetByRoleOptions().setName("Switch").setExact(true));
    page.waitForFunction(
        "el => { const r = el.getBoundingClientRect(); return r.top >= 0 && r.bottom <= innerHeight; }",
        heading.elementHandle());
    assertThat(Session.unobscured(heading)).as("heading clear of the sticky header").isTrue();
  }

  @BrowserTest
  @DisplayName("breadcrumbs and the pager lead between pages")
  void breadcrumbsAndPagerLeadBetweenPages(Session session) {
    var page = session.open("/forms/");

    nav(session, "Documentation navigation")
        .getByRole(
            AriaRole.LINK,
            new Locator.GetByRoleOptions().setName(Pattern.compile("Next.*Data & code")))
        .click();
    assertThat(page).hasURL(session.url("/data/"));
    nav(session, "Documentation navigation")
        .getByRole(
            AriaRole.LINK,
            new Locator.GetByRoleOptions().setName(Pattern.compile("Previous.*Forms")))
        .click();
    assertThat(page).hasURL(session.url("/forms/"));
    nav(session, "Breadcrumb").getByRole(AriaRole.LINK, exactly("Tessera")).click();
    assertThat(page).hasURL(session.url("/"));
  }

  @BrowserTest
  @DisplayName("on a phone, the pager stacks its links one above the other")
  void pagerStacksOnPhones(Session session) {
    session.width(Session.PHONE_WIDTH).open("/navigation/");
    var pager = nav(session, "Documentation navigation");
    var previous = Session.box(pager.getByRole(AriaRole.LINK).first());
    var next = Session.box(pager.getByRole(AriaRole.LINK).last());

    assertThat(next.y)
        .as("next below previous")
        .isGreaterThanOrEqualTo(previous.y + previous.height);
    assertThat(next.width).as("cards keep the full width").isCloseTo(previous.width, within(1.0));
  }

  @BrowserTest
  @DisplayName("on a phone, every header control is visible and works")
  void headerControlsWorkOnPhones(Session session) {
    var page = session.width(Session.PHONE_WIDTH).open("/");

    for (var name : List.of("Pages", "Inspector")) {
      assertThat(session.button(name)).isVisible();
    }
    assertThat(
            page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^Search"))))
        .isVisible();
    nav(session, "Primary navigation").getByRole(AriaRole.LINK, exactly("Motion")).click();
    assertThat(page).hasURL(session.url("/motion/"));
    assertThat(session.fitsViewport()).isTrue();
  }

  @BrowserTest
  @DisplayName("the header stays in view while reading a long page")
  void headerStaysInView(Session session) {
    var page = session.open("/navigation/");
    var brand = page.getByRole(AriaRole.BANNER).getByRole(AriaRole.LINK, exactly("Tessera"));

    page.mouse().wheel(0, 1500);
    page.waitForFunction("() => scrollY >= 1000");

    assertThat(Session.box(brand).y).as("brand position after scrolling").isBetween(0.0, 40.0);
    assertThat(Session.unobscured(brand)).isTrue();
  }

  @BrowserTest(Engine.CHROMIUM)
  @DisplayName("the header gains a raised, translucent surface once stuck")
  void headerSurfaceChangesWhenStuck(Session session) {
    var page = session.open("/navigation/");
    var header = page.getByRole(AriaRole.BANNER);
    var resting = header.screenshot();

    page.mouse().wheel(0, 1500);
    page.waitForFunction("() => scrollY >= 1000");

    assertThat(header.evaluate("el => getComputedStyle(el).boxShadow")).isNotEqualTo("none");
    assertThat(header.screenshot()).isNotEqualTo(resting);
  }

  @BrowserTest
  @DisplayName("footnote references and backlinks lead to each other and stay in view")
  void footnotesLeadBothWays(Session session) {
    var page = session.open("/docs/");
    var references =
        page.getByRole(
            AriaRole.LINK, new Page.GetByRoleOptions().setName("Footnote 1").setExact(true));
    assertThat(references).hasCount(2);

    var repeated = references.nth(1);
    repeated.click();
    var note =
        page.getByRole(AriaRole.LISTITEM)
            .filter(new Locator.FilterOptions().setHasText("no polyfills are shipped"));
    assertThat(isTarget(note)).as("the reference leads to its note").isTrue();
    assertThat(Session.unobscured(note)).as("note clear of the sticky header").isTrue();

    session.link("Back to reference 1, occurrence 2").click();
    assertThat(isTarget(repeated)).as("the backlink returns to the second reference").isTrue();
    assertThat(Session.unobscured(repeated)).as("reference clear of the sticky header").isTrue();
  }

  @BrowserTest
  @DisplayName("a footnote reference previews its note next to it, on hover and focus")
  void footnotePreview(Session session) {
    var page = session.open("/docs/");
    var reference = session.link("Footnote 2");
    reference.scrollIntoViewIfNeeded();
    var preview =
        page.getByRole(AriaRole.TOOLTIP)
            .filter(
                new Locator.FilterOptions().setHasText("Search includes its JavaScript module"));

    reference.hover();
    assertThat(preview).isVisible();
    assertNextTo(preview, reference);

    page.mouse().move(0, 0);
    assertThat(preview).isHidden();
    reference.focus();
    assertThat(preview).isVisible();
    page.keyboard().press("Escape");
    assertThat(preview).isHidden();
  }

  @BrowserTest(Engine.CHROMIUM)
  @DisplayName("the table of contents highlights the section in view")
  void tableOfContentsFollowsScrolling(Session session) {
    var page = session.open("/docs/");
    var toc = nav(session, "On this page");
    var steps = toc.getByRole(AriaRole.LINK, exactly("Steps"));
    var hero = toc.getByRole(AriaRole.LINK, exactly("Hero image"));
    var before = steps.screenshot();

    page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Steps"))
        .scrollIntoViewIfNeeded();
    page.waitForFunction("() => scrollY > 0");

    assertThat(steps.screenshot()).as("current entry").isNotEqualTo(before);
    assertThat(style(steps)).isNotEqualTo(style(hero));
  }

  @BrowserTest
  @DisplayName("links to ids with #, %, accents and dots reach their targets, and #top the top")
  void fragmentLinksWithUnusualIds(Session session) {
    var page = session.open("/fixtures/ids/");
    for (var id : Fixtures.UNUSUAL_IDS) {
      session.link("Go to " + id).click();
      var heading =
          page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(id + " section"));
      assertThat(heading).isInViewport();
      session.link("Back to top").click();
      page.waitForFunction("() => scrollY === 0");
    }
  }

  @BrowserTest(Engine.CHROMIUM)
  @DisplayName("moving between pages animates with a view transition, both ways")
  void pagesTransitionBothWays(Session session) {
    session.context().addInitScript(RECORD_REVEAL);
    var page = session.open("/");

    clickDocsCard(session);
    assertThat(page).hasURL(session.url("/docs/"));
    assertThat(revealedWithTransition(page)).as("forward").isTrue();

    page.goBack();
    assertThat(page).hasURL(session.url("/"));
    assertThat(revealedWithTransition(page)).as("back").isTrue();
  }

  @BrowserTest(Engine.CHROMIUM)
  @DisplayName("with reduced motion, moving between pages does not animate")
  void reducedMotionSkipsPageTransitions(Session session) {
    session
        .page()
        .emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.REDUCE));
    session.context().addInitScript(RECORD_REVEAL);
    var page = session.open("/");

    clickDocsCard(session);

    assertThat(page).hasURL(session.url("/docs/"));
    assertThat(revealedWithTransition(page)).isFalse();
  }

  @BrowserTest
  @DisplayName("back and forward return to the pages left")
  void backAndForward(Session session) {
    var page = session.open("/");
    clickDocsCard(session);
    assertThat(page).hasURL(session.url("/docs/"));

    page.goBack();
    assertThat(page).hasURL(session.url("/"));
    assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1)))
        .isVisible();
    page.goForward();
    assertThat(page).hasURL(session.url("/docs/"));
    assertThat(
            page.getByRole(
                AriaRole.IMG, new Page.GetByRoleOptions().setName("Tessera documentation banner")))
        .isVisible();
  }

  /// Clicks the banner image of the Docs card, which the card's stretched link covers.
  private static void clickDocsCard(Session session) {
    var page = session.page();
    var image =
        page.getByRole(
            AriaRole.IMG, new Page.GetByRoleOptions().setName("Documentation page banner"));
    image.evaluate("el => el.scrollIntoView({ block: 'center' })");
    var box = Session.box(image);
    page.mouse().click(box.x + box.width / 2, box.y + box.height / 2);
  }

  private static boolean revealedWithTransition(Page page) {
    page.waitForFunction("() => window.revealedWithTransition !== undefined");
    return (Boolean) page.evaluate("window.revealedWithTransition");
  }

  private static Locator nav(Session session, String name) {
    return session.role(AriaRole.NAVIGATION, name);
  }

  private static Locator.GetByRoleOptions exactly(String name) {
    return new Locator.GetByRoleOptions().setName(name).setExact(true);
  }

  private static String style(Locator element) {
    return (String)
        element.evaluate(
            "el => { const s = getComputedStyle(el); return s.color + s.fontWeight + s.backgroundColor; }");
  }

  /// Whether the URL fragment now names `element` or an element around it.
  private static boolean isTarget(Locator element) {
    return Boolean.TRUE.equals(
        element.evaluate("el => document.querySelector(':target')?.contains(el) ?? false"));
  }

  /// Fails unless `popup` sits just above or below `trigger` and overlaps it horizontally.
  static void assertNextTo(Locator popup, Locator trigger) {
    var p = Session.box(popup);
    var t = Session.box(trigger);
    assertThat(p.x).as("popup starts before the trigger ends").isLessThan(t.x + t.width);
    assertThat(p.x + p.width).as("popup ends after the trigger starts").isGreaterThan(t.x);
    var gapAbove = t.y - (p.y + p.height);
    var gapBelow = p.y - (t.y + t.height);
    assertThat(Math.max(gapAbove, gapBelow))
        .as("gap between popup and trigger")
        .isBetween(-1.0, 16.0);
  }
}
