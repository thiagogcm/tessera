package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;

/// One test's page in one engine, opened in its class's browser context.
///
/// The session records every uncaught page error, `console.error` message and Content Security
/// Policy violation. After the test, any it did not expect fails the test: a scenario that provokes
/// a failure declares it with [#expectError] or waits for it with [#awaitError].
final class Session {
  private final String engine;
  private final Page page;
  private final String origin;
  private final BrowserSuite.Fixture fixture;
  private final List<String> errors = new ArrayList<>();
  private final List<String> expected = new ArrayList<>();

  Session(String engine, Page page, BrowserSuite.Fixture fixture) {
    this.engine = engine;
    this.page = page;
    this.fixture = fixture;
    this.origin = fixture.server().baseUrl();
    page.onPageError(errors::add);
    page.onConsoleMessage(message -> {
      if (message.type().equals("error")) {
        errors.add(describe(message));
      }
    });
  }

  /// The text of a console error. Firefox prints a logged `Error` object as just "Error", so the
  /// message of each logged error is read from the page instead.
  private static String describe(ConsoleMessage message) {
    var text = new StringBuilder(message.text());
    for (var arg : message.args()) {
      try {
        var detail = arg.evaluate("value => value instanceof Error ? value.message : null");
        if (detail != null) {
          text.append(' ').append(detail);
        }
      } catch (PlaywrightException e) {
        // The page navigated away before the argument could be read; keep the plain text.
      }
    }
    return text.toString();
  }

  Page page() {
    return page;
  }

  /// The suite's compiled sites and their servers.
  BrowserSuite.Fixture fixture() {
    return fixture;
  }

  String engine() {
    return engine;
  }

  boolean chromium() {
    return engine.equals("chromium");
  }

  /// The URL of `path` on the fixture site.
  String url(String path) {
    return origin + path;
  }

  /// Navigates to `path` on the fixture site.
  Page open(String path) {
    page.navigate(url(path));
    return page;
  }

  /// Allows errors containing non-empty `fragment`, however many are reported.
  void expectError(String fragment) {
    if (fragment.isBlank()) {
      throw new IllegalArgumentException("Expected error fragment must not be blank");
    }
    expected.add(fragment);
  }

  /// Waits until an error containing `fragment` is reported, and allows it.
  void awaitError(String fragment) {
    expectError(fragment);
    page.waitForCondition(() -> errors.stream().anyMatch(error -> error.contains(fragment)));
  }

  /// Waits until at least `count` errors matching `fragment` have been reported, and allows them.
  void awaitErrors(int count, String fragment) {
    expectError(fragment);
    page.waitForCondition(
        () -> errors.stream().filter(error -> error.contains(fragment)).count() >= count);
  }

  /// Every error reported so far, expected or not.
  List<String> errors() {
    return List.copyOf(errors);
  }

  /// The button named exactly `name`.
  Locator button(String name) {
    return page.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
  }

  /// The control labelled exactly `label`.
  Locator field(String label) {
    return page.getByLabel(label, new Page.GetByLabelOptions().setExact(true));
  }

  /// The element with `role` named exactly `name`.
  Locator role(AriaRole role, String name) {
    return page.getByRole(role, new Page.GetByRoleOptions().setName(name).setExact(true));
  }

  /// The visible text `text`, matched exactly.
  Locator text(String text) {
    return page.getByText(text, new Page.GetByTextOptions().setExact(true));
  }

  /// Asserts the items of the list named `name`, in order.
  void assertList(String name, String... items) {
    var list = role(AriaRole.LIST, name).getByRole(AriaRole.LISTITEM);
    if (items.length == 0) {
      assertThat(list).hasCount(0);
    } else {
      assertThat(list).hasText(items);
    }
  }

  /// `[selectionStart, selectionEnd, selectionDirection]` of a text control, as a list.
  static Object selection(Locator control) {
    return control.evaluate(
        "node => [node.selectionStart, node.selectionEnd, node.selectionDirection]");
  }

  /// `[selectionStart, selectionEnd]` of a text control, as a list.
  static Object caret(Locator control) {
    return control.evaluate("node => [node.selectionStart, node.selectionEnd]");
  }

  /// Selects `start` to `end` in a focused text control, as a user's shift-arrow selection would.
  static void select(Locator control, int start, int end, String direction) {
    control.focus();
    control.evaluate(
        "(node, [start, end, direction]) => node.setSelectionRange(start, end, direction)",
        List.of(start, end, direction));
  }

  void verify() {
    var unexpected =
        errors.stream().filter(error -> expected.stream().noneMatch(error::contains)).toList();
    Assertions.assertThat(unexpected).as("unexpected page errors in %s", engine).isEmpty();
  }

  void close() {
    page.close();
  }
}
