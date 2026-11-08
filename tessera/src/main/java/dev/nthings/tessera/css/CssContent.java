package dev.nthings.tessera.css;

import java.util.List;
import java.util.stream.Collectors;
import org.owasp.encoder.Encode;

/// A `content` value, or a custom property holding a string.
public sealed interface CssContent extends CssValue
    permits CssContent.Text,
        CssContent.Counter,
        CssContent.Property,
        CssContent.Sequence,
        CssContent.Alternative {

  /// A quoted string; the text is escaped for CSS.
  record Text(String value) implements CssContent {
    @Override
    public String toCss() {
      return "\"" + Encode.forCssString(value) + "\"";
    }
  }

  /// `counter(name)`.
  record Counter(CssIdent name) implements CssContent {
    @Override
    public String toCss() {
      return "counter(" + name.toCss() + ")";
    }
  }

  /// `var(--name)` of a custom property that holds a string.
  record Property(CssIdent name) implements CssContent {
    public Property {
      name.requireCustom();
    }

    @Override
    public String toCss() {
      return name.varReference();
    }
  }

  /// Parts joined with spaces.
  record Sequence(List<CssContent> parts) implements CssContent {
    public Sequence {
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

  static CssContent text(String value) {
    return new Text(value);
  }

  static CssContent empty() {
    return new Text("");
  }

  static CssContent counter(CssIdent name) {
    return new Counter(name);
  }

  static CssContent property(CssIdent customProperty) {
    return new Property(customProperty);
  }

  static CssContent sequence(CssContent... parts) {
    return new Sequence(List.of(parts));
  }

  /// A glyph hidden from assistive technology.
  static CssContent decorative(String glyph) {
    return new Alternative(text(glyph), empty());
  }

  /// A glyph that assistive technology announces as `name`.
  static CssContent labelled(String glyph, String name) {
    return new Alternative(text(glyph), text(name));
  }

  /// `content` that assistive technology reads as the concatenation of `alternative`.
  static CssContent withAlternative(CssContent content, CssContent... alternative) {
    return new Alternative(content, sequence(alternative));
  }
}
