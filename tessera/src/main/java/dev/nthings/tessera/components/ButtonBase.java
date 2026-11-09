package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Command;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/// Options shared by [Button] and [IconButton]. Each setter returns the same button, for chaining.
abstract class ButtonBase<B extends ButtonBase<B>> implements FluentBuilder<B> {

  Tone tone;
  Button.Variant variant;
  Button.Size size = Button.Size.MD;
  boolean submit;
  boolean disabled;
  boolean busy;
  final List<ElementRef> domRefs = new ArrayList<>(1);
  @Nullable ElementRef commandTarget;
  @Nullable Command command;
  @Nullable UnaryOperator<TagElement> tooltip;

  ButtonBase(Tone tone, Button.Variant variant) {
    this.tone = tone;
    this.variant = variant;
  }

  /// Sets the semantic color; `ACCENT` for [Button] and `NEUTRAL` for [IconButton] by default.
  /// It does not change the surface or behavior.
  public B tone(Tone tone) {
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

  /// Runs `command` on the element bound to `target`, replacing any earlier command. Dialog
  /// commands need a `<dialog>` target and popover commands need an element with a `popover`
  /// attribute, or the page fails with `incompatible-command-target`; custom commands accept any
  /// target. A target that is never bound fails with `unbound-reference`.
  public B command(ElementRef target, Command command) {
    this.commandTarget = target;
    this.command = command;
    return self();
  }

  /// Binds `ref` to this button, so other elements can reference it. Binding several refs aliases
  /// them, as [TagElement#id(ElementRef)] does.
  public B id(ElementRef ref) {
    domRefs.add(ref);
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
