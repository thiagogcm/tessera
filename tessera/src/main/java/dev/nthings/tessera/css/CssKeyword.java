package dev.nthings.tessera.css;

import java.util.Locale;

/// An enum whose constants are CSS keywords; the kebab-case constant name is the CSS value.
public non-sealed interface CssKeyword extends CssValue {

  String name();

  @Override
  default String toCss() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
