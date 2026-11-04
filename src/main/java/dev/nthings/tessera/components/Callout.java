package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.aside;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.rawHtml;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.FluentBuilder;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/// Editorial callout admonition emitting an accessible `<aside role="note">` tinted with adaptive
/// `color-mix(in oklch, var(--token) 14%, var(--surface-card))` tokens.
public final class Callout implements RenderView, FluentBuilder<Callout> {

  public enum Tone {
    NOTE(
        "Note",
        Tokens.STATUS_INFO,
        "<svg width=\"16\" height=\"16\" viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.75\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><circle cx=\"8\" cy=\"8\" r=\"7\"/><line x1=\"8\" y1=\"8\" x2=\"8\" y2=\"12\"/><line x1=\"8\" y1=\"4.5\" x2=\"8\" y2=\"5\"/></svg>"),
    TIP(
        "Tip",
        Tokens.STATUS_SUCCESS,
        "<svg width=\"16\" height=\"16\" viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.75\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M8 1.5a4.5 4.5 0 0 0-4.5 4.5c0 1.8 1.1 3.4 2.5 4.1v1.9h4v-1.9c1.4-.7 2.5-2.3 2.5-4.1A4.5 4.5 0 0 0 8 1.5z\"/><line x1=\"6\" y1=\"14.5\" x2=\"10\" y2=\"14.5\"/></svg>"),
    IMPORTANT(
        "Important",
        Tokens.ACCENT_PRIMARY,
        "<svg width=\"16\" height=\"16\" viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.75\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M3 2h10l-2 5 2 5H3z\"/><line x1=\"3\" y1=\"2\" x2=\"3\" y2=\"15\"/></svg>"),
    WARNING(
        "Warning",
        Tokens.STATUS_WARNING,
        "<svg width=\"16\" height=\"16\" viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.75\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M8 1.5l6.5 12H1.5L8 1.5z\"/><line x1=\"8\" y1=\"6\" x2=\"8\" y2=\"9.5\"/><line x1=\"8\" y1=\"11.5\" x2=\"8\" y2=\"12\"/></svg>"),
    CAUTION(
        "Caution",
        Tokens.STATUS_DANGER,
        "<svg width=\"16\" height=\"16\" viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.75\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><polygon points=\"5 1.5 11 1.5 14.5 5 14.5 11 11 14.5 5 14.5 1.5 11 1.5 5 5 1.5\"/><line x1=\"8\" y1=\"5\" x2=\"8\" y2=\"9\"/><line x1=\"8\" y1=\"11\" x2=\"8\" y2=\"11.5\"/></svg>");

    private final String defaultTitle;
    private final DesignToken token;
    private final String iconSvg;

    Tone(String defaultTitle, DesignToken token, String iconSvg) {
      this.defaultTitle = defaultTitle;
      this.token = token;
      this.iconSvg = iconSvg;
    }

    public String defaultTitle() {
      return defaultTitle;
    }

    public DesignToken token() {
      return token;
    }

    public String iconSvg() {
      return iconSvg;
    }
  }

  private final Tone tone;
  private @Nullable String customTitle;
  private final List<Element> children = new ArrayList<>();
  private final String instanceId;

  public Callout(Tone tone) {
    this.tone = tone;
    this.instanceId =
        "callout-"
            + tone.name().toLowerCase(Locale.ROOT)
            + "-"
            + Fnv1a64.toBase36Hash(System.nanoTime());
  }

  public static Callout note(String text) {
    return of(Tone.NOTE, text);
  }

  public static Callout note(IntoElement... elements) {
    return of(Tone.NOTE, elements);
  }

  public static Callout tip(String text) {
    return of(Tone.TIP, text);
  }

  public static Callout tip(IntoElement... elements) {
    return of(Tone.TIP, elements);
  }

  public static Callout important(String text) {
    return of(Tone.IMPORTANT, text);
  }

  public static Callout important(IntoElement... elements) {
    return of(Tone.IMPORTANT, elements);
  }

  public static Callout warning(String text) {
    return of(Tone.WARNING, text);
  }

  public static Callout warning(IntoElement... elements) {
    return of(Tone.WARNING, elements);
  }

  public static Callout caution(String text) {
    return of(Tone.CAUTION, text);
  }

  public static Callout caution(IntoElement... elements) {
    return of(Tone.CAUTION, elements);
  }

  public static Callout of(Tone tone, String text) {
    return new Callout(tone).child(Elements.p().text(text));
  }

  public static Callout of(Tone tone, IntoElement... elements) {
    return new Callout(tone).children(elements);
  }

  @Override
  public Callout self() {
    return this;
  }

  public Callout title(String title) {
    this.customTitle = title;
    return this;
  }

  public Callout child(IntoElement child) {
    this.children.add(child.intoElement());
    return this;
  }

  public Callout children(IntoElement... elements) {
    for (var el : elements) {
      this.children.add(el.intoElement());
    }
    return this;
  }

  public Tone tone() {
    return tone;
  }

  @Override
  public String viewType() {
    return "tessera-callout";
  }

  @Override
  public ElementId entityId() {
    return ElementId.of(instanceId);
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.flex()
                    .flexCol()
                    .gap(CssLength.rem(0.5))
                    .px(CssLength.rem(1.25))
                    .py(CssLength.rem(1))
                    .my(CssLength.rem(1.25))
                    .rounded(CssLength.rem(0.5))
                    .borderNone())
        .select(
            ":scope > .callout-header",
            s ->
                s.flex()
                    .itemsCenter()
                    .gap(CssLength.rem(0.5))
                    .fontWeight(600)
                    .fontSize(CssLength.rem(0.95)))
        .select(
            ":scope > .callout-header > .callout-icon",
            s ->
                s.inlineFlex()
                    .itemsCenter()
                    .justifyCenter()
                    .inlineSize(CssLength.rem(1.25))
                    .blockSize(CssLength.rem(1.25)))
        .select(
            ":scope > .callout-content",
            s -> s.fontSize(CssLength.rem(0.95)).lineHeight("1.6").textColor(Tokens.TEXT_PRIMARY))
        .select(":scope > .callout-content > :first-child", s -> s.mt(CssLength.px(0)))
        .select(":scope > .callout-content > :last-child", s -> s.mb(CssLength.px(0)));
  }

  @Override
  public Element render() {
    String displayTitle = customTitle != null ? customTitle : tone.defaultTitle();
    var header =
        div()
            .attr("class", "callout-header")
            .textColor(tone.token())
            .children(
                span().attr("class", "callout-icon").child(rawHtml(tone.iconSvg())),
                span().attr("class", "callout-title").text(displayTitle));

    var content = div().attr("class", "callout-content").children(children);

    String bgValue =
        "color-mix(in oklch, "
            + tone.token().varReference()
            + " 14%, "
            + Tokens.SURFACE_CARD.varReference()
            + ")";

    return aside()
        .role("note")
        .attr("class", "callout callout--" + tone.name().toLowerCase(Locale.ROOT))
        .bgRaw(bgValue)
        .borderInlineStart("4px solid " + tone.token().varReference())
        .children(header, content);
  }
}
