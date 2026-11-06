package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.ScopeStyle;

/// Icon-only native `<button>`. `accessibleName` is the accessible name. The icon is decorative
/// and is hidden from assistive technology. Tone, surface, and size use the same choices as
/// [Button].
public final class IconButton extends ButtonBase<IconButton> implements RenderView {

  private final String name;
  private final IntoElement icon;

  private IconButton(String name, IntoElement icon) {
    super(Button.Tone.NEUTRAL, Button.Variant.GHOST);
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

  @Override
  public void configureScope(ScopeStyle scope) {
    ButtonFace.configure(scope, true);
  }

  @Override
  public Element render() {
    return ButtonFace.element(this, true)
        .ariaLabel(name)
        .child(span().attr("class", "button-icon").ariaHidden(true).child(icon));
  }
}
