package dev.nthings.tessera.core;

import java.util.Locale;

/// Native Invoker Command for the `command` attribute, set with [InteractiveElement#command].
/// `SHOW_MODAL`, `CLOSE` and `REQUEST_CLOSE` need a `<dialog>` target, and the popover commands
/// need a target with a `popover` attribute; otherwise the compiler reports
/// `incompatible-command-target`. An unbound target reports `unbound-reference`, and a literal
/// `commandfor` (set with `attr`) that names no element reports `missing-command-target`.
public enum BuiltinCommand {
  SHOW_MODAL,
  CLOSE,
  REQUEST_CLOSE,
  TOGGLE_POPOVER,
  SHOW_POPOVER,
  HIDE_POPOVER;

  private final String wireValue = name().toLowerCase(Locale.ROOT).replace('_', '-');

  /// Returns the attribute value, such as `show-modal`.
  public String wireValue() {
    return wireValue;
  }
}
