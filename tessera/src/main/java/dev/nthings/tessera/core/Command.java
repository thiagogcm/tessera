package dev.nthings.tessera.core;

import java.util.Map;

/// What an invoker button does to its target, written as the `command` attribute and set with
/// [TagElement#command(ElementRef, Command)]. Either a native [BuiltinCommand], checked against its
/// target, or a custom command from [#custom(String, String)], which any existing target accepts.
public sealed interface Command permits BuiltinCommand, CustomCommand {

  /// The `command` attribute value, such as `show-modal` or `--cart-add`.
  String wireValue();

  /// Attributes set on the invoking element together with the command, such as `data-id`. Empty for
  /// built-in commands.
  Map<String, String> payload();

  /// Creates the custom command `--<namespace>-<name>`, handled by the application's own script on
  /// the target's `command` event. Each part is sanitized: characters outside `[A-Za-z0-9_-]`
  /// become `-`. Throws IllegalArgumentException if a part is empty.
  static Command custom(String namespace, String name) {
    return new CustomCommand(namespace, name, Map.of());
  }

  /// Creates a custom command as [#custom(String, String)] does, with `payload` attributes set on
  /// the invoker, such as `data-sku`. Payload keys follow the rules of [TagElement#attr(String,
  /// String)]: plain ASCII attribute names other than `id`, `role`, `aria-*`, inline event handlers
  /// (`on*`) and `srcdoc`, in any letter case. Throws IllegalArgumentException if a part is empty
  /// or a payload key breaks those rules.
  static Command custom(String namespace, String name, Map<String, String> payload) {
    return new CustomCommand(namespace, name, payload);
  }
}
