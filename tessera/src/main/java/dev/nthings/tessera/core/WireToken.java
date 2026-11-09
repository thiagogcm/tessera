package dev.nthings.tessera.core;

import java.util.Locale;

/// Enum whose kebab-case constant name is its attribute value: the one place that spells the
/// mapping, so `SHOW_MODAL` gives `show-modal`.
interface WireToken {

  String name();

  /// The attribute value: the constant name in lower case with `_` replaced by `-`.
  default String wireValue() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
