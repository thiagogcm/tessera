package dev.nthings.tessera.css;

import java.util.List;
import java.util.stream.Collectors;
import org.owasp.encoder.Encode;

/// The [CssContent] implementations, built only through its static factories.
final class CssContents {

  private CssContents() {}

  /// A quoted string; the text is escaped for CSS.
  record Text(String value) implements CssContent {
    @Override
    public String toCss() {
      return "\"" + Encode.forCssString(value) + "\"";
    }
  }

  /// `counter(name)`.
  record CounterValue(Counter name) implements CssContent {
    @Override
    public String toCss() {
      return "counter(" + name.toCss() + ")";
    }
  }

  /// `var(--name)` of a custom property that holds a string.
  record Property(CustomProperty name) implements CssContent {
    @Override
    public String toCss() {
      return name.varReference();
    }
  }

  /// Parts joined with spaces.
  record Sequence(List<CssContent> parts) implements CssContent {
    Sequence {
      parts = List.copyOf(parts);
    }

    @Override
    public String toCss() {
      return parts.stream().map(CssContent::toCss).collect(Collectors.joining(" "));
    }
  }

  /// `content / alternative`: generated content with the text assistive technology reads instead.
  record Alternative(CssContent content, CssContent alternative) implements CssContent {
    @Override
    public String toCss() {
      return content.toCss() + " / " + alternative.toCss();
    }
  }
}
