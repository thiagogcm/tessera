package dev.nthings.tessera.core;

import java.util.Locale;

/// Kebab-case wire spelling of enum constants: `SHOW_MODAL` gives `show-modal`.
final class WireToken {

  private WireToken() {}

  /// The attribute value: the constant name in lower case with `_` replaced by `-`.
  static String of(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
