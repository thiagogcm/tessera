package dev.nthings.tessera.css;

import java.util.List;

/// A `content` value: text, a counter, or a string custom property, combined as needed. Set it with
/// [Styled#content(CssContent)]. Build values with the static factories.
public sealed interface CssContent extends CssValue
    permits CssContents.Text,
        CssContents.CounterValue,
        CssContents.Property,
        CssContents.Sequence,
        CssContents.Alternative {

  /// A quoted, CSS-escaped string, such as `"Slide "`.
  static CssContent text(String value) {
    return new CssContents.Text(value);
  }

  /// `""`.
  static CssContent empty() {
    return text("");
  }

  /// `counter(name)`, the counter's current value.
  static CssContent counter(Counter counter) {
    return new CssContents.CounterValue(counter);
  }

  /// `var(--name)` of a string-valued custom property.
  static CssContent property(CustomProperty customProperty) {
    return new CssContents.Property(customProperty);
  }

  /// The parts joined with spaces.
  static CssContent sequence(CssContent... parts) {
    return new CssContents.Sequence(List.of(parts));
  }

  /// `"glyph" / ""`: a glyph hidden from assistive technology.
  static CssContent decorative(String glyph) {
    return new CssContents.Alternative(text(glyph), empty());
  }

  /// `"glyph" / "name"`: a glyph that assistive technology announces as `name`.
  static CssContent labelled(String glyph, String name) {
    return new CssContents.Alternative(text(glyph), text(name));
  }

  /// `content / "alternative"`: `content` that assistive technology reads as the concatenation of
  /// `alternative`.
  static CssContent withAlternative(CssContent content, CssContent... alternative) {
    return new CssContents.Alternative(content, sequence(alternative));
  }
}
