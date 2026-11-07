package dev.nthings.tessera.showcase.e2e;

import com.microsoft.playwright.Page;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Runs axe-core against a page through the WebJar-bundled script.
final class Axe {

  private static final String SOURCE = load();

  private Axe() {}

  /// WCAG A and AA violations, formatted as one block per rule for a readable test failure.
  static String violations(Page page) {
    page.addScriptTag(new Page.AddScriptTagOptions().setContent(SOURCE));
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

  private static String load() {
    try (var input =
        Objects.requireNonNull(
            Axe.class.getResourceAsStream("/META-INF/resources/webjars/axe-core/4.14.0/axe.min.js"),
            "axe-core WebJar resource")) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read axe-core", e);
    }
  }
}
