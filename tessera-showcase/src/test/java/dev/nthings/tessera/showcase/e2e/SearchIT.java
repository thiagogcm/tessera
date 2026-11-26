package dev.nthings.tessera.showcase.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Search")
class SearchIT {

  private static final Pattern SEARCH_BUTTON = Pattern.compile("^Search");

  @BrowserTest
  @DisplayName("typing filters the results, and the keyboard opens one")
  void filtersAndOpensByKeyboard(Session session) {
    var page = session.open("/");
    var dialog = openSearch(session);
    var query = dialog.getByRole(AriaRole.SEARCHBOX);
    assertThat(query).isFocused();

    query.fill("modal");
    var results = dialog.getByRole(AriaRole.LINK);
    assertThat(results).hasCount(1);
    query.press("ArrowDown");
    assertThat(result(dialog, "Modal dialog")).isFocused();
    page.keyboard().press("Enter");

    assertThat(page).hasURL(session.url("/overlays/#modal"));
    assertThat(session.button("Open modal")).isInViewport();
  }

  @BrowserTest
  @DisplayName("a query without matches says so, and Enter opens the first match")
  void emptyResultsAndEnter(Session session) {
    var page = session.open("/");
    var dialog = openSearch(session);
    var query = dialog.getByRole(AriaRole.SEARCHBOX);

    query.fill("not-a-real-component");
    assertThat(dialog.getByRole(AriaRole.STATUS)).hasText(Pattern.compile("\\S"));
    assertThat(dialog.getByRole(AriaRole.STATUS)).isVisible();
    assertThat(dialog.getByRole(AriaRole.LINK)).hasCount(0);

    query.fill("");
    assertThat(dialog.getByRole(AriaRole.STATUS)).isHidden();
    assertThat(dialog.getByRole(AriaRole.LINK).first()).isVisible();

    query.fill("auto growing textarea");
    assertThat(dialog.getByRole(AriaRole.LINK)).hasCount(1);
    query.press("Enter");
    assertThat(page).hasURL(session.url("/forms/#textarea"));
    assertThat(dialog).isHidden();
  }

  @BrowserTest
  @DisplayName("the advertised Ctrl+K shortcut opens search")
  void controlKOpensSearch(Session session) {
    var page = session.open("/");
    var trigger =
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(SEARCH_BUTTON));
    assertThat(trigger).hasAttribute("aria-keyshortcuts", "Control+K Meta+K /");

    page.keyboard().press("Control+K");

    var dialog = page.getByRole(AriaRole.DIALOG);
    assertThat(dialog).isVisible();
    assertThat(dialog.getByRole(AriaRole.SEARCHBOX)).isFocused();
  }

  @BrowserTest
  @DisplayName("slash opens search, but types a slash inside a text field")
  void slashOpensSearchOutsideFields(Session session) {
    var page = session.open("/forms/");
    var field = page.getByRole(AriaRole.TEXTBOX).first();
    field.fill("");
    field.press("/");
    assertThat(field).hasValue("/");
    assertThat(page.getByRole(AriaRole.DIALOG)).isHidden();

    page.locator("body").click(new Locator.ClickOptions().setPosition(1, 1));
    page.keyboard().press("/");

    var dialog = page.getByRole(AriaRole.DIALOG);
    assertThat(dialog).isVisible();
    assertThat(dialog.getByRole(AriaRole.SEARCHBOX)).hasValue("");
  }

  @BrowserTest
  @DisplayName("arrow keys wrap around the results")
  void arrowKeysWrap(Session session) {
    var page = session.open("/");
    var dialog = openSearch(session);
    var query = dialog.getByRole(AriaRole.SEARCHBOX);
    query.fill("drawer");
    var results = dialog.getByRole(AriaRole.LINK);
    assertThat(results.first()).isVisible();

    query.press("ArrowUp");
    assertThat(results.last()).isFocused();
    page.keyboard().press("ArrowDown");
    assertThat(results.first()).isFocused();
  }

  @BrowserTest
  @DisplayName("clicking a result closes search and opens the page")
  void clickOpensResult(Session session) {
    var page = session.open("/");
    var dialog = openSearch(session);
    dialog.getByRole(AriaRole.SEARCHBOX).fill("pager");

    result(dialog, "Pager").click();

    assertThat(page).hasURL(session.url("/navigation/#pager"));
    assertThat(dialog).isHidden();
  }

  @BrowserTest
  @DisplayName("two palettes on one page search their own links, ignoring accents")
  void palettesSearchIndependently(Session session) {
    var page = session.open("/fixtures/search/");
    page.getByRole(
            AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^Find articles")))
        .click();
    var dialog = page.getByRole(AriaRole.DIALOG);
    var query = dialog.getByRole(AriaRole.SEARCHBOX);

    query.fill("cafe");
    assertThat(dialog.getByRole(AriaRole.LINK)).hasCount(1);
    assertThat(result(dialog, "Café guide")).isVisible();
    query.press("Escape");
    assertThat(dialog).isHidden();

    page.getByRole(
            AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^Find guides")))
        .click();
    var other = page.getByRole(AriaRole.DIALOG);
    var otherQuery = other.getByRole(AriaRole.SEARCHBOX);
    assertThat(otherQuery).hasValue("");
    assertThat(other.getByRole(AriaRole.LINK)).hasCount(2);
    otherQuery.fill("guide");
    otherQuery.press("ArrowUp");
    var forms = result(other, "Forms guide");
    assertThat(forms).isFocused();
    forms.press("Enter");

    assertThat(page).hasURL(Pattern.compile("#forms$"));
    assertThat(other).isHidden();
    assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Forms guide")))
        .isInViewport();
  }

  private static Locator openSearch(Session session) {
    var page = session.page();
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(SEARCH_BUTTON)).click();
    var dialog = page.getByRole(AriaRole.DIALOG);
    assertThat(dialog).isVisible();
    return dialog;
  }

  private static Locator result(Locator dialog, String title) {
    return dialog.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(title));
  }
}
