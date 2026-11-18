package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.List;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Overlays")
class OverlaysIT {

  /// A drawer demo: its trigger, its title, and the window edge it docks to.
  record DrawerCase(String trigger, String title, boolean startEdge) {
    @Override
    public String toString() {
      return title + (startEdge ? " (start edge)" : " (end edge)");
    }
  }

  static List<DrawerCase> drawers() {
    return List.of(
        new DrawerCase("Open start drawer", "Filters", true),
        new DrawerCase("Open end drawer", "Details", false));
  }

  static List<Integer> widths() {
    return List.of(Session.PHONE_WIDTH, Session.DESKTOP_WIDTH);
  }

  @BrowserTest
  @DisplayName("a modal opens from the keyboard, keeps focus inside, and gives it back")
  void modalKeepsAndReturnsFocus(Session session) {
    var page = session.open("/overlays/");
    var trigger = session.button("Open modal");
    var dialog = session.dialog("Publish release?");
    var close = dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Got it"));

    trigger.focus();
    page.keyboard().press("Enter");
    assertThat(dialog).isVisible();
    assertThat(close).isFocused();
    for (var i = 0; i < 4; i++) {
      page.keyboard().press("Tab");
      assertThat(
              page.evaluate(
                  "document.activeElement === document.body || !!document.activeElement.closest('dialog[open]')"))
          .as("focus after %d Tab presses stays out of the page behind", i + 1)
          .isEqualTo(true);
    }
    page.keyboard().press("Escape");
    assertThat(dialog).isHidden();
    assertThat(trigger).isFocused();

    trigger.click();
    close.click();
    assertThat(dialog).isHidden();
    assertThat(trigger).isFocused();
  }

  @BrowserTest
  @DisplayName("clicking the backdrop closes a modal")
  void backdropClickClosesModal(Session session) {
    var page = session.open("/overlays/");
    session.button("Open modal").click();
    var dialog = session.dialog("Publish release?");
    assertThat(dialog).isVisible();

    page.mouse().click(5, 5);

    assertThat(dialog).isHidden();
  }

  @BrowserTest
  @DisplayName("on a phone, an open modal fits the window")
  void modalFitsPhones(Session session) {
    session.width(Session.PHONE_WIDTH).open("/overlays/");
    session.button("Open modal").click();
    var box = Session.box(session.dialog("Publish release?"));

    assertThat(box.x).isGreaterThanOrEqualTo(0);
    assertThat(box.x + box.width).isLessThanOrEqualTo(Session.PHONE_WIDTH);
    assertThat(session.fitsViewport()).isTrue();
  }

  @BrowserTest
  @DisplayName("a popover toggles, light-dismisses, and returns focus on Escape")
  void popoverTogglesAndDismisses(Session session) {
    var page = session.open("/overlays/");
    var trigger = session.button("Toggle popover");
    var popover = session.text("Click outside or press Escape to close.");

    trigger.click();
    assertThat(popover).isVisible();
    page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1)).click();
    assertThat(popover).isHidden();

    trigger.click();
    trigger.click();
    assertThat(popover).isHidden();

    trigger.focus();
    page.keyboard().press("Enter");
    assertThat(popover).isVisible();
    page.keyboard().press("Escape");
    assertThat(popover).isHidden();
    assertThat(trigger).isFocused();
  }

  @BrowserTest(cases = "drawers")
  @DisplayName("a drawer docks to its edge, closes on Escape, and returns focus")
  void drawerDocksAndReturnsFocus(Session session, DrawerCase drawerCase) {
    for (var width : widths()) {
      var page = session.width(width).open("/overlays/");
      var trigger = session.button(drawerCase.trigger());
      var drawer = session.dialog(drawerCase.title());

      trigger.focus();
      page.keyboard().press("Enter");
      assertThat(drawer).isVisible();
      page.waitForFunction(
          "el => el.getAnimations().every(a => a.playState === 'finished')",
          drawer.elementHandle());
      var box = Session.box(drawer);
      var edge = drawerCase.startEdge() ? box.x : box.x + box.width;
      assertThat(edge)
          .as("docked edge at %dpx", width)
          .isCloseTo(drawerCase.startEdge() ? 0 : session.contentRight(), within(1.0));
      assertThat(box.width).isLessThanOrEqualTo(width);

      page.keyboard().press("Escape");
      assertThat(drawer).isHidden();
      assertThat(trigger).isFocused();
    }
  }

  @BrowserTest
  @DisplayName("in right-to-left content, the start drawer docks to and slides from the right")
  void startDrawerInRightToLeft(Session session) {
    var page = session.open("/fixtures/rtl/");
    var drawer = session.dialog("Filters");

    session.button("Open filters").click();
    assertThat(drawer).isVisible();
    // Where the entry slide starts: every running transition rewound to its first frame.
    var slideStart =
        (List<?>)
            drawer.evaluate(
                """
                el => {
                  const moves = el.getAnimations();
                  moves.forEach(a => { a.pause(); a.currentTime = 0; });
                  const x = el.getBoundingClientRect().x;
                  moves.forEach(a => a.finish());
                  return [x, moves.length];
                }
                """);
    page.waitForFunction("el => el.getAnimations().length === 0", drawer.elementHandle());
    var docked = Session.box(drawer);

    assertThat(docked.x + docked.width)
        .as("docked to the right edge")
        .isCloseTo(session.contentRight(), within(1.0));
    assertThat(Session.number(slideStart.get(1))).as("the drawer slides in").isPositive();
    assertThat(Session.number(slideStart.get(0)))
        .as("the slide starts at the right edge, not across the window")
        .isGreaterThanOrEqualTo(docked.x - 1);
  }

  @BrowserTest(cases = "widths")
  @DisplayName("the Pages menu opens next to its trigger, inside the window")
  void pagesMenuOpensNextToTrigger(Session session, Integer width) {
    var page = session.width(width).open("/");
    var trigger = session.button("Pages");

    trigger.focus();
    page.keyboard().press("Enter");
    var inspector = session.button("Open inspector");
    assertThat(inspector).isVisible();
    page.keyboard().press("Tab");
    var first = page.locator(":focus");
    assertThat(first).hasText("Overview");

    var t = Session.box(trigger);
    for (var item : List.of(first, inspector)) {
      var box = Session.box(item);
      assertThat(box.x).isGreaterThanOrEqualTo(0);
      assertThat(box.x + box.width).isLessThanOrEqualTo(width);
      assertThat(box.y + box.height).isLessThanOrEqualTo(Session.HEIGHT);
    }
    assertThat(Session.box(first).y - (t.y + t.height))
        .as("gap below the trigger")
        .isBetween(0.0, 24.0);

    page.keyboard().press("Escape");
    assertThat(inspector).isHidden();
    assertThat(trigger).isFocused();

    trigger.click();
    assertThat(inspector).isVisible();
    page.mouse().click(width - 4, Session.HEIGHT - 4);
    assertThat(inspector).isHidden();
  }

  @BrowserTest
  @DisplayName("the inspector opened from the Pages menu returns focus to the menu button")
  void inspectorFromPagesMenuReturnsFocus(Session session) {
    var page = session.open("/");
    var trigger = session.button("Pages");
    var inspector = session.dialog("Compiler & Cascade Inspector");

    trigger.focus();
    page.keyboard().press("Enter");
    session.button("Open inspector").focus();
    page.keyboard().press("Enter");
    assertThat(inspector).isVisible();
    page.keyboard().press("Escape");

    assertThat(inspector).isHidden();
    assertThat(trigger).isFocused();
  }

  @BrowserTest
  @DisplayName("the inspector opens from the header and returns focus")
  void inspectorFromHeader(Session session) {
    var page = session.open("/");
    var trigger = session.button("Inspector");
    var inspector = session.dialog("Compiler & Cascade Inspector");

    trigger.click();
    assertThat(inspector).isVisible();
    page.keyboard().press("Escape");

    assertThat(inspector).isHidden();
    assertThat(trigger).isFocused();
  }

  @BrowserTest
  @DisplayName("a tooltip appears next to its button on hover and focus, and Escape hides it")
  void tooltipFollowsInterest(Session session) {
    var page = session.open("/overlays/");
    var trigger = session.button("Hover me");
    var tooltip =
        page.getByRole(AriaRole.TOOLTIP)
            .filter(new Locator.FilterOptions().setHasText("Saved 2 minutes ago"));
    trigger.scrollIntoViewIfNeeded();

    assertThat(trigger).hasAccessibleDescription("Saved 2 minutes ago");
    trigger.hover();
    assertThat(tooltip).isVisible();
    NavigationIT.assertNextTo(tooltip, trigger);
    page.mouse().move(0, 0);
    assertThat(tooltip).isHidden();

    session.tabTo(trigger);
    assertThat(tooltip).isVisible();
    page.keyboard().press("Escape");
    assertThat(tooltip).isHidden();
    page.keyboard().press("Shift+Tab");
    page.keyboard().press("Tab");
    assertThat(tooltip).isVisible();
    page.keyboard().press("Tab");
    assertThat(trigger).not().isFocused();
    assertThat(tooltip).isHidden();
  }

  @BrowserTest
  @DisplayName("a field tooltip sits above its control, even when the control id has dots")
  void fieldTooltipWithDottedId(Session session) {
    var page = session.open("/fixtures/ids/");
    var field = page.getByLabel("Contact email", new Page.GetByLabelOptions().setExact(true));
    var tooltip =
        page.getByRole(AriaRole.TOOLTIP)
            .filter(new Locator.FilterOptions().setHasText("A work address is best"));

    field.hover();

    assertThat(tooltip).isVisible();
    var t = Session.box(tooltip);
    var f = Session.box(field);
    assertThat(t.y + t.height)
        .as("tooltip bottom against the control top")
        .isCloseTo(f.y, within(8.0));
    assertThat(t.x).isLessThan(f.x + f.width);
  }
}
