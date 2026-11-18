package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ColorScheme;
import com.microsoft.playwright.options.ForcedColors;
import com.microsoft.playwright.options.ReducedMotion;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Accessibility")
class AccessibilityIT {

  /// A route audited in one color scheme.
  record Audit(String route, ColorScheme scheme) {
    @Override
    public String toString() {
      return route + " in " + scheme.name().toLowerCase(java.util.Locale.ROOT) + " mode";
    }
  }

  /// Something that opens on top of a page, and how a visitor opens it.
  record Overlay(
      String name, String route, Consumer<Session> open, Function<Session, Locator> shown) {
    @Override
    public String toString() {
      return name;
    }
  }

  static Stream<Audit> audits() {
    return SiteSmokeIT.routes().stream()
        .flatMap(
            route -> Stream.of(ColorScheme.LIGHT, ColorScheme.DARK).map(s -> new Audit(route, s)));
  }

  static List<Overlay> overlays() {
    return List.of(
        new Overlay(
            "search palette",
            "/",
            s ->
                s.page()
                    .getByRole(
                        AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName(Pattern.compile("^Search")))
                    .click(),
            s -> s.page().getByRole(AriaRole.DIALOG)),
        new Overlay(
            "Pages menu", "/", s -> s.button("Pages").click(), s -> s.link("Layout").first()),
        new Overlay(
            "inspector drawer",
            "/",
            s -> s.button("Inspector").click(),
            s -> s.dialog("Compiler & Cascade Inspector")),
        new Overlay(
            "modal dialog",
            "/overlays/",
            s -> s.button("Open modal").click(),
            s -> s.dialog("Publish release?")),
        new Overlay(
            "start drawer",
            "/overlays/",
            s -> s.button("Open start drawer").click(),
            s -> s.dialog("Filters")),
        new Overlay(
            "popover",
            "/overlays/",
            s -> s.button("Toggle popover").click(),
            s -> s.text("Click outside or press Escape to close.")),
        new Overlay(
            "tooltip",
            "/overlays/",
            s -> s.button("Hover me").hover(),
            s -> s.page().getByRole(AriaRole.TOOLTIP)),
        new Overlay(
            "footnote preview",
            "/docs/",
            s -> s.link("Footnote 2").hover(),
            s -> s.page().getByRole(AriaRole.TOOLTIP)));
  }

  /// An overlay audited in dark mode.
  record DarkOverlay(Overlay overlay) {
    @Override
    public String toString() {
      return overlay + " in dark mode";
    }
  }

  @BrowserTest(cases = "audits")
  @DisplayName("pages have no WCAG A/AA violations in light or dark mode")
  void pagesHaveNoViolations(Session session, Audit audit) {
    // Settled pages: entry animations would otherwise skew contrast.
    session
        .page()
        .emulateMedia(
            new Page.EmulateMediaOptions()
                .setColorScheme(audit.scheme())
                .setReducedMotion(ReducedMotion.REDUCE));
    var page = session.open(audit.route());

    assertThat(Axe.violations(page)).as("axe violations on %s", audit).isEmpty();
  }

  @BrowserTest(cases = "overlays")
  @DisplayName("open overlays have no WCAG A/AA violations")
  void openOverlaysHaveNoViolations(Session session, Overlay overlay) {
    audit(session, overlay, ColorScheme.LIGHT);
  }

  @BrowserTest(cases = "darkOverlays")
  @DisplayName("open overlays have no WCAG A/AA violations in dark mode")
  void openOverlaysHaveNoViolationsInDarkMode(Session session, DarkOverlay dark) {
    audit(session, dark.overlay(), ColorScheme.DARK);
  }

  static Stream<DarkOverlay> darkOverlays() {
    return overlays().stream().map(DarkOverlay::new);
  }

  private static void audit(Session session, Overlay overlay, ColorScheme scheme) {
    session
        .page()
        .emulateMedia(
            new Page.EmulateMediaOptions()
                .setColorScheme(scheme)
                .setReducedMotion(ReducedMotion.REDUCE));
    var page = session.open(overlay.route());
    overlay.open().accept(session);
    assertThat(overlay.shown().apply(session)).isVisible();

    assertThat(Axe.violations(page)).as("axe violations with the %s open", overlay).isEmpty();
  }

  @BrowserTest
  @DisplayName("in forced colors, a switch shows whether it is on")
  void switchStateSurvivesForcedColors(Session session) {
    forcedColors(session).open("/forms/");
    var toggle = session.role(AriaRole.SWITCH, "Weekly digest");
    toggle.scrollIntoViewIfNeeded();
    var off = toggle.screenshot();

    toggle.check();

    assertThat(toggle).isChecked();
    assertThat(toggle.screenshot()).as("on and off look different").isNotEqualTo(off);
  }

  @BrowserTest
  @DisplayName("in forced colors, the selected tab and current page stand out")
  void selectionSurvivesForcedColors(Session session) {
    var page = forcedColors(session).open("/navigation/");
    var nav = session.role(AriaRole.NAVIGATION, "Primary navigation");
    DisclosureIT.assertUnderlined(page, navLink(nav, "Navigation"), navLink(nav, "Forms"));

    var java = session.role(AriaRole.TAB, "Java");
    java.scrollIntoViewIfNeeded();
    DisclosureIT.assertUnderlined(page, java, session.role(AriaRole.TAB, "HTML"));
  }

  @BrowserTest
  @DisplayName("in forced colors, the focused search result is visibly focused")
  void searchFocusSurvivesForcedColors(Session session) {
    var page = forcedColors(session).open("/");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^Search")))
        .click();
    var dialog = page.getByRole(AriaRole.DIALOG);
    dialog.getByRole(AriaRole.SEARCHBOX).fill("drawer");
    var first = dialog.getByRole(AriaRole.LINK).first();
    var resting = first.screenshot();

    page.keyboard().press("ArrowDown");

    assertThat(first).isFocused();
    assertThat(first.screenshot()).as("focused and resting result").isNotEqualTo(resting);
  }

  private static Locator navLink(Locator nav, String name) {
    return nav.getByRole(
        AriaRole.LINK, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }

  private static Session forcedColors(Session session) {
    session
        .page()
        .emulateMedia(
            new Page.EmulateMediaOptions()
                .setForcedColors(ForcedColors.ACTIVE)
                .setReducedMotion(ReducedMotion.REDUCE));
    return session;
  }
}
