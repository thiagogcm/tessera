package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.rawHtml;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.FluentBuilder;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CssProperty;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/// Compact badge and pill label component with adaptive OKLCH tints, status dots, and outline/solid
/// variants.
public final class Badge implements RenderView, FluentBuilder<Badge> {

  public enum Tone {
    NEUTRAL(Tokens.TEXT_MUTED),
    ACCENT(Tokens.ACCENT_PRIMARY),
    SUCCESS(Tokens.STATUS_SUCCESS),
    WARNING(Tokens.STATUS_WARNING),
    DANGER(Tokens.STATUS_DANGER),
    INFO(Tokens.STATUS_INFO);

    private final DesignToken token;

    Tone(DesignToken token) {
      this.token = token;
    }

    public DesignToken token() {
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
                    .refine(
                        r ->
                            r.put(CssProperty.BORDER_RADIUS, CssLength.rem(0.375))
                                .put(CssProperty.TEXT_DECORATION_LINE, "none")
                                .put(
                                    CssProperty.TRANSITION,
                                    "background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease, opacity 0.15s ease")))
        .select(":scope.is-pill", s -> s.refine(r -> r.put(CssProperty.BORDER_RADIUS, "9999px")))
        .select(
            ":scope.badge--sm",
            s -> s.fontSize(CssLength.rem(0.7)).px(CssLength.rem(0.4)).py(CssLength.rem(0.15)))
        .select(
            ":scope.badge--md",
            s -> s.fontSize(CssLength.rem(0.75)).px(CssLength.rem(0.55)).py(CssLength.rem(0.225)))
        .select(
            ":scope.badge--lg",
            s -> s.fontSize(CssLength.rem(0.85)).px(CssLength.rem(0.75)).py(CssLength.rem(0.3)))
        .select(
            ":scope > .badge-dot",
            s ->
                s.inlineBlock()
                    .inlineSize(CssLength.rem(0.45))
                    .blockSize(CssLength.rem(0.45))
                    .rounded(CssLength.rem(99))
                    .refine(
                        r ->
                            r.put(CssProperty.BACKGROUND, "currentColor")
                                .put(CssProperty.FLEX_SHRINK, "0")))
        .select(
            ":scope.badge--sm > .badge-dot",
            s -> s.inlineSize(CssLength.rem(0.375)).blockSize(CssLength.rem(0.375)))
        .select(
            ":scope.badge--lg > .badge-dot",
            s -> s.inlineSize(CssLength.rem(0.5)).blockSize(CssLength.rem(0.5)))
        .select(
            ":scope > .badge-icon",
            s -> s.inlineFlex().itemsCenter().refine(r -> r.put(CssProperty.FLEX_SHRINK, "0")))
        .select(
            ":scope.badge--neutral.badge--tinted",
            s ->
                s.bg(Tokens.SURFACE_SUBTLE)
                    .textColor(Tokens.TEXT_MUTED)
                    .borderSubtle(Tokens.BORDER_SUBTLE))
        .select(
            ":scope.badge--neutral.badge--outline",
            s ->
                s.bgRaw("transparent")
                    .textColor(Tokens.TEXT_MUTED)
                    .borderSubtle(Tokens.BORDER_SUBTLE))
        .select(
            ":scope.badge--neutral.badge--solid",
            s ->
                s.bg(Tokens.TEXT_MUTED)
                    .textColor(Tokens.SURFACE_CARD)
                    .refine(r -> r.put(CssProperty.BORDER, "1px solid transparent")))
        .select(
            ":scope.badge--accent.badge--tinted",
            s ->
                s.bgRaw(Tokens.ACCENT_PRIMARY.mixTransparent(14))
                    .textColor(Tokens.ACCENT_PRIMARY)
                    .refine(
                        r ->
                            r.put(
                                CssProperty.BORDER,
                                "1px solid " + Tokens.ACCENT_PRIMARY.mixTransparent(28))))
        .select(
            ":scope.badge--accent.badge--outline",
            s ->
                s.bgRaw("transparent")
                    .textColor(Tokens.ACCENT_PRIMARY)
                    .refine(
                        r ->
                            r.put(
                                CssProperty.BORDER,
                                "1px solid " + Tokens.ACCENT_PRIMARY.varReference())))
        .select(
            ":scope.badge--accent.badge--solid",
            s ->
                s.bg(Tokens.ACCENT_PRIMARY)
                    .textColorRaw("#ffffff")
                    .refine(r -> r.put(CssProperty.BORDER, "1px solid transparent")))
        .select(
            ":scope.badge--success.badge--tinted",
            s ->
                s.bgRaw(Tokens.STATUS_SUCCESS.mixTransparent(14))
                    .textColor(Tokens.STATUS_SUCCESS)
                    .refine(
                        r ->
                            r.put(
                                CssProperty.BORDER,
                                "1px solid " + Tokens.STATUS_SUCCESS.mixTransparent(28))))
        .select(
            ":scope.badge--success.badge--outline",
            s ->
                s.bgRaw("transparent")
                    .textColor(Tokens.STATUS_SUCCESS)
                    .refine(
                        r ->
                            r.put(
                                CssProperty.BORDER,
                                "1px solid " + Tokens.STATUS_SUCCESS.varReference())))
        .select(
            ":scope.badge--success.badge--solid",
            s ->
                s.bg(Tokens.STATUS_SUCCESS)
                    .textColorRaw("#ffffff")
                    .refine(r -> r.put(CssProperty.BORDER, "1px solid transparent")))
        .select(
            ":scope.badge--warning.badge--tinted",
            s ->
                s.bgRaw(Tokens.STATUS_WARNING.mixTransparent(14))
                    .textColor(Tokens.STATUS_WARNING)
                    .refine(
                        r ->
                            r.put(
                                CssProperty.BORDER,
                                "1px solid " + Tokens.STATUS_WARNING.mixTransparent(28))))
        .select(
            ":scope.badge--warning.badge--outline",
            s ->
                s.bgRaw("transparent")
                    .textColor(Tokens.STATUS_WARNING)
                    .refine(
                        r ->
                            r.put(
                                CssProperty.BORDER,
                                "1px solid " + Tokens.STATUS_WARNING.varReference())))
        .select(
            ":scope.badge--warning.badge--solid",
            s ->
                s.bg(Tokens.STATUS_WARNING)
                    .textColorRaw("oklch(15% 0.02 75)")
                    .refine(r -> r.put(CssProperty.BORDER, "1px solid transparent")))
        .select(
            ":scope.badge--danger.badge--tinted",
            s ->
                s.bgRaw(Tokens.STATUS_DANGER.mixTransparent(14))
                    .textColor(Tokens.STATUS_DANGER)
                    .refine(
                        r ->
                            r.put(
                                CssProperty.BORDER,
                                "1px solid " + Tokens.STATUS_DANGER.mixTransparent(28))))
        .select(
            ":scope.badge--danger.badge--outline",
            s ->
                s.bgRaw("transparent")
                    .textColor(Tokens.STATUS_DANGER)
                    .refine(
                        r ->
                            r.put(
                                CssProperty.BORDER,
                                "1px solid " + Tokens.STATUS_DANGER.varReference())))
        .select(
            ":scope.badge--danger.badge--solid",
            s ->
                s.bg(Tokens.STATUS_DANGER)
                    .textColorRaw("#ffffff")
                    .refine(r -> r.put(CssProperty.BORDER, "1px solid transparent")))
        .select(
            ":scope.badge--info.badge--tinted",
            s ->
                s.bgRaw(Tokens.STATUS_INFO.mixTransparent(14))
                    .textColor(Tokens.STATUS_INFO)
                    .refine(
                        r ->
                            r.put(
                                CssProperty.BORDER,
                                "1px solid " + Tokens.STATUS_INFO.mixTransparent(28))))
        .select(
            ":scope.badge--info.badge--outline",
            s ->
                s.bgRaw("transparent")
                    .textColor(Tokens.STATUS_INFO)
                    .refine(
                        r ->
                            r.put(
                                CssProperty.BORDER,
                                "1px solid " + Tokens.STATUS_INFO.varReference())))
        .select(
            ":scope.badge--info.badge--solid",
            s ->
                s.bg(Tokens.STATUS_INFO)
                    .textColorRaw("#ffffff")
                    .refine(r -> r.put(CssProperty.BORDER, "1px solid transparent")))
        .select("a:scope:hover", s -> s.opacity(0.85));
  }

  @Override
  public Element render() {
    var cls =
        new StringBuilder("badge")
            .append(" badge--")
            .append(tone.name().toLowerCase(Locale.ROOT))
            .append(" badge--")
            .append(variant.name().toLowerCase(Locale.ROOT))
            .append(" badge--")
            .append(size.name().toLowerCase(Locale.ROOT));

    if (pill) {
      cls.append(" is-pill");
    }

    var el = (href != null ? a(href) : span()).attr("class", cls.toString());

    if (dot) {
      el.child(span().attr("class", "badge-dot").attr("aria-hidden", "true"));
    }

    if (iconSvg != null && !iconSvg.isBlank()) {
      el.child(
          span().attr("class", "badge-icon").attr("aria-hidden", "true").child(rawHtml(iconSvg)));
    }

    el.child(span().attr("class", "badge-label").text(text));
    return el;
  }
}
