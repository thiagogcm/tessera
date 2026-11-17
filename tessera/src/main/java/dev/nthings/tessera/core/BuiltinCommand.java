package dev.nthings.tessera.core;

import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Native Invoker Command, set with [TagElement#command(ElementRef, Command)]. `SHOW_MODAL`,
/// `CLOSE` and `REQUEST_CLOSE` need a `<dialog>` target, and the popover commands need a target
/// with a `popover` attribute; otherwise the compiler reports `incompatible-command-target`. An
/// unbound target reports `unbound-reference`.
public enum BuiltinCommand implements Command, WireToken {
  SHOW_MODAL(Target.DIALOG),
  CLOSE(Target.DIALOG),
  REQUEST_CLOSE(Target.DIALOG),
  TOGGLE_POPOVER(Target.POPOVER),
  SHOW_POPOVER(Target.POPOVER),
  HIDE_POPOVER(Target.POPOVER);

  /// Compiler-facing: the kind of element a built-in command acts on.
  public enum Target {
    /// A `<dialog>` element.
    DIALOG,
    /// An element with a `popover` attribute.
    POPOVER
  }

  private final Target target;

  BuiltinCommand(Target target) {
    this.target = target;
  }

  /// Compiler-facing: the kind of element this command needs as its target.
  public Target target() {
    return target;
  }

  /// Compiler-facing: the command whose attribute value is `value`, ignoring case, or `null` for a
  /// custom or unknown command.
  public static @Nullable BuiltinCommand fromWireValue(String value) {
    var lower = value.toLowerCase(Locale.ROOT);
    for (var command : values()) {
      if (command.wireValue().equals(lower)) {
        return command;
      }
    }
    return null;
  }

  /// The attribute value, such as `show-modal`.
  @Override
  public String wireValue() {
    return WireToken.super.wireValue();
  }

  /// Always empty: built-in commands carry no payload.
  @Override
  public Map<String, String> payload() {
    return Map.of();
  }
}
