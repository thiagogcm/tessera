package dev.nthings.tessera.core;

/// Modes of the native Popover API, set with [InteractiveElement#popover(PopoverMode)]. `AUTO`
/// closes on light dismiss and when another auto popover opens, `MANUAL` closes only through a
/// command or script, and `HINT` is for tooltip-style popovers.
public enum PopoverMode implements WireToken {
  AUTO,
  MANUAL,
  HINT
}
