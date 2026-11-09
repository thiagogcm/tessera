package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import java.util.List;

/// Native `<button>`, `type="button"` unless [Button#submit()] is called. Use an anchor for
/// navigation.
public final class Button extends ButtonBase<Button> implements View {

  /// Surface: `SOLID` (filled with the tone, the default), `OUTLINE` (transparent with a tone
  /// border), or `GHOST` (transparent with tone text).
  public enum Variant {
    SOLID,
    OUTLINE,
    GHOST
  }

  /// Control size. Padding follows density; type size does not.
  public enum Size {
    SM(Tokens.FONT_SIZE_XS, Tokens.SPACE_3, Tokens.SPACE_1, Tokens.SPACE_8),
    MD(Tokens.CONTROL_FONT_SIZE, Tokens.CONTROL_PADDING_INLINE, Tokens.SPACE_2, Tokens.SPACE_10),
    LG(Tokens.FONT_SIZE_MD, Tokens.SPACE_5, Tokens.SPACE_3, Tokens.SPACE_12);

    private final DesignToken.Length fontSize;
    private final DesignToken.Length paddingInline;
    private final DesignToken.Length paddingBlock;
    private final DesignToken.Length iconBox;

    Size(
        DesignToken.Length fontSize,
        DesignToken.Length paddingInline,
        DesignToken.Length paddingBlock,
        DesignToken.Length iconBox) {
      this.fontSize = fontSize;
      this.paddingInline = paddingInline;
      this.paddingBlock = paddingBlock;
      this.iconBox = iconBox;
    }

    DesignToken.Length fontSize() {
      return fontSize;
    }

    DesignToken.Length paddingInline() {
      return paddingInline;
    }

    DesignToken.Length paddingBlock() {
      return paddingBlock;
    }

    DesignToken.Length iconBox() {
      return iconBox;
    }
  }

  private final String name;
  private final List<IntoElement> content;

  private Button(String name, List<IntoElement> content) {
    super(Tone.ACCENT, Variant.SOLID);
    this.name = name;
    this.content = content;
  }

  /// Button whose visible text is its accessible name.
  public static Button of(String label) {
    return new Button(label, List.of());
  }

  /// Button with custom `content`. `accessibleName` is set as `aria-label`, so assistive technology
  /// announces it instead of the content text. Keep it matching the visible words.
  public static Button of(String accessibleName, IntoElement... content) {
    return new Button(accessibleName, List.of(content));
  }

  @Override
  public Button self() {
    return this;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    ButtonFace.configure(scope, false);
  }

  @Override
  public Element render(RenderContext context) {
    var el = ButtonFace.element(this, false);
    if (content.isEmpty()) {
      el.text(name);
    } else {
      el.ariaLabel(name);
      content.forEach(el::child);
    }
    return el;
  }
}
