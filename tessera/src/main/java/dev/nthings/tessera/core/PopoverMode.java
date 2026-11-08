package dev.nthings.tessera.core;

import java.util.Locale;

/// Modes of the native Popover API, set with [InteractiveElement#popover(PopoverMode)]. `AUTO`
/// closes on light dismiss and when another auto popover opens, `MANUAL` closes only through a
/// command or script, and `HINT` is for tooltip-style popovers.
public enum PopoverMode {
  AUTO,
  MANUAL,
  HINT;

  private final String wireValue = name().toLowerCase(Locale.ROOT);

  public String wireValue() {
    return wireValue;
  }
}
