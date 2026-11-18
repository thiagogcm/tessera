package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Tables and code")
class DataIT {

  @BrowserTest
  @DisplayName("on a phone, a flat table turns into cards whose values carry their headers")
  void flatTableStacksIntoLabelledCards(Session session) {
    var page = session.width(1280).open("/data/");
    var table = session.role(AriaRole.TABLE, "Service health by region");
    var gateway =
        table.getByRole(AriaRole.ROW, new Locator.GetByRoleOptions().setName("gateway us-east-1"));
    assertThat(gateway).isVisible();
    assertThat(
            table.getByRole(
                AriaRole.COLUMNHEADER, new Locator.GetByRoleOptions().setName("Region")))
        .isVisible();
    assertThat(gateway.innerText()).doesNotContain("Region");

    session.width(360);

    assertThat(
            table.getByRole(
                AriaRole.COLUMNHEADER, new Locator.GetByRoleOptions().setName("Region")))
        .isHidden();
    assertThat(gateway.innerText())
        .containsSubsequence("Region", "us-east-1", "P99 latency", "3.4 ms");
    assertThat(
            table.getByRole(AriaRole.ROWHEADER, new Locator.GetByRoleOptions().setName("gateway")))
        .isVisible();
    assertThat(gateway.getByRole(AriaRole.CELL)).hasCount(4);
    assertThat(session.fitsViewport()).isTrue();
  }

  @BrowserTest
  @DisplayName("on a phone, a grouped table keeps its grid and scrolls from the keyboard")
  void groupedTableScrollsFromKeyboard(Session session) {
    var page = session.width(360).open("/data/");
    var region = session.role(AriaRole.REGION, "Quarterly infrastructure spend");
    assertThat(
            region.getByRole(
                AriaRole.COLUMNHEADER, new Locator.GetByRoleOptions().setName("Second quarter")))
        .isAttached();

    region.scrollIntoViewIfNeeded();
    region.focus();
    page.keyboard().press("ArrowRight");

    page.waitForFunction("el => el.scrollLeft > 0", region.elementHandle());
    assertThat(session.fitsViewport()).isTrue();
  }

  @BrowserTest
  @DisplayName("on a phone, a long code line scrolls from the keyboard")
  void codeScrollsFromKeyboard(Session session) {
    var page = session.width(Session.PHONE_WIDTH).open("/data/");
    var code = codeBlock(page);
    code.scrollIntoViewIfNeeded();

    code.focus();
    page.keyboard().press("ArrowRight");

    page.waitForFunction("el => el.scrollLeft > 0", code.elementHandle());
    assertThat(session.fitsViewport()).isTrue();
  }

  @BrowserTest
  @DisplayName(
      "added, deleted and highlighted code lines look different, and a diff beats a highlight")
  void codeLineStatesAreDistinct(Session session) {
    var page = session.open("/data/");
    var code = codeBlock(page);
    code.scrollIntoViewIfNeeded();

    var highlighted = lineBackground(page, code, "public IntoElement expand");
    var deleted = lineBackground(page, code, "-  return div()");
    var highlightedAndAdded = lineBackground(page, code, "+  var dialog");
    var added = lineBackground(page, code, "+  return dialog()");
    var plain = lineBackground(page, code, "}");

    assertThat(Screenshots.difference(highlighted, plain))
        .as("highlighted vs plain")
        .isGreaterThan(4);
    assertThat(Screenshots.difference(added, plain)).as("added vs plain").isGreaterThan(4);
    assertThat(Screenshots.difference(deleted, plain)).as("deleted vs plain").isGreaterThan(4);
    assertThat(Screenshots.difference(added, deleted)).as("added vs deleted").isGreaterThan(4);
    assertThat(Screenshots.difference(added, highlighted))
        .as("added vs highlighted")
        .isGreaterThan(4);
    assertThat(Screenshots.difference(highlightedAndAdded, added))
        .as("a highlighted added line looks added")
        .isLessThan(1);
  }

  private static Locator codeBlock(Page page) {
    return page.locator("pre")
        .filter(new Locator.FilterOptions().setHasText("closedBy(ClosedBy.ANY)"));
  }

  /// The mean color of the empty end of the code line that starts with `start`.
  private static double[] lineBackground(Page page, Locator code, String start) {
    var line = code.getByText(start).first();
    var lineBox = Session.box(line);
    var codeBox = Session.box(code);
    var right = codeBox.x + codeBox.width - 12;
    return Screenshots.meanColor(page, right - 24, lineBox.y + lineBox.height / 2 - 2, 24, 4);
  }
}
