package dev.nthings.tessera.showcase.e2e;

import com.microsoft.playwright.Page;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/// Runs axe-core against a page through the WebJar-bundled script.
final class Axe {

  private static final String SOURCE = load();

  private Axe() {}

  /// Loads axe-core into every document `page` navigates to from now on, sending the script once.
  static void install(Page page) {
    page.addInitScript(SOURCE);
  }

  /// WCAG A and AA violations in the current document of a page [#install]ed earlier, formatted as
  /// one block per rule for a readable test failure.
  static String violations(Page page) {
    var result =
        page.evaluate(
            """
            async () => {
              const results = await axe.run(document, {
                runOnly: {
                  type: 'tag',
                  values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
                },
                resultTypes: ['violations'],
              });
              return results.violations
                .map((violation) =>
                  `${violation.id} (${violation.impact}): ${violation.help}\\n  ` +
                  violation.nodes.map((node) => node.target.join(' ')).join('\\n  '),
                )
                .join('\\n');
            }
            """);
    return result instanceof String text ? text : String.valueOf(result);
  }

  /// Reads the script of whichever axe-core version the WebJar dependency resolves to.
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
