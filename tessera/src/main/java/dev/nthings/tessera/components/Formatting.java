package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.UnderlineStyle;

/// Inline text formatting. Each method returns an element with a semantic tag (`mark`, `u`, `s`,
/// `del`, `ins`, `code`, `kbd`) styled with the theme's colors. The results can be placed
/// anywhere an inline element fits.
public final class Formatting {

  private Formatting() {}

  /// Highlights `text` with the warning tone.
  public static TagElement highlight(String text) {
    return highlight(text, HighlightTone.WARNING);
  }

  public static TagElement highlight(String text, HighlightTone tone) {
    return highlight(Elements.text(text), tone);
  }

  /// Highlights `content`, which may contain markup.
  public static TagElement highlight(IntoElement content, HighlightTone tone) {
    return Elements.mark().highlight(tone).textColor(Tokens.TEXT_PRIMARY).child(content);
  }

  /// Underlines `text` with a solid accent line.
  public static TagElement underline(String text) {
    return underline(text, UnderlineStyle.SOLID, Tokens.ACCENT_PRIMARY);
  }

  public static TagElement underline(String text, UnderlineStyle style) {
    return underline(text, style, Tokens.ACCENT_PRIMARY);
  }

  public static TagElement underline(
      String text, UnderlineStyle style, DesignToken.Color decorationColor) {
    return underline(Elements.text(text), style, decorationColor);
  }

  /// Underlines `content` in `style` and `decorationColor`. The content may contain markup.
  public static TagElement underline(
      IntoElement content, UnderlineStyle style, DesignToken.Color decorationColor) {
    return Elements.u().underline(style, decorationColor).child(content);
  }

  /// Strikes through `text` in a muted line color.
  public static TagElement strikethrough(String text) {
    return strikethrough(Elements.text(text), Tokens.TEXT_MUTED);
  }

  /// Strikes through `content` in `decorationColor`. The content may contain markup.
  public static TagElement strikethrough(IntoElement content, DesignToken.Color decorationColor) {
    return Elements.s().strikethrough(decorationColor).child(content);
  }

  /// Marks removed text: a danger strikethrough on a danger highlight.
  public static TagElement deleted(String text) {
    return Elements.del()
        .strikethrough(Tokens.STATUS_DANGER)
        .highlight(HighlightTone.DANGER)
        .text(text);
  }

  /// Marks added text: a success underline on a success highlight.
  public static TagElement inserted(String text) {
    return Elements.ins()
        .underline(UnderlineStyle.SOLID, Tokens.STATUS_SUCCESS)
        .highlight(HighlightTone.SUCCESS)
        .text(text);
  }

  /// Inline code: `codeText` on a subtle surface, at 0.9 times the surrounding font size.
  public static TagElement inlineCode(String codeText) {
    return Elements.code()
        .bg(Tokens.SURFACE_SUBTLE)
        .px(CssLength.rem(0.35))
        .py(CssLength.rem(0.1))
        .rounded(Tokens.RADIUS_SM)
        .fontSize(CssLength.em(0.9))
        .text(codeText);
  }

  /// Keyboard key or shortcut, such as `Ctrl+K`, drawn as a key cap.
  public static TagElement keybinding(String keys) {
    return Elements.kbd()
        .bg(Tokens.SURFACE_SUBTLE)
        .border(Border.hairline(Tokens.BORDER_SUBTLE))
        .px(CssLength.rem(0.4))
        .py(CssLength.rem(0.1))
        .rounded(Tokens.RADIUS_SM)
        .fontSize(CssLength.em(0.85))
        .shadow(Shadow.drop(CssLength.px(1), CssLength.zero(), Tokens.BORDER_SUBTLE))
        .text(keys);
  }
}
