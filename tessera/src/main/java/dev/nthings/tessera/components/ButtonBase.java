package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Command;
import dev.nthings.tessera.core.ElementBlock;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TagHost;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Options shared by [Button] and [IconButton]. Each setter returns the same button, for chaining.
/// A button opens no id scope of its own: it renders a single `<button>` styled with element-level
/// styles, so the refs it binds and targets resolve in the enclosing scope.
abstract class ButtonBase<B extends ButtonBase<B>> extends BuiltinComponent
    implements TagHost, FluentBuilder<B> {

  Tone tone;
  Button.Variant variant;
  Size size = Size.MD;
  boolean submit;
  boolean disabled;
  boolean busy;
  final List<ElementRef> domRefs = new ArrayList<>(1);
  @Nullable ElementRef commandTarget;
  @Nullable Command command;
  @Nullable ElementBlock tooltip;

  ButtonBase(Tone tone, Button.Variant variant) {
    this.tone = tone;
    this.variant = variant;
  }

  /// Sets the semantic color; `ACCENT` for [Button] and `NEUTRAL` for [IconButton] by default. It
  /// does not change the surface or behavior.
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
  public B size(Size size) {
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

  /// Binds `ref` to the `<button>` element, so other elements can reference it. The ref binds in
  /// the enclosing id scope, so `ElementRef.scoped("save")` inside an id scope with id `toolbar`
  /// emits `toolbar--save`. Binding several refs aliases them, as [TagElement#id(ElementRef)] does.
  public B id(ElementRef ref) {
    domRefs.add(ref);
    return self();
  }

  /// Sets the native `disabled` attribute, so the button cannot be activated or focused. Use
  /// `onlyIf(condition, B::disabled)` to disable it conditionally.
  public B disabled() {
    this.disabled = true;
    return self();
  }

  /// Sets `aria-busy="true"` and a progress cursor. The button stays enabled and clickable, so the
  /// application must guard the action itself.
  public B busy() {
    this.busy = true;
    return self();
  }

  /// Adds a hint popover shown while the button is hovered or focused (`interestfor`); `tooltip`
  /// fills and styles the popover element, as [TagElement#interestTooltip(ElementBlock)] does.
  public B interestTooltip(ElementBlock tooltip) {
    this.tooltip = tooltip;
    return self();
  }

  /// The `<button>` element this configuration renders, built now. Wrapping this button directly in
  /// `DocumentElement.of(...)` or `LiveElement.of(...)` calls this method automatically through
  /// [TagHost]. Later setters do not change the returned element.
  @Override
  public abstract TagElement toTagElement();

  @Override
  final IntoElement expand(ViewContext context) {
    return toTagElement();
  }
}
