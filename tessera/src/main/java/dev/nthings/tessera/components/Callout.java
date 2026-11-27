package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.aside;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ParentElement;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.Typography;
import dev.nthings.tessera.internal.css.Tint;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Admonition rendered as an `<aside role="note">` with a colored icon, a title, and a tinted
/// background. Create one with [#of(Kind, String)] or [#of(Kind, IntoElement...)], and add more
/// content as children. The title defaults to the English name of the [Kind]; [#title(String)]
/// replaces it.
///
/// Without an [#scopeId(String)], the compiler numbers instances `tessera-callout-<n>`.
public final class Callout extends BuiltinScope<Callout> implements ParentElement<Callout> {

  /// Admonition kind, each with its own icon, [Tone] and default title.
  public enum Kind {
    NOTE(
        "Note",
        Tone.INFO,
        () ->
            Icon.stroked(
                16,
                16,
                1.75,
                Icon.circle(8, 8, 7),
                Icon.line(8, 8, 8, 12),
                Icon.line(8, 4.5, 8, 5))),
    TIP(
        "Tip",
        Tone.SUCCESS,
        () ->
            Icon.stroked(
                16,
                16,
                1.75,
                Icon.path(
                    "M8 1.5a4.5 4.5 0 0 0-4.5 4.5c0 1.8 1.1 3.4 2.5 4.1v1.9h4v-1.9c1.4-.7 2.5-2.3"
                        + " 2.5-4.1A4.5 4.5 0 0 0 8 1.5z"),
                Icon.line(6, 14.5, 10, 14.5))),
    IMPORTANT(
        "Important",
        Tone.ACCENT,
        () -> Icon.stroked(16, 16, 1.75, Icon.path("M3 2h10l-2 5 2 5H3z"), Icon.line(3, 2, 3, 15))),
    WARNING(
        "Warning",
        Tone.WARNING,
        () ->
            Icon.stroked(
                16,
                16,
                1.75,
                Icon.path("M8 1.5l6.5 12H1.5L8 1.5z"),
                Icon.line(8, 6, 8, 9.5),
                Icon.line(8, 11.5, 8, 12))),
    CAUTION(
        "Caution",
        Tone.DANGER,
        () ->
            Icon.stroked(
                16,
                16,
                1.75,
                Icon.polygon("5 1.5 11 1.5 14.5 5 14.5 11 11 14.5 5 14.5 1.5 11 1.5 5 5 1.5"),
                Icon.line(8, 5, 8, 9),
                Icon.line(8, 11, 8, 11.5)));

    private final String defaultTitle;
    private final Tone tone;
    private final Supplier<TagElement> icon;

    Kind(String defaultTitle, Tone tone, Supplier<TagElement> icon) {
      this.defaultTitle = defaultTitle;
      this.tone = tone;
      this.icon = icon;
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
  public Callout child(IntoElement child) {
    children.add(child.intoElement());
    return this;
  }

  @Override
  String scopeName() {
    return "tessera-callout";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s ->
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
                    .typography(Typography.LABEL)
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
  TagElement expand(ViewContext context) {
    var displayTitle = customTitle != null ? customTitle : kind.defaultTitle;
    var header =
        div()
            .attr("class", "callout-header")
            .children(
                span()
                    .attr("class", "callout-icon")
                    .textColor(kind.tone.color())
                    .child(kind.icon.get()),
                span().attr("class", "callout-title").text(displayTitle));

    var content = div().attr("class", "callout-content").children(children);

    return aside()
        .role(Role.NOTE)
        .attr("class", "callout callout--" + CssNames.of(kind))
        .bg(Tint.CALLOUT.fill(kind.tone))
        .borderInlineStart(Border.solid(CssLength.px(4), kind.tone.color()))
        .children(header, content);
  }
}
