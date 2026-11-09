package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.rawHtml;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.css.WhiteSpace;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/// Compact label such as a status or a count. Create one with [#of(String)]; it is a neutral,
/// tinted, medium badge until [#tone(Tone)], [#variant(Variant)] or [#size(Size)] change it. It
/// renders a `<span>`, or an `<a>` when given an [#href(String)].
public final class Badge extends BuiltinView implements FluentBuilder<Badge> {

  /// Fill style: `TINTED` (a light fill, the default), `OUTLINE` (a border only), or `SOLID`.
  public enum Variant {
    TINTED,
    OUTLINE,
    SOLID
  }

  private final String text;
  private Tone tone = Tone.NEUTRAL;
  private Variant variant = Variant.TINTED;
  private Size size = Size.MD;
  private boolean pill;
  private boolean dot;
  private @Nullable String iconSvg;
  private @Nullable String href;

  private Badge(String text) {
    this.text = text;
  }

  /// Creates a neutral, tinted, medium badge showing `text`.
  public static Badge of(String text) {
    return new Badge(text);
  }

  @Override
  public Badge self() {
    return this;
  }

  /// Names this instance in the enclosing view scope. Without it, the compiler assigns
  /// `tessera-badge-<n>`.
  public Badge id(String id) {
    this.id = id;
    return this;
  }

  /// Sets the semantic color. The default is `NEUTRAL`.
  public Badge tone(Tone tone) {
    this.tone = tone;
    return this;
  }

  /// Sets the fill style. The default is `TINTED`.
  public Badge variant(Variant variant) {
    this.variant = variant;
    return this;
  }

  /// Sets the size. The default is `MD`.
  public Badge size(Size size) {
    this.size = size;
    return this;
  }

  /// Gives the badge fully rounded ends.
  public Badge pill() {
    this.pill = true;
    return this;
  }

  /// Adds a status dot in the tone's color before the text.
  public Badge dot() {
    this.dot = true;
    return this;
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

  @Override
  String viewType() {
    return "tessera-badge";
  }

  @Override
  void configureScope(ScopeStyle scope) {
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
                    .transition(
                        Transition.controlFeedback()
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
      var token = tone.color();
      var neutral = tone == Tone.NEUTRAL;
      CssColor tintFill = neutral ? Tokens.SURFACE_SUBTLE : token.mixTransparent(14);
      CssColor tintEdge = neutral ? Tokens.BORDER_SUBTLE : token.mixTransparent(28);
      var outlineEdge = neutral ? Tokens.BORDER_SUBTLE : token;
      var selector = ":scope.badge--" + cssName(tone) + ".badge--";
      scope
          .select(
              selector + cssName(Variant.TINTED),
              s -> s.bg(tintFill).textColor(tone.textColor()).border(Border.hairline(tintEdge)))
          .select(
              selector + cssName(Variant.OUTLINE),
              s ->
                  s.bg(CssColor.transparent())
                      .textColor(tone.textColor())
                      .border(Border.hairline(outlineEdge)))
          .select(
              selector + cssName(Variant.SOLID),
              s ->
                  s.bg(token)
                      .textColor(onSolid(tone))
                      .border(Border.hairline(CssColor.transparent())));
    }

    scope.select("a:scope:hover", s -> s.opacity(0.85));
  }

  private static CssColor onSolid(Tone tone) {
    return switch (tone) {
      case NEUTRAL -> Tokens.SURFACE_CARD;
      case WARNING -> CssColor.oklch(15, 0.02, 75);
      case ACCENT, INFO, SUCCESS, DANGER -> CssColor.white();
    };
  }

  @Override
  IntoElement render(RenderContext context) {
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
