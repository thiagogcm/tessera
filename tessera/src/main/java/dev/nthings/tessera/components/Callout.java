package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.aside;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.rawHtml;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ParentElement;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Admonition rendered as an `<aside role="note">` with a colored icon, a title, and a tinted
/// background. Create one with [#of(Kind, String)] or [#of(Kind, IntoElement...)], and add more
/// content as children. The title defaults to the English name of the [Kind]; [#title(String)]
/// replaces it.
///
/// Without an [#id(String)], the compiler numbers instances `tessera-callout-<n>`.
public final class Callout extends BuiltinScope<Callout> implements ParentElement<Callout> {

  /// Admonition kind, each with its own icon, [Tone] and default title.
  public enum Kind {
    NOTE(
        "Note",
        Tone.INFO,
        "<svg width=\"16\" height=\"16\" viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.75\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><circle cx=\"8\" cy=\"8\" r=\"7\"/><line x1=\"8\" y1=\"8\" x2=\"8\" y2=\"12\"/><line x1=\"8\" y1=\"4.5\" x2=\"8\" y2=\"5\"/></svg>"),
    TIP(
        "Tip",
        Tone.SUCCESS,
        "<svg width=\"16\" height=\"16\" viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.75\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M8 1.5a4.5 4.5 0 0 0-4.5 4.5c0 1.8 1.1 3.4 2.5 4.1v1.9h4v-1.9c1.4-.7 2.5-2.3 2.5-4.1A4.5 4.5 0 0 0 8 1.5z\"/><line x1=\"6\" y1=\"14.5\" x2=\"10\" y2=\"14.5\"/></svg>"),
    IMPORTANT(
        "Important",
        Tone.ACCENT,
        "<svg width=\"16\" height=\"16\" viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.75\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M3 2h10l-2 5 2 5H3z\"/><line x1=\"3\" y1=\"2\" x2=\"3\" y2=\"15\"/></svg>"),
    WARNING(
        "Warning",
        Tone.WARNING,
        "<svg width=\"16\" height=\"16\" viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.75\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M8 1.5l6.5 12H1.5L8 1.5z\"/><line x1=\"8\" y1=\"6\" x2=\"8\" y2=\"9.5\"/><line x1=\"8\" y1=\"11.5\" x2=\"8\" y2=\"12\"/></svg>"),
    CAUTION(
        "Caution",
        Tone.DANGER,
        "<svg width=\"16\" height=\"16\" viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.75\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><polygon points=\"5 1.5 11 1.5 14.5 5 14.5 11 11 14.5 5 14.5 1.5 11 1.5 5 5 1.5\"/><line x1=\"8\" y1=\"5\" x2=\"8\" y2=\"9\"/><line x1=\"8\" y1=\"11\" x2=\"8\" y2=\"11.5\"/></svg>");

    private final String defaultTitle;
    private final Tone tone;
    private final String iconSvg;

    Kind(String defaultTitle, Tone tone, String iconSvg) {
      this.defaultTitle = defaultTitle;
      this.tone = tone;
      this.iconSvg = iconSvg;
    }
  }

  private final Kind kind;
  private @Nullable String customTitle;
  private final List<Element> children = new ArrayList<>();

  private Callout(Kind kind) {
    this.kind = kind;
  }

  /// Creates a callout of `kind` whose body is one paragraph of `text`.
  public static Callout of(Kind kind, String text) {
    return new Callout(kind).child(Elements.p().text(text));
  }

  /// Creates a callout of `kind` whose body is `elements`.
  public static Callout of(Kind kind, IntoElement... elements) {
    return new Callout(kind).children(elements);
  }

  @Override
  public Callout self() {
    return this;
  }

  /// Sets the visible title, replacing the kind's default title.
  public Callout title(String title) {
    this.customTitle = title;
    return this;
  }

  @Override
  public void appendChild(Element child) {
    children.add(child);
  }

  @Override
  String scopeName() {
    return "tessera-callout";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.flex()
                    .flexCol()
                    .gap(Tokens.SPACE_2)
                    .px(Tokens.SPACE_5)
                    .py(Tokens.SPACE_4)
                    .my(Tokens.SPACE_5)
                    .rounded(Tokens.RADIUS_LG)
                    .border(Border.none()))
        .select(
            ":scope > .callout-header",
            s ->
                s.flex()
                    .itemsCenter()
                    .gap(Tokens.SPACE_2)
                    .typography(Roles.LABEL)
                    .textColor(Tokens.CONTROL_FG))
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
            s -> s.fontSize(Tokens.FONT_SIZE_MD).lineHeight(1.6).textColor(Tokens.CONTROL_FG))
        .select(":scope > .callout-content > :first-child", s -> s.mt(CssLength.px(0)))
        .select(":scope > .callout-content > :last-child", s -> s.mb(CssLength.px(0)));
  }

  @Override
  IntoElement expand(DocumentContext context) {
    var displayTitle = customTitle != null ? customTitle : kind.defaultTitle;
    var header =
        div()
            .attr("class", "callout-header")
            .children(
                span()
                    .attr("class", "callout-icon")
                    .textColor(kind.tone.color())
                    .child(rawHtml(kind.iconSvg)),
                span().attr("class", "callout-title").text(displayTitle));

    var content = div().attr("class", "callout-content").children(children);

    var tint = kind.tone.color().mix(CssColor.Space.OKLAB, 14, Tokens.SURFACE_CARD);

    return aside()
        .role(Role.NOTE)
        .attr("class", "callout callout--" + CssNames.of(kind))
        .bg(tint)
        .borderInlineStart(Border.solid(CssLength.px(4), kind.tone.color()))
        .children(header, content);
  }
}
