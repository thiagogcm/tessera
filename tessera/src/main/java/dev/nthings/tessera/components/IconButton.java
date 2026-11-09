package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.ScopeStyle;

/// Icon-only `<button>` created with [#of(String, IntoElement)]. The name you pass is its only
/// accessible name, because the icon is decorative and hidden from assistive technology, so pass
/// a non-blank name. It defaults to the neutral tone, ghost variant and medium size, and takes
/// the same tone, variant, size, command and disabled settings as [Button].
public final class IconButton extends ButtonBase<IconButton> implements View {

  private final String name;
  private final IntoElement icon;

  private IconButton(String name, IntoElement icon) {
    super(Button.Tone.NEUTRAL, Button.Variant.GHOST);
    this.name = name;
    this.icon = icon;
  }

  /// Creates an icon button named `accessibleName` that shows `icon`.
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
  public Element render(RenderContext context) {
    return ButtonFace.element(this, true)
        .ariaLabel(name)
        .child(span().attr("class", "button-icon").ariaHidden(true).child(icon));
  }
}
