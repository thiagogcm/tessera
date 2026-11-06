package dev.nthings.tessera.core;

import java.util.Locale;

/// An enum whose kebab-case constant name is the attribute value that represents it.
interface WireToken {

  /// The constant name, supplied by [Enum#name()].
  String name();

  default String wireValue() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
