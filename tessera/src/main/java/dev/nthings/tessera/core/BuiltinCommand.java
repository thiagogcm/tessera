package dev.nthings.tessera.core;

import java.util.Map;

/// Native Invoker Command, set with [TagElement#command(ElementRef, Command)]. `SHOW_MODAL`,
/// `CLOSE` and `REQUEST_CLOSE` need a `<dialog>` target, and the popover commands need a target
/// with a `popover` attribute; otherwise the compiler reports `incompatible-command-target`. An
/// unbound target reports `unbound-reference`.
public enum BuiltinCommand implements Command {
  SHOW_MODAL,
  CLOSE,
  REQUEST_CLOSE,
  TOGGLE_POPOVER,
  SHOW_POPOVER,
  HIDE_POPOVER;

  /// The attribute value, such as `show-modal`.
  @Override
  public String wireValue() {
    return WireToken.of(this);
  }

  /// Always empty: built-in commands carry no payload.
  @Override
  public Map<String, String> payload() {
    return Map.of();
  }
}
