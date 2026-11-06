package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Action;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.foundation.FluentBuilder;

/// Icon-only native `<button>`. `accessibleName` is the accessible name. The icon is decorative
/// and is hidden from assistive technology. Tone, surface, and size use the same choices as
/// [Button].
public final class IconButton implements RenderView, FluentBuilder<IconButton> {

  private final String name;
  private final IntoElement icon;
  private final ButtonModel model = new ButtonModel(Button.Tone.NEUTRAL, Button.Variant.GHOST);

  private IconButton(String name, IntoElement icon) {
    this.name = name;
    this.icon = icon;
  }

  public static IconButton of(String accessibleName, IntoElement icon) {
    return new IconButton(accessibleName, icon);
  }

  @Override
  public IconButton self() {
    return this;
  }

  public IconButton tone(Button.Tone tone) {
    model.tone = tone;
    return this;
  }

  public IconButton variant(Button.Variant variant) {
    model.variant = variant;
    return this;
  }

  public IconButton size(Button.Size size) {
    model.size = size;
    return this;
  }

  public IconButton submit() {
    model.submit = true;
    return this;
  }

  public IconButton command(ElementRef target, BuiltinCommand command) {
    model.commandTarget = target;
    model.command = command;
    model.action = null;
    return this;
  }

  public IconButton dispatchAction(ElementRef target, Action action) {
    model.commandTarget = target;
    model.command = null;
    model.action = action;
    return this;
  }

  public IconButton id(ElementRef ref) {
    model.domRef = ref;
    return this;
  }

  public IconButton disabled(boolean disabled) {
    model.disabled = disabled;
    return this;
  }

  public IconButton disabled() {
    return disabled(true);
  }

  public IconButton busy(boolean busy) {
    model.busy = busy;
    return this;
  }

  public IconButton busy() {
    return busy(true);
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    ButtonFace.configure(scope);
  }

  @Override
  public Element render() {
    return ButtonFace.element(model, true)
        .ariaLabel(name)
        .child(span().attr("class", "button-icon").ariaHidden(true).child(icon));
  }
}
