package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.UnderlineStyle;

/// Inline text presets. Each returns an element with a semantic tag (`del`, `ins`, `code`, `kbd`)
/// styled with the theme's colors, to place anywhere an inline element fits. For a highlight,
/// underline or strikethrough, style any element directly, such as
/// `mark("new").highlight(Tone.ACCENT)` or `s("old").strikethrough(Tokens.TEXT_MUTED)`.
public final class Formatting {

  private Formatting() {}

  /// Marks removed text: a danger strikethrough on a danger highlight.
  public static TagElement deleted(String text) {
    return Elements.del().strikethrough(Tokens.STATUS_DANGER).highlight(Tone.DANGER).text(text);
  }

  /// Marks added text: a success underline on a success highlight.
  public static TagElement inserted(String text) {
    return Elements.ins()
        .underline(UnderlineStyle.SOLID, Tokens.STATUS_SUCCESS)
        .highlight(Tone.SUCCESS)
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
