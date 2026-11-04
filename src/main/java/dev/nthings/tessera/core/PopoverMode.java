package dev.nthings.tessera.core;

import java.util.Locale;

/// Native HTML Popover API modes (`popover` attribute).
public enum PopoverMode {
  AUTO,
  MANUAL,
  HINT;

  private final String wireValue = name().toLowerCase(Locale.ROOT).replace('_', '-');

  public String wireValue() {
    return wireValue;
  }
}
