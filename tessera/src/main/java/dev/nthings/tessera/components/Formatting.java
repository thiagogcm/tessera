package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.UnderlineStyle;

/// Semantic HTML5 inline text formatting primitives backed by CSS Text Decoration Level 4 and
/// adaptive `color-mix(in oklch, ...)` tokens.
public final class Formatting {

  private Formatting() {}

  public static TagElement highlight(String text) {
    return highlight(text, HighlightTone.WARNING);
  }

  public static TagElement highlight(String text, HighlightTone tone) {
    return highlight(Elements.text(text), tone);
  }

  public static TagElement highlight(IntoElement content, HighlightTone tone) {
    return Elements.mark().highlight(tone).textColor(Tokens.TEXT_PRIMARY).child(content);
  }

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

  public static TagElement underline(
      IntoElement content, UnderlineStyle style, DesignToken.Color decorationColor) {
    return Elements.u().underline(style, decorationColor).child(content);
  }

  public static TagElement strikethrough(String text) {
    return strikethrough(Elements.text(text), Tokens.TEXT_MUTED);
  }

  public static TagElement strikethrough(IntoElement content, DesignToken.Color decorationColor) {
    return Elements.s().strikethrough(decorationColor).child(content);
  }

  public static TagElement deleted(String text) {
    return Elements.del()
        .strikethrough(Tokens.STATUS_DANGER)
        .highlight(HighlightTone.DANGER)
        .text(text);
  }

  public static TagElement inserted(String text) {
    return Elements.ins()
        .underline(UnderlineStyle.SOLID, Tokens.STATUS_SUCCESS)
        .highlight(HighlightTone.SUCCESS)
        .text(text);
  }

  public static TagElement inlineCode(String codeText) {
    return Elements.code()
        .bg(Tokens.SURFACE_SUBTLE)
        .px(CssLength.rem(0.35))
        .py(CssLength.rem(0.1))
        .rounded(Tokens.RADIUS_SM.length())
        .fontSize(CssLength.em(0.9))
        .text(codeText);
  }

  public static TagElement keybinding(String keys) {
    return Elements.kbd()
        .bg(Tokens.SURFACE_SUBTLE)
        .borderSubtle(Tokens.BORDER_SUBTLE)
        .px(CssLength.rem(0.4))
        .py(CssLength.rem(0.1))
        .rounded(Tokens.RADIUS_SM.length())
        .fontSize(CssLength.em(0.85))
        .shadow("0 1px 0 " + Tokens.BORDER_SUBTLE.varReference())
        .text(keys);
  }
}
