package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import java.util.List;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Layout and content")
class LayoutIT {

  @BrowserTest
  @DisplayName("grids put tiles side by side on wide screens and stack them on phones")
  void gridsReflowWithWidth(Session session) {
    session.open("/layout/");
    var one = Session.box(session.text("One"));
    var two = Session.box(session.text("Two"));
    var wide = Session.box(session.text("Span 2 columns"));
    var cell = Session.box(session.text("Cell").first());
    assertThat(two.y).as("One and Two on one row").isCloseTo(one.y, within(1.0));
    assertThat(wide.width).as("a two-column cell spans two cells").isGreaterThan(cell.width * 1.8);

    session.width(Session.PHONE_WIDTH);

    one = Session.box(session.text("One"));
    two = Session.box(session.text("Two"));
    assertThat(two.x).as("Two below One").isCloseTo(one.x, within(1.0));
    assertThat(two.y).isGreaterThan(one.y + one.height - 1);
    assertThat(Session.box(session.text("Span 2 columns")).width)
        .as("spans collapse on a phone")
        .isCloseTo(Session.box(session.text("Cell").first()).width, within(1.0));
  }

  @BrowserTest
  @DisplayName("card footers line up across a row")
  void cardFootersLineUp(Session session) {
    session.open("/layout/");
    var shortCard = card(session, "Stretched link");
    var longCard = card(session, "Release notes");
    assertThat(Session.box(longCard).y)
        .as("cards on one row")
        .isCloseTo(Session.box(shortCard).y, within(1.0));

    assertThat(Session.box(session.button("Save for later")).y)
        .isCloseTo(Session.box(session.button("Read more")).y, within(1.0));
  }

  @BrowserTest
  @DisplayName("a stretched-link card opens its page from anywhere but its own buttons")
  void stretchedLinkCard(Session session) {
    var page = session.open("/layout/");

    session.button("Save for later").click();
    assertThat(page).hasURL(session.url("/layout/"));

    var body = session.text("Click anywhere on this card to open the Data page.");
    var box = Session.box(body);
    page.mouse().click(box.x + box.width / 2, box.y + box.height / 2);
    assertThat(page).hasURL(session.url("/data/"));
  }

  @BrowserTest
  @DisplayName(
      "a component inside prose looks the same as outside it, while prose styles its own text")
  void proseLeavesComponentsAlone(Session session) {
    var page = session.open("/layout/");
    var titles = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Changelog"));
    assertThat(titles).hasCount(2);
    var stage = page.locator("#prose");
    var paragraph = page.getByText("Every interaction on this site");

    assertThat(look(titles.last())).as("card title inside prose").isEqualTo(look(titles.first()));
    assertThat(Session.box(paragraph).width)
        .as("prose keeps a reading measure")
        .isLessThan(Session.box(stage).width * 0.9);
  }

  @BrowserTest
  @DisplayName("a description list pairs each term with its definition on wide screens")
  void descriptionListPairsTerms(Session session) {
    session.open("/layout/");
    var term = Session.box(session.text("Runtime"));
    var definition = Session.box(session.text("Latest Chromium + Firefox"));

    assertThat(definition.y).isCloseTo(term.y, within(2.0));
    assertThat(definition.x).isGreaterThan(term.x + term.width);
  }

  @BrowserTest
  @DisplayName("compact density tightens spacing and keeps text the same size")
  void compactDensityTightensSpacing(Session session) {
    var page = session.open("/fixtures/density-regular/");
    var regularButton = Session.box(session.button("Publish"));
    var regularField = Session.box(page.getByLabel("Release name"));
    var regularText = fontSize(session.button("Publish"));

    session.open("/fixtures/density-compact/");
    var compactButton = Session.box(session.button("Publish"));
    var compactField = Session.box(page.getByLabel("Release name"));

    assertThat(compactButton.height).isLessThan(regularButton.height);
    assertThat(compactField.height).isLessThan(regularField.height);
    assertThat(fontSize(session.button("Publish"))).isEqualTo(regularText);
  }

  @BrowserTest
  @DisplayName("in a container widened by dragging, a link still shows its hover style")
  void hoverBeatsContainerStyle(Session session) {
    var page = session.open("/motion/");
    var link = session.link("More layouts");
    link.evaluate("el => el.scrollIntoView({ block: 'center' })");
    var corner = resizableBox(link);
    var narrow = color(link);

    page.mouse().move(corner.x + corner.width - 3, corner.y + corner.height - 3);
    page.mouse().down();
    page.mouse()
        .move(
            corner.x + corner.width + 300,
            corner.y + corner.height - 3,
            new Mouse.MoveOptions().setSteps(10));
    page.mouse().up();
    assertThat(resizableBox(link).width).isGreaterThan(corner.width);
    page.mouse().move(0, 0);
    var wide = color(link);
    link.hover();
    var hovered = color(link);

    assertThat(wide).as("wide container restyles the link").isNotEqualTo(narrow);
    assertThat(hovered).as("hover wins over the container style").isNotEqualTo(wide);
  }

  private static Locator card(Session session, String title) {
    var heading =
        session.page().getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(title));
    return session
        .page()
        .getByRole(AriaRole.ARTICLE)
        .filter(new Locator.FilterOptions().setHas(heading))
        .first();
  }

  private static BoundingBox resizableBox(Locator inside) {
    var box =
        (List<?>)
            inside.evaluate(
                """
                el => {
                  while (el && getComputedStyle(el).resize === 'none') el = el.parentElement;
                  const r = el.getBoundingClientRect();
                  return [r.x, r.y, r.width, r.height];
                }
                """);
    var result = new BoundingBox();
    result.x = Session.number(box.get(0));
    result.y = Session.number(box.get(1));
    result.width = Session.number(box.get(2));
    result.height = Session.number(box.get(3));
    return result;
  }

  private static String look(Locator element) {
    return (String)
        element.evaluate(
            """
            el => {
              const s = getComputedStyle(el);
              return [s.fontSize, s.fontWeight, s.color, s.marginBlockStart, s.lineHeight].join(' ');
            }
            """);
  }

  private static String color(Locator element) {
    return (String) element.evaluate("el => getComputedStyle(el).color");
  }

  private static double fontSize(Locator element) {
    return Session.number(element.evaluate("el => parseFloat(getComputedStyle(el).fontSize)"));
  }
}
