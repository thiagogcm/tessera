package dev.nthings.tessera.core;

import java.util.Locale;

/// Enum whose kebab-case constant name is its attribute value.
interface WireToken {

  String name();

  default String wireValue() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
