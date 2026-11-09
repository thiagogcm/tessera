package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.Tone;
import java.util.List;

/// Native `<button>`, `type="button"` unless [#submit()] is called. Create one with
/// [#of(String, IntoElement...)]; it is an accent, solid, medium button by default. Use a link for
/// navigation.
public final class Button extends ButtonBase<Button> {

  /// Surface: `SOLID` (filled with the tone, the default), `OUTLINE` (transparent with a tone
  /// border), or `GHOST` (transparent with tone text).
  public enum Variant {
    SOLID,
    OUTLINE,
    GHOST
  }

  private final String label;
  private final List<IntoElement> content;

  private Button(String label, List<IntoElement> content) {
    super(Tone.ACCENT, Variant.SOLID);
    this.label = label;
    this.content = content;
  }

  /// Creates a button named `label`. Without `content`, `label` is its visible text. With
  /// `content`, the button shows the content and `label` becomes its `aria-label`, which assistive
  /// technology announces instead of the content text, so keep it matching the visible words.
  public static Button of(String label, IntoElement... content) {
    return new Button(label, List.of(content));
  }

  @Override
  public Button self() {
    return this;
  }

  @Override
  IntoElement expand(DocumentContext context) {
    var el = ButtonFace.element(this, false);
    if (content.isEmpty()) {
      el.text(label);
    } else {
      el.ariaLabel(label);
      content.forEach(el::child);
    }
    return el;
  }
}
