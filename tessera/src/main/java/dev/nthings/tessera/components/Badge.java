package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.rawHtml;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.WhiteSpace;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/// Compact label such as a status or a count. Create one with `Badge.of` or a tone factory such as
/// `Badge.success`. It renders a `<span>`, or an `<a>` when given an [#href(String)].
public final class Badge implements View, FluentBuilder<Badge> {

  /// Semantic color of a badge. `NEUTRAL` is the default.
  public enum Tone {
    NEUTRAL(Tokens.TEXT_MUTED, Tokens.SURFACE_CARD, Tokens.TEXT_MUTED),
    ACCENT(Tokens.ACCENT_PRIMARY, CssColor.white(), Tokens.ACCENT_TEXT),
    SUCCESS(Tokens.STATUS_SUCCESS, CssColor.white(), Tokens.STATUS_SUCCESS_TEXT),
    WARNING(Tokens.STATUS_WARNING, CssColor.oklch(15, 0.02, 75), Tokens.STATUS_WARNING_TEXT),
    DANGER(Tokens.STATUS_DANGER, CssColor.white(), Tokens.STATUS_DANGER_TEXT),
    INFO(Tokens.STATUS_INFO, CssColor.white(), Tokens.STATUS_INFO_TEXT);

    private final DesignToken.Color token;
    private final CssColor onSolid;
    private final DesignToken.Color onTint;

    Tone(DesignToken.Color token, CssColor onSolid, DesignToken.Color onTint) {
      this.token = token;
      this.onSolid = onSolid;
      this.onTint = onTint;
    }

    /// Compiler-facing: the semantic color token this tone paints.
    public DesignToken.Color token() {
      return token;
    }
  }

  /// Fill style: `TINTED` (a light fill, the default), `OUTLINE` (a border only), or `SOLID`.
  public enum Variant {
    TINTED,
    OUTLINE,
    SOLID
  }

  /// Size: `SM`, `MD` (the default), or `LG`.
  public enum Size {
    SM,
    MD,
    LG
  }

  private final String text;
  private Tone tone = Tone.NEUTRAL;
  private Variant variant = Variant.TINTED;
  private Size size = Size.MD;
  private boolean pill = false;
  private boolean dot = false;
  private @Nullable String iconSvg;
  private @Nullable String href;
  private @Nullable String viewId;

  private Badge(String text) {
    this.text = text;
  }

  /// Creates a neutral tinted badge with `text`.
  public static Badge of(String text) {
    return new Badge(text);
  }

  /// Creates a badge with fully rounded corners.
  public static Badge pill(String text) {
    return new Badge(text).pill();
  }

  public static Badge neutral(String text) {
    return new Badge(text).tone(Tone.NEUTRAL);
  }

  public static Badge accent(String text) {
    return new Badge(text).tone(Tone.ACCENT);
  }

  public static Badge success(String text) {
    return new Badge(text).tone(Tone.SUCCESS);
  }

  public static Badge warning(String text) {
    return new Badge(text).tone(Tone.WARNING);
  }

  public static Badge danger(String text) {
    return new Badge(text).tone(Tone.DANGER);
  }

  public static Badge info(String text) {
    return new Badge(text).tone(Tone.INFO);
  }

  /// Creates a tone-colored badge with a status dot.
  public static Badge dot(String text, Tone tone) {
    return new Badge(text).tone(tone).dot();
  }

  @Override
  public Badge self() {
    return this;
  }

  @Override
  public @Nullable String viewId() {
    return viewId;
  }

  /// Sets the instance id in the enclosing view scope. The badge emits no DOM ids of its own.
  public Badge id(String id) {
    this.viewId = id;
    return this;
  }

  public Badge tone(Tone tone) {
    this.tone = tone;
    return this;
  }

  public Badge neutral() {
    return tone(Tone.NEUTRAL);
  }

  public Badge accent() {
    return tone(Tone.ACCENT);
  }

  public Badge success() {
    return tone(Tone.SUCCESS);
  }

  public Badge warning() {
    return tone(Tone.WARNING);
  }

  public Badge danger() {
    return tone(Tone.DANGER);
  }

  public Badge info() {
    return tone(Tone.INFO);
  }

  public Badge variant(Variant variant) {
    this.variant = variant;
    return this;
  }

  public Badge tinted() {
    return variant(Variant.TINTED);
  }

  public Badge outline() {
    return variant(Variant.OUTLINE);
  }

  public Badge solid() {
    return variant(Variant.SOLID);
  }

  public Badge size(Size size) {
    this.size = size;
    return this;
  }

  public Badge sm() {
    return size(Size.SM);
  }

  public Badge md() {
    return size(Size.MD);
  }

  public Badge lg() {
    return size(Size.LG);
  }

  public Badge pill(boolean isPill) {
    this.pill = isPill;
    return this;
  }

  public Badge pill() {
    return pill(true);
  }

  public Badge dot(boolean hasDot) {
    this.dot = hasDot;
    return this;
  }

  public Badge dot() {
    return dot(true);
  }

  /// Adds an icon from trusted SVG markup, inserted unescaped in an `aria-hidden` span. Never pass
  /// user input.
  public Badge icon(String iconSvg) {
    this.iconSvg = iconSvg;
    return this;
  }

  /// Renders the badge as an `<a href>` link instead of a `<span>`.
  public Badge href(String href) {
    this.href = href;
    return this;
  }

  public Tone tone() {
    return tone;
  }

  public Variant variant() {
    return variant;
  }

  public Size size() {
    return size;
  }

  public boolean isPill() {
    return pill;
  }

  public boolean hasDot() {
    return dot;
  }

  @Override
  public String viewType() {
    return "tessera-badge";
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.inlineFlex()
                    .itemsCenter()
                    .justifyCenter()
                    .gap(CssLength.rem(0.35))
                    .fontFamilyInherit()
                    .fontWeight(500)
                    .lineHeight(1)
                    .whiteSpace(WhiteSpace.NOWRAP)
                    .rounded(Tokens.RADIUS_MD)
                    .textDecorationNone()
                    .motion(
                        Motion.controlFeedback()
                            .properties(
                                Animatable.BACKGROUND_COLOR,
                                Animatable.COLOR,
                                Animatable.BORDER_COLOR,
                                Animatable.OPACITY)))
        .select(":scope.is-pill", s -> s.rounded(Tokens.RADIUS_FULL))
        .select(
            ":scope.badge--sm",
            s -> s.fontSize(Tokens.FONT_SIZE_XS).px(CssLength.rem(0.4)).py(CssLength.rem(0.15)))
        .select(
            ":scope.badge--md",
            s -> s.fontSize(Tokens.FONT_SIZE_XS).px(CssLength.rem(0.55)).py(CssLength.rem(0.225)))
        .select(
            ":scope.badge--lg",
            s -> s.fontSize(Tokens.FONT_SIZE_SM).px(Tokens.SPACE_3).py(CssLength.rem(0.3)))
        .select(
            ":scope > .badge-dot",
            s ->
                s.inlineBlock()
                    .inlineSize(CssLength.rem(0.45))
                    .blockSize(CssLength.rem(0.45))
                    .rounded(Tokens.RADIUS_FULL)
                    .bg(CssColor.currentColor())
                    .flexShrink(0))
        .select(
            ":scope.badge--sm > .badge-dot",
            s -> s.inlineSize(CssLength.rem(0.375)).blockSize(CssLength.rem(0.375)))
        .select(
            ":scope.badge--lg > .badge-dot",
            s -> s.inlineSize(CssLength.rem(0.5)).blockSize(CssLength.rem(0.5)))
        .select(":scope > .badge-icon", s -> s.inlineFlex().itemsCenter().flexShrink(0));

    for (var tone : Tone.values()) {
      var token = tone.token;
      var neutral = tone == Tone.NEUTRAL;
      CssColor tintFill = neutral ? Tokens.SURFACE_SUBTLE : token.mixTransparent(14);
      CssColor tintEdge = neutral ? Tokens.BORDER_SUBTLE : token.mixTransparent(28);
      var outlineEdge = neutral ? Tokens.BORDER_SUBTLE : token;
      var selector = ":scope.badge--" + cssName(tone) + ".badge--";
      scope
          .select(
              selector + cssName(Variant.TINTED),
              s -> s.bg(tintFill).textColor(tone.onTint).borderSubtle(tintEdge))
          .select(
              selector + cssName(Variant.OUTLINE),
              s -> s.bg(CssColor.transparent()).textColor(tone.onTint).borderSubtle(outlineEdge))
          .select(
              selector + cssName(Variant.SOLID),
              s -> s.bg(token).textColor(tone.onSolid).borderSubtle(CssColor.transparent()));
    }

    scope.select("a:scope:hover", s -> s.opacity(0.85));
  }

  @Override
  public Element render(RenderContext context) {
    var cls =
        "badge badge--"
            + cssName(tone)
            + " badge--"
            + cssName(variant)
            + " badge--"
            + cssName(size)
            + (pill ? " is-pill" : "");

    var el = (href != null ? a(href) : span()).attr("class", cls);

    if (dot) {
      el.child(span().attr("class", "badge-dot").ariaHidden(true));
    }

    if (iconSvg != null) {
      el.child(span().attr("class", "badge-icon").ariaHidden(true).child(rawHtml(iconSvg)));
    }

    el.child(span().attr("class", "badge-label").text(text));
    return el;
  }

  private static String cssName(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }
}
