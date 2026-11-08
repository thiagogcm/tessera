package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Action;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/// Options shared by [Button] and [IconButton]. Each setter returns the same button, for chaining.
abstract class ButtonBase<B extends ButtonBase<B>> implements FluentBuilder<B> {

  Button.Tone tone;
  Button.Variant variant;
  Button.Size size = Button.Size.MD;
  boolean submit;
  boolean disabled;
  boolean busy;
  @Nullable ElementRef domRef;
  @Nullable ElementRef commandTarget;
  @Nullable BuiltinCommand command;
  @Nullable Action action;
  @Nullable UnaryOperator<TagElement> tooltip;

  ButtonBase(Button.Tone tone, Button.Variant variant) {
    this.tone = tone;
    this.variant = variant;
  }

  /// Sets the semantic color; `ACCENT` for [Button] and `NEUTRAL` for [IconButton] by default.
  public B tone(Button.Tone tone) {
    this.tone = tone;
    return self();
  }

  /// Sets the surface: `SOLID`, `OUTLINE`, or `GHOST`. The default is `SOLID` for [Button] and
  /// `GHOST` for [IconButton].
  public B variant(Button.Variant variant) {
    this.variant = variant;
    return self();
  }

  /// Sets the control size. The default is `MD`.
  public B size(Button.Size size) {
    this.size = size;
    return self();
  }

  /// Makes the button `type="submit"`, so it submits its enclosing form. Buttons default to
  /// `type="button"`.
  public B submit() {
    this.submit = true;
    return self();
  }

  /// Runs a built-in command on the element bound to `target`, replacing any earlier `command` or
  /// `dispatchAction` call. Dialog commands need a `<dialog>` target and popover commands need an
  /// element with a `popover` attribute, or the page fails with `incompatible-command-target`. A
  /// target that is never bound fails with `unbound-reference`.
  public B command(ElementRef target, BuiltinCommand command) {
    this.commandTarget = target;
    this.command = command;
    this.action = null;
    return self();
  }

  /// Dispatches `action` to the element bound to `target` as a custom invoker command, replacing
  /// any earlier `command` or `dispatchAction` call.
  public B dispatchAction(ElementRef target, Action action) {
    this.commandTarget = target;
    this.command = null;
    this.action = action;
    return self();
  }

  /// Binds this button's DOM id to `ref`, so other elements can reference it.
  public B id(ElementRef ref) {
    this.domRef = ref;
    return self();
  }

  /// Sets the native `disabled` attribute, so the button cannot be activated or focused.
  public B disabled(boolean disabled) {
    this.disabled = disabled;
    return self();
  }

  public B disabled() {
    return disabled(true);
  }

  /// Sets `aria-busy="true"` and a progress cursor. The button stays enabled and clickable, so
  /// the application must guard the action itself.
  public B busy(boolean busy) {
    this.busy = busy;
    return self();
  }

  public B busy() {
    return busy(true);
  }

  /// Hint popover shown on hover or focus (`interestfor`).
  public B interestTooltip(UnaryOperator<TagElement> tooltip) {
    this.tooltip = tooltip;
    return self();
  }
}
