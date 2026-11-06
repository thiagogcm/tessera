package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ParentElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;

/// Editorial typographic container providing proportional vertical rhythm, fluid font sizing, and
/// optimal reading line-length (measure) for documentation and narrative prose.
public final class Prose implements RenderView, ParentElement<Prose> {

  private final ElementId id;
  private final List<Element> children = new ArrayList<>();
  private CssLength maxMeasure = CssLength.ch(75);

  private Prose(ElementId id) {
    this.id = id;
  }

  public static Prose of(IntoElement... elements) {
    var prose = new Prose(ElementId.of("prose"));
    prose.children(elements);
    return prose;
  }

  public static Prose create(String id) {
    return new Prose(ElementId.of(id));
  }

  @Override
  public Prose self() {
    return this;
  }

  @Override
  public void appendChild(Element child) {
    children.add(child);
  }

  public Prose maxMeasure(CssLength measure) {
    this.maxMeasure = measure;
    return this;
  }

  @Override
  public ElementId entityId() {
    return id;
  }

  @Override
  public String viewType() {
    return "prose-container";
  }

  @Override
  public Element render() {
    var container = div().attr("class", "prose-canvas").flex().flexCol().gap(Tokens.SPACE_10);
    container.children(children);
    return container;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
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
                s.borderNone()
                    .borderBlockStartSubtle(Tokens.BORDER_SUBTLE)
                    .my(CssLength.rem(2.25)));
  }
}
