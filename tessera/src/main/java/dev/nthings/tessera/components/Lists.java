package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CssNumber;
import dev.nthings.tessera.css.GridTrack;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.TrackList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/// Semantic HTML5 list primitives: [BulletList] (`<ul>`), [NumberedList] (`<ol>`),
/// [DescriptionList] (`<dl>`), and [StepList] (numbered steps powered by Chrome 156+
/// `sibling-index()`).
public final class Lists {

  private Lists() {}

  private static List<Element> texts(String... items) {
    return Arrays.stream(items).<Element>map(Elements::text).toList();
  }

  private static List<Element> elements(IntoElement... items) {
    return Arrays.stream(items).map(IntoElement::intoElement).toList();
  }

  private static Stream<TagElement> listItems(List<Element> items) {
    return items.stream().map(item -> Elements.li().child(item));
  }

  public record BulletList(List<Element> items) implements RenderOnce {

    public static BulletList ofStrings(String... items) {
      return new BulletList(texts(items));
    }

    public static BulletList of(IntoElement... items) {
      return new BulletList(elements(items));
    }

    @Override
    public Element render() {
      var ul = Elements.ul().flex().flexCol().gap(Tokens.SPACE_2).px(Tokens.SPACE_5);
      ul.children(listItems(items));
      return ul;
    }
  }

  public record NumberedList(List<Element> items) implements RenderOnce {

    public static NumberedList ofStrings(String... items) {
      return new NumberedList(texts(items));
    }

    public static NumberedList of(IntoElement... items) {
      return new NumberedList(elements(items));
    }

    @Override
    public Element render() {
      var ol = Elements.ol().flex().flexCol().gap(Tokens.SPACE_2).px(Tokens.SPACE_5).tabularNums();
      ol.children(listItems(items));
      return ol;
    }
  }

  public record Term(String term, Element description) {
    public static Term of(String term, String description) {
      return new Term(term, Elements.text(description));
    }

    public static Term of(String term, IntoElement description) {
      return new Term(term, description.intoElement());
    }
  }

  public record DescriptionList(List<Term> terms) implements RenderOnce {

    public static DescriptionList of(Term... terms) {
      return new DescriptionList(List.of(terms));
    }

    @Override
    public Element render() {
      var dl =
          Elements.dl()
              .grid()
              .gridColumns(
                  TrackList.of(
                      GridTrack.minmax(
                          GridTrack.length(CssLength.rem(8)), GridTrack.Keyword.MAX_CONTENT),
                      GridTrack.fr(1)))
              .gap(Tokens.SPACE_3);
      for (var entry : terms) {
        dl.children(
            Elements.dt().fontWeight(600).textColor(Tokens.TEXT_PRIMARY).text(entry.term()),
            Elements.dd().textColor(Tokens.TEXT_MUTED).child(entry.description()));
      }
      return dl;
    }
  }

  public record StepList(String id, List<Element> steps) implements RenderView {

    private static final CssIdent STEP_INDEX = CssIdent.custom("step-index");
    private static final CssIdent STEP_TOTAL = CssIdent.custom("step-total");

    public static StepList ofStrings(String id, String... steps) {
      return new StepList(id, texts(steps));
    }

    public static StepList of(String id, IntoElement... steps) {
      return new StepList(id, elements(steps));
    }

    @Override
    public ElementId entityId() {
      return ElementId.of("steps-" + id);
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(
              s ->
                  s.listStyleNone()
                      .flex()
                      .flexCol()
                      .gap(Tokens.SPACE_3)
                      .counterReset(CssIdent.of("step")))
          .select(
              ":scope > li",
              s ->
                  s.customProperty(STEP_INDEX, CssNumber.siblingIndex())
                      .customProperty(STEP_TOTAL, CssNumber.siblingCount())
                      .counterIncrement(CssIdent.of("step"))
                      .flex()
                      .itemsBaseline()
                      .gap(Tokens.SPACE_3))
          .select(
              ":scope > li::before",
              s ->
                  s.content(CssContent.counter(CssIdent.of("step")))
                      .inlineFlex()
                      .itemsCenter()
                      .justifyCenter()
                      .minInlineSize(CssLength.rem(1.75))
                      .blockSize(CssLength.rem(1.75))
                      .rounded(Tokens.RADIUS_FULL)
                      .fontWeight(600)
                      .tabularNums()
                      .bg(
                          Tokens.ACCENT_PRIMARY.mix(
                              CssColor.Space.OKLAB,
                              CssLength.pct(20)
                                  .plus(
                                      CssLength.pct(60)
                                          .times(CssNumber.property(STEP_INDEX))
                                          .dividedBy(CssNumber.property(STEP_TOTAL))),
                              Tokens.SURFACE_SUBTLE))
                      .textColor(Tokens.TEXT_PRIMARY));
    }

    @Override
    public Element render() {
      return Elements.ol().children(listItems(steps));
    }
  }
}
