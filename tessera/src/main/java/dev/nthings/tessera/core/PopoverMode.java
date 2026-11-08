package dev.nthings.tessera.core;

import java.util.Locale;

/// Native Popover API modes (`popover` attribute).
public enum PopoverMode {
  AUTO,
  MANUAL,
  HINT;

  private final String wireValue = name().toLowerCase(Locale.ROOT);

  public String wireValue() {
    return wireValue;
  }
}
