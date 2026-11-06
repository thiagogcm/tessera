package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.rawHtml;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/// Compact badge and pill label component with adaptive OKLCH tints, status dots, and outline/solid
/// variants.
public final class Badge implements RenderView, FluentBuilder<Badge> {

  public enum Tone {
    NEUTRAL(Tokens.TEXT_MUTED, Tokens.SURFACE_CARD.varReference()),
    ACCENT(Tokens.ACCENT_PRIMARY, "#ffffff"),
    SUCCESS(Tokens.STATUS_SUCCESS, "#ffffff"),
    WARNING(Tokens.STATUS_WARNING, "oklch(15% 0.02 75)"),
    DANGER(Tokens.STATUS_DANGER, "#ffffff"),
    INFO(Tokens.STATUS_INFO, "#ffffff");

    private final DesignToken.Color token;
    private final String onSolid;

    Tone(DesignToken.Color token, String onSolid) {
      this.token = token;
      this.onSolid = onSolid;
    }

    public DesignToken.Color token() {
      return token;
    }
  }

  public enum Variant {
    TINTED,
    OUTLINE,
    SOLID
  }

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
  private @Nullable ElementId entityId;

  private Badge(String text) {
    this.text = text;
  }

  public static Badge of(String text) {
    return new Badge(text);
  }

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

  public static Badge dot(String text, Tone tone) {
    return new Badge(text).tone(tone).dot();
  }

  @Override
  public Badge self() {
    return this;
  }

  @Override
  public @Nullable ElementId entityId() {
    return entityId;
  }

  public Badge id(String id) {
    this.entityId = ElementId.of(id);
    return this;
  }

  public Badge id(ElementId id) {
    this.entityId = id;
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

  public Badge icon(String iconSvg) {
    this.iconSvg = iconSvg;
    return this;
  }

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
                    .fontFamily("inherit")
                    .fontWeight(500)
                    .lineHeight("1")
                    .whiteSpace("nowrap")
                    .rounded(Tokens.RADIUS_MD)
                    .textDecorationNone()
                    .transition(
                        "background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease, opacity 0.15s ease"))
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
                    .bgRaw("currentColor")
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
      var tintFill = neutral ? Tokens.SURFACE_SUBTLE.varReference() : token.mixTransparent(14);
      var tintEdge = neutral ? Tokens.BORDER_SUBTLE.varReference() : token.mixTransparent(28);
      var outlineEdge = (neutral ? Tokens.BORDER_SUBTLE : token).varReference();
      var selector = ":scope.badge--" + cssName(tone) + ".badge--";
      scope
          .select(
              selector + cssName(Variant.TINTED),
              s -> s.bgRaw(tintFill).textColor(token).border("1px solid " + tintEdge))
          .select(
              selector + cssName(Variant.OUTLINE),
              s -> s.bgRaw("transparent").textColor(token).border("1px solid " + outlineEdge))
          .select(
              selector + cssName(Variant.SOLID),
              s -> s.bg(token).textColorRaw(tone.onSolid).border("1px solid transparent"));
    }

    scope.select("a:scope:hover", s -> s.opacity(0.85));
  }

  @Override
  public Element render() {
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

    if (iconSvg != null && !iconSvg.isBlank()) {
      el.child(span().attr("class", "badge-icon").ariaHidden(true).child(rawHtml(iconSvg)));
    }

    el.child(span().attr("class", "badge-label").text(text));
    return el;
  }

  private static String cssName(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }
}
