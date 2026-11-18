package dev.nthings.tessera.showcase.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/// Runs the WebJar-bundled axe-core against the current state of a page, so an audit can include an
/// open dialog or menu.
final class Axe {

  private static final String SOURCE = load();

  private static final String REPORT =
      """
      async ([root, rules]) => {
        const options = rules
          ? { runOnly: { type: 'rule', values: rules } }
          : { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'] } };
        const results = await axe.run(root ?? document, { ...options, resultTypes: ['violations'] });
        return results.violations
          .map((violation) =>
            `${violation.id} (${violation.impact}): ${violation.help}\\n  ` +
            violation.nodes.map((node) => node.target.join(' ') + ': ' + node.failureSummary)
              .join('\\n  '))
          .join('\\n');
      }
      """;

  private Axe() {}

  /// WCAG A/AA violations in the page as it is now, one block per rule; empty when there are none.
  static String violations(Page page) {
    load(page);
    return String.valueOf(page.evaluate(REPORT, new Object[] {null, null}));
  }

  /// Color-contrast violations inside `element`; empty when its text is legible.
  static String contrastViolations(Locator element) {
    load(element.page());
    return String.valueOf(
        element.evaluate("(el, report) => (0, eval)(report)([el, ['color-contrast']])", REPORT));
  }

  private static void load(Page page) {
    if (Boolean.TRUE.equals(page.evaluate("typeof axe === 'undefined'"))) {
      page.addScriptTag(new Page.AddScriptTagOptions().setContent(SOURCE));
    }
  }

  /// Reads the axe-core script of the WebJar version on the classpath.
  private static String load() {
    try {
      var pom = new Properties();
      try (var input = open("/META-INF/maven/org.webjars.npm/axe-core/pom.properties")) {
        pom.load(input);
      }
      var script =
          "/META-INF/resources/webjars/axe-core/" + pom.getProperty("version") + "/axe.min.js";
      try (var input = open(script)) {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read axe-core", e);
    }
  }

  private static InputStream open(String path) {
    var input = Axe.class.getResourceAsStream(path);
    if (input == null) {
      throw new IllegalStateException("axe-core WebJar resource not found: " + path);
    }
    return input;
  }
}
