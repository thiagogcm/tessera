package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Action;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/// Options shared by [Button] and [IconButton], read by [ButtonFace] at render time.
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

  public B tone(Button.Tone tone) {
    this.tone = tone;
    return self();
  }

  public B variant(Button.Variant variant) {
    this.variant = variant;
    return self();
  }

  public B size(Button.Size size) {
    this.size = size;
    return self();
  }

  /// Participates in native form submission (`type="submit"`).
  public B submit() {
    this.submit = true;
    return self();
  }

  public B command(ElementRef target, BuiltinCommand command) {
    this.commandTarget = target;
    this.command = command;
    this.action = null;
    return self();
  }

  public B dispatchAction(ElementRef target, Action action) {
    this.commandTarget = target;
    this.command = null;
    this.action = action;
    return self();
  }

  public B id(ElementRef ref) {
    this.domRef = ref;
    return self();
  }

  public B disabled(boolean disabled) {
    this.disabled = disabled;
    return self();
  }

  public B disabled() {
    return disabled(true);
  }

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
