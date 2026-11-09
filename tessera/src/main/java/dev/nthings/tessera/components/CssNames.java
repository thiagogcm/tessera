package dev.nthings.tessera.components;

import java.util.Locale;

/// Names the built-in components derive from their enum options.
final class CssNames {

  private CssNames() {}

  /// The lowercase constant name of `value`, as used in modifier classes and data attributes:
  /// `Tone.WARNING` gives `warning`.
  static String of(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }
}
