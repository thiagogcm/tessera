package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import java.util.List;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;

/// Keyed rows without wrapper elements on the lists page, edited while the list changes.
@DisplayName("Keyed lists")
class KeyedListIT {
  private static final Pattern ENTRY = Pattern.compile("^Entry ");

  @BothEngines
  @DisplayName(
      "a row keeps its value, focus and selection while rows are inserted, moved and removed")
  void rowsKeepTheirStateThroughChanges(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/lists/");
    session.field("Live feed").check();
    var beta = session.field("Entry b");
    beta.fill("Edited Beta");
    Session.select(beta, 2, 7, "forward");

    page.clock().runFor(60_000);
    assertOrder(session, "item-4", "a", "b", "c");
    assertKept(beta);

    page.keyboard().press("Alt+ArrowUp");
    assertOrder(session, "item-4", "b", "a", "c");
    assertKept(beta);
    page.keyboard().press("Alt+ArrowDown");
    page.keyboard().press("Alt+ArrowDown");
    assertOrder(session, "item-4", "a", "c", "b");
    assertKept(beta);

    session.button("Remove first entry").click();
    session.button("Remove first entry").click();
    assertOrder(session, "c", "b");
    assertThat(beta).hasValue("Edited Beta");
    assertThat(session.field("Entry a")).hasCount(0);
  }

  @BothEngines
  @DisplayName("a retained row's button sees the latest state and a replaced row type starts empty")
  void retainedRowsUseCurrentHandlers(Session session) {
    session.open("/lists/");
    var status = session.page().getByRole(AriaRole.STATUS);
    var beta = session.field("Entry b");
    beta.fill("Kept while compatible");

    session.button("Refresh entries").click();
    assertThat(status).hasText("Revision: 1; choices: 0; last: none");
    session.button("Choose b").click();
    assertThat(status).hasText("Revision: 1; choices: 1; last: b@1");
    session.button("Refresh entries").click();
    session.button("Choose b").click();
    assertThat(status).hasText("Revision: 2; choices: 2; last: b@2");
    assertThat(beta).hasValue("Kept while compatible");

    session.button("Replace entry type").click();
    assertThat(beta).hasAccessibleDescription("Replacement for b");
    assertThat(beta).hasValue("");
    assertThat(session.field("Entry a")).hasAccessibleDescription("Description for a");
    session.button("Choose b").click();
    assertThat(status).hasText("Revision: 2; choices: 3; last: b@2");
  }

  @BothEngines
  @DisplayName("live rows expose the same accessibility tree and styles as the static page")
  void liveRowsMatchStaticRows(Session session) {
    session.open("/lists/");
    session.button("Toggle details").click();
    assertThat(session.text("Additional details")).isVisible();
    var live = entries(session);
    var liveTree = live.ariaSnapshot();
    var liveStyles = styles(live);
    assertThat(live.locator("svg")).isVisible();

    session.open("/lists/static/");
    var compiled = entries(session);
    assertThat(compiled.locator("svg")).isVisible();
    Assertions.assertThat(compiled.ariaSnapshot()).isEqualTo(liveTree);
    Assertions.assertThat(styles(compiled)).isEqualTo(liveStyles);
  }

  private static Locator entries(Session session) {
    return session.role(AriaRole.REGION, "Editable entries");
  }

  /// Computed styles a user sees of each row's input and button.
  private static Object styles(Locator entries) {
    return entries.evaluate(
        """
        section => [...section.querySelectorAll('input, button')].map(node => {
          const style = getComputedStyle(node);
          return ['display', 'color', 'background-color', 'border-top-style', 'font-size',
                  'font-weight'].map(name => style.getPropertyValue(name)).join(' ');
        })
        """);
  }

  private static void assertOrder(Session session, String... keys) {
    var labels = new String[keys.length];
    for (var index = 0; index < keys.length; index++) {
      labels[index] = "Entry " + keys[index];
    }
    assertThat(session.page().getByText(ENTRY)).hasText(labels);
  }

  private static void assertKept(Locator beta) {
    assertThat(beta).isFocused();
    assertThat(beta).hasValue("Edited Beta");
    Assertions.assertThat(Session.selection(beta)).isEqualTo(List.of(2, 7, "forward"));
  }
}
