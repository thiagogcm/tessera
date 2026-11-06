package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// Native `<button>`. Tone, surface, and size are independent of submission and invoker commands.
/// An ordinary button is `type="button"`; [Button#submit] opts into form submission. Navigation
/// stays on anchors.
public final class Button extends ButtonBase<Button> implements RenderView {

  /// Semantic color. It does not choose the surface or the button's behavior.
  public enum Tone {
    NEUTRAL(Tokens.TEXT_PRIMARY, Tokens.SURFACE_CARD, Tokens.BORDER_SUBTLE, Tokens.SURFACE_SUBTLE),
    ACCENT(Tokens.ACCENT_PRIMARY, CssColor.white()),
    SUCCESS(Tokens.STATUS_SUCCESS, CssColor.white()),
    WARNING(Tokens.STATUS_WARNING, CssColor.oklch(15, 0.02, 75)),
    DANGER(Tokens.STATUS_DANGER, CssColor.white());

    private final DesignToken.Color token;
    private final CssColor onSolid;
    private final DesignToken.Color edge;
    private final CssColor wash;

    Tone(DesignToken.Color token, CssColor onSolid) {
      this(token, onSolid, token, token.mixTransparent(12));
    }

    Tone(DesignToken.Color token, CssColor onSolid, DesignToken.Color edge, CssColor wash) {
      this.token = token;
      this.onSolid = onSolid;
      this.edge = edge;
      this.wash = wash;
    }

    DesignToken.Color token() {
      return token;
    }

    CssColor onSolid() {
      return onSolid;
    }

    /// Outline border color.
    DesignToken.Color edge() {
      return edge;
    }

    /// Hover background for outline and ghost buttons.
    CssColor wash() {
      return wash;
    }
  }

  /// Surface treatment, independent of tone and behavior.
  public enum Variant {
    SOLID,
    OUTLINE,
    GHOST
  }

  /// Control size. Padding follows density. The type size does not.
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

  /// Button with custom content. `accessibleName` stays the accessible name.
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
  public Element render() {
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
