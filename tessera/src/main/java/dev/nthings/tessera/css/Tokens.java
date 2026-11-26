package dev.nthings.tessera.css;

import java.util.List;

/// The built-in design tokens. Components and authoring code reference them as values (each is
/// `var(--name)`), and [Theme] sets their values: `Theme.defaults().with(Tokens.DENSITY,
/// CssNumber.of(0.9))`. Spacing tokens scale with [#DENSITY]; the `CONTROL_*` tokens follow the
/// body tokens until a theme overrides them.
public final class Tokens {

  public static final DesignToken.FontFamily FONT_BODY =
      new DesignToken.FontFamily(
          "font-body", FontStack.of(FontStack.Generic.SYSTEM_UI, FontStack.Generic.SANS_SERIF));
  public static final DesignToken.FontFamily FONT_MONO =
      new DesignToken.FontFamily(
          "font-mono",
          FontStack.of(
              FontStack.Generic.UI_MONOSPACE,
              FontStack.named("SFMono-Regular"),
              FontStack.named("Consolas"),
              FontStack.Generic.MONOSPACE));
  public static final DesignToken.Length FONT_SIZE_XS = length("font-size-xs", 0.75);
  public static final DesignToken.Length FONT_SIZE_SM = length("font-size-sm", 0.875);
  public static final DesignToken.Length FONT_SIZE_MD = length("font-size-md", 1);
  public static final DesignToken.Length FONT_SIZE_LG = length("font-size-lg", 1.125);
  public static final DesignToken.Length FONT_SIZE_XL = length("font-size-xl", 1.25);
  public static final DesignToken.Length FONT_SIZE_2XL = length("font-size-2xl", 1.5);
  public static final DesignToken.Number LINE_HEIGHT_BODY = number("line-height-body", 1.5);
  public static final DesignToken.Length TRACKING_NORMAL =
      new DesignToken.Length("tracking-normal", CssLength.zero());
  public static final DesignToken.Number DENSITY = number("density", 1);
  public static final DesignToken.Length CONTROL_PADDING_INLINE =
      space("control-padding-inline", 0.875);
  public static final DesignToken.Length SPACE_0_5 = space("space-0-5", 0.125);
  public static final DesignToken.Length SPACE_1_5 = space("space-1-5", 0.375);
  public static final DesignToken.Length SPACE_2_5 = space("space-2-5", 0.625);
  public static final DesignToken.Length SPACE_3_5 = space("space-3-5", 0.875);
  public static final DesignToken.Length SPACE_1 = space("space-1", 0.25);
  public static final DesignToken.Length SPACE_2 = space("space-2", 0.5);
  public static final DesignToken.Length SPACE_3 = space("space-3", 0.75);
  public static final DesignToken.Length SPACE_4 = space("space-4", 1);
  public static final DesignToken.Length SPACE_5 = space("space-5", 1.25);
  public static final DesignToken.Length SPACE_6 = space("space-6", 1.5);
  public static final DesignToken.Length SPACE_8 = space("space-8", 2);
  public static final DesignToken.Length SPACE_10 = space("space-10", 2.5);
  public static final DesignToken.Length SPACE_12 = space("space-12", 3);
  public static final DesignToken.Length RADIUS_SM = length("radius-sm", 0.25);
  public static final DesignToken.Length RADIUS_MD = length("radius-md", 0.375);
  public static final DesignToken.Length RADIUS_LG = length("radius-lg", 0.5);
  public static final DesignToken.Length RADIUS_XL = length("radius-xl", 0.75);
  public static final DesignToken.Length RADIUS_FULL =
      new DesignToken.Length("radius-full", CssLength.px(9999));

  public static final DesignToken.FontFamily CONTROL_FAMILY =
      new DesignToken.FontFamily("control-family", FontStack.of(FONT_BODY));
  public static final DesignToken.Length CONTROL_FONT_SIZE =
      new DesignToken.Length("control-font-size", FONT_SIZE_SM);
  public static final DesignToken.Number CONTROL_LINE_HEIGHT =
      new DesignToken.Number("control-line-height", LINE_HEIGHT_BODY);
  public static final DesignToken.Length CONTROL_TRACKING =
      new DesignToken.Length("control-tracking", TRACKING_NORMAL);
  public static final DesignToken.Duration CONTROL_FEEDBACK_DURATION =
      new DesignToken.Duration("control-feedback-duration", CssDuration.ms(150));
  public static final DesignToken.Duration OVERLAY_DURATION =
      new DesignToken.Duration("overlay-duration", CssDuration.ms(200));
  public static final DesignToken.Duration DISCLOSURE_DURATION =
      new DesignToken.Duration("disclosure-duration", CssDuration.ms(250));

  public static final DesignToken.Color SURFACE_CANVAS =
      adaptive("surface-canvas", 99, 0.005, 260, 14, 0.015, 260);
  public static final DesignToken.Color SURFACE_CARD =
      adaptive("surface-card", 100, 0, 0, 19, 0.02, 260);
  public static final DesignToken.Color SURFACE_SUBTLE =
      adaptive("surface-subtle", 95, 0.01, 260, 24, 0.02, 260);
  public static final DesignToken.Color TEXT_PRIMARY =
      adaptive("text-primary", 18, 0.02, 260, 96, 0.01, 260);
  public static final DesignToken.Color TEXT_MUTED =
      adaptive("text-muted", 45, 0.02, 260, 72, 0.02, 260);
  public static final DesignToken.Color ACCENT_PRIMARY =
      adaptive("accent-primary", 54, 0.21, 262, 68, 0.19, 262);

  /// Accent safe for small text on tinted or canvas surfaces.
  public static final DesignToken.Color ACCENT_TEXT =
      tintText("accent-text", ACCENT_PRIMARY, 38, 0.16, 84, 0.12);

  public static final DesignToken.Color BORDER_SUBTLE =
      adaptive("border-subtle", 88, 0.01, 260, 30, 0.02, 260);
  public static final DesignToken.Color STATUS_SUCCESS =
      adaptive("status-success", 52, 0.17, 152, 72, 0.16, 152);
  public static final DesignToken.Color STATUS_WARNING =
      adaptive("status-warning", 68, 0.17, 75, 80, 0.16, 75);
  public static final DesignToken.Color STATUS_DANGER =
      adaptive("status-danger", 56, 0.21, 25, 70, 0.19, 25);
  public static final DesignToken.Color STATUS_INFO =
      adaptive("status-info", 52, 0.16, 235, 74, 0.14, 235);

  /// Status tones safe for small text on tinted or canvas surfaces.
  public static final DesignToken.Color STATUS_SUCCESS_TEXT =
      tintText("status-success-text", STATUS_SUCCESS, 38, 0.14, 84, 0.12);

  public static final DesignToken.Color STATUS_WARNING_TEXT =
      tintText("status-warning-text", STATUS_WARNING, 35, 0.12, 88, 0.12);
  public static final DesignToken.Color STATUS_DANGER_TEXT =
      tintText("status-danger-text", STATUS_DANGER, 38, 0.18, 84, 0.12);
  public static final DesignToken.Color STATUS_INFO_TEXT =
      tintText("status-info-text", STATUS_INFO, 38, 0.14, 84, 0.12);
  public static final DesignToken.Color CONTROL_FG =
      new DesignToken.Color("control-fg", TEXT_PRIMARY);

  /// Syntax colors of highlighted `CodeBlock` source, each readable on cards and highlighted lines.
  public static final DesignToken.Color SYNTAX_COMMENT =
      adaptive("syntax-comment", 45, 0.02, 260, 72, 0.02, 260);

  public static final DesignToken.Color SYNTAX_KEYWORD =
      adaptive("syntax-keyword", 50, 0.19, 25, 74, 0.14, 25);
  public static final DesignToken.Color SYNTAX_STRING =
      adaptive("syntax-string", 40, 0.12, 255, 82, 0.08, 240);
  public static final DesignToken.Color SYNTAX_CONSTANT =
      adaptive("syntax-constant", 48, 0.16, 255, 78, 0.11, 250);
  public static final DesignToken.Color SYNTAX_TYPE =
      adaptive("syntax-type", 48, 0.13, 55, 82, 0.12, 65);
  public static final DesignToken.Color SYNTAX_FUNCTION =
      adaptive("syntax-function", 48, 0.19, 300, 78, 0.12, 300);
  public static final DesignToken.Color SYNTAX_PROPERTY =
      adaptive("syntax-property", 46, 0.11, 215, 80, 0.09, 210);
  public static final DesignToken.Color SYNTAX_TAG =
      adaptive("syntax-tag", 46, 0.13, 150, 80, 0.14, 150);
  public static final DesignToken.Color SYNTAX_META =
      adaptive("syntax-meta", 48, 0.15, 345, 78, 0.12, 345);

  /// Every built-in token, in the order [Theme#defaults()] emits them.
  static final List<DesignToken<?>> ALL =
      List.of(
          FONT_BODY,
          FONT_MONO,
          FONT_SIZE_XS,
          FONT_SIZE_SM,
          FONT_SIZE_MD,
          FONT_SIZE_LG,
          FONT_SIZE_XL,
          FONT_SIZE_2XL,
          LINE_HEIGHT_BODY,
          TRACKING_NORMAL,
          DENSITY,
          CONTROL_PADDING_INLINE,
          SPACE_0_5,
          SPACE_1_5,
          SPACE_2_5,
          SPACE_3_5,
          SPACE_1,
          SPACE_2,
          SPACE_3,
          SPACE_4,
          SPACE_5,
          SPACE_6,
          SPACE_8,
          SPACE_10,
          SPACE_12,
          RADIUS_SM,
          RADIUS_MD,
          RADIUS_LG,
          RADIUS_XL,
          RADIUS_FULL,
          CONTROL_FAMILY,
          CONTROL_FONT_SIZE,
          CONTROL_LINE_HEIGHT,
          CONTROL_TRACKING,
          CONTROL_FEEDBACK_DURATION,
          OVERLAY_DURATION,
          DISCLOSURE_DURATION,
          SURFACE_CANVAS,
          SURFACE_CARD,
          SURFACE_SUBTLE,
          TEXT_PRIMARY,
          TEXT_MUTED,
          ACCENT_PRIMARY,
          ACCENT_TEXT,
          BORDER_SUBTLE,
          STATUS_SUCCESS,
          STATUS_SUCCESS_TEXT,
          STATUS_WARNING,
          STATUS_WARNING_TEXT,
          STATUS_DANGER,
          STATUS_DANGER_TEXT,
          STATUS_INFO,
          STATUS_INFO_TEXT,
          CONTROL_FG,
          SYNTAX_COMMENT,
          SYNTAX_KEYWORD,
          SYNTAX_STRING,
          SYNTAX_CONSTANT,
          SYNTAX_TYPE,
          SYNTAX_FUNCTION,
          SYNTAX_PROPERTY,
          SYNTAX_TAG,
          SYNTAX_META);

  /// The syntax colors of highlighted code.
  static final List<DesignToken.Color> SYNTAX =
      List.of(
          SYNTAX_COMMENT,
          SYNTAX_KEYWORD,
          SYNTAX_STRING,
          SYNTAX_CONSTANT,
          SYNTAX_TYPE,
          SYNTAX_FUNCTION,
          SYNTAX_PROPERTY,
          SYNTAX_TAG,
          SYNTAX_META);

  private Tokens() {}

  private static DesignToken.Number number(String name, double value) {
    return new DesignToken.Number(name, CssNumber.of(value));
  }

  private static DesignToken.Length length(String name, double rem) {
    return new DesignToken.Length(name, CssLength.rem(rem));
  }

  /// A spacing length that scales with [#DENSITY].
  private static DesignToken.Length space(String name, double rem) {
    return new DesignToken.Length(name, CssLength.rem(rem).times(DENSITY));
  }

  /// A scheme-aware text color derived from `source` at readable light/dark lightness and chroma.
  private static DesignToken.Color tintText(
      String name,
      DesignToken.Color source,
      double lightLightness,
      double lightChroma,
      double darkLightness,
      double darkChroma) {
    return new DesignToken.Color(
        name,
        CssColor.lightDark(
            CssColor.sameHue(source, lightLightness, lightChroma),
            CssColor.sameHue(source, darkLightness, darkChroma)));
  }

  private static DesignToken.Color adaptive(
      String name,
      double lightLightness,
      double lightChroma,
      double lightHue,
      double darkLightness,
      double darkChroma,
      double darkHue) {
    return new DesignToken.Color(
        name,
        CssColor.lightDark(
            CssColor.oklch(lightLightness, lightChroma, lightHue),
            CssColor.oklch(darkLightness, darkChroma, darkHue)));
  }
}
