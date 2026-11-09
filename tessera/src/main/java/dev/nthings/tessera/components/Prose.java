package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ParentElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;

/// Container for long-form text. It spaces paragraphs, lists, headings, quotes and rules, and
/// limits the line length to 75ch unless [#maxMeasure(CssLength)] changes it. Create one with
/// [#of(IntoElement...)] and add more content as children. Paragraphs with a class attribute keep
/// their own styling.
///
/// Without an [#id(String)], the compiler numbers instances `prose-container-<n>`.
public final class Prose extends BuiltinView<Prose> implements ParentElement<Prose> {

  private final List<Element> children = new ArrayList<>();
  private CssLength maxMeasure = CssLength.ch(75);

  private Prose() {}

  /// Creates a prose container holding `content`.
  public static Prose of(IntoElement... content) {
    return new Prose().children(content);
  }

  @Override
  public Prose self() {
    return this;
  }

  @Override
  public void appendChild(Element child) {
    children.add(child);
  }

  /// Sets the maximum line length of the container. The default is 75ch.
  public Prose maxMeasure(CssLength measure) {
    this.maxMeasure = measure;
    return this;
  }

  @Override
  String viewType() {
    return "prose-container";
  }

  @Override
  IntoElement render(RenderContext context) {
    var container = div().attr("class", "prose-canvas").flex().flexCol().gap(Tokens.SPACE_10);
    container.children(children);
    return container;
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.block()
                    .inlineSize(CssLength.pct(100))
                    .maxInlineSize(maxMeasure)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .lineHeight(1.75))
        .select(
            ":scope p:not([class])",
            s ->
                s.mb(CssLength.rem(1.125))
                    .fontSize(Tokens.FONT_SIZE_MD)
                    .textColor(Tokens.TEXT_PRIMARY)
                    .lineHeight(1.75))
        .select(
            ":scope > h2",
            s ->
                s.fontSize(Tokens.FONT_SIZE_2XL)
                    .fontWeight(700)
                    .mt(CssLength.rem(2.25))
                    .mb(Tokens.SPACE_3)
                    .lineHeight(1.3))
        .select(
            ":scope > h3",
            s ->
                s.fontSize(Tokens.FONT_SIZE_XL)
                    .fontWeight(600)
                    .mt(CssLength.rem(1.75))
                    .mb(Tokens.SPACE_2)
                    .lineHeight(1.4))
        .select(
            ":scope > ul, :scope > ol",
            s -> s.ps(Tokens.SPACE_6).mb(Tokens.SPACE_5).lineHeight(1.75))
        .select(
            ":scope > blockquote",
            s ->
                s.borderInlineStart(Border.solid(CssLength.px(3), Tokens.ACCENT_PRIMARY))
                    .ps(Tokens.SPACE_4)
                    .py(Tokens.SPACE_1)
                    .my(Tokens.SPACE_6)
                    .textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope > hr",
            s ->
                s.border(Border.none())
                    .borderBlockStart(Border.hairline(Tokens.BORDER_SUBTLE))
                    .my(CssLength.rem(2.25)));
  }
}
