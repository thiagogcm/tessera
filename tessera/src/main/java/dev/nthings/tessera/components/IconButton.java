package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.Tone;

/// Icon-only `<button>` created with [#of(String, IntoElement)]. The label you pass is its only
/// accessible name, because the icon is decorative and hidden from assistive technology, so pass a
/// non-blank label. It defaults to the neutral tone, ghost variant and medium size, and takes the
/// same tone, variant, size, command and disabled settings as [Button].
public final class IconButton extends ButtonBase<IconButton> {

  private final String label;
  private final IntoElement icon;

  private IconButton(String label, IntoElement icon) {
    super(Tone.NEUTRAL, Button.Variant.GHOST);
    this.label = label;
    this.icon = icon;
  }

  /// Creates an icon button named `label` that shows `icon`.
  public static IconButton of(String label, IntoElement icon) {
    return new IconButton(label, icon);
  }

  @Override
  public IconButton self() {
    return this;
  }

  @Override
  IntoElement expand(DocumentContext context) {
    return ButtonFace.element(this, true).ariaLabel(label).child(ButtonFace.icon(icon));
  }
}
