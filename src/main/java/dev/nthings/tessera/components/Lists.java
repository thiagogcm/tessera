package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
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
      var ul = Elements.ul().flex().flexCol().gap(CssLength.rem(0.5)).px(CssLength.rem(1.25));
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
      var ol =
          Elements.ol()
              .flex()
              .flexCol()
              .gap(CssLength.rem(0.5))
              .px(CssLength.rem(1.25))
              .tabularNums();
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
              .gridColumns("minmax(8rem, max-content) 1fr")
              .gap(CssLength.rem(0.75));
      for (var entry : terms) {
        dl.children(
            Elements.dt().fontWeight(600).textColor(Tokens.TEXT_PRIMARY).text(entry.term()),
            Elements.dd().textColor(Tokens.TEXT_MUTED).child(entry.description()));
      }
      return dl;
    }
  }

  public record StepList(String id, List<Element> steps) implements RenderView {

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
              s -> s.listStyleNone().flex().flexCol().gap(CssLength.rem(0.75)).counterReset("step"))
          .select(
              ":scope > li",
              s ->
                  s.customProperty("--step-index", "sibling-index()")
                      .customProperty("--step-total", "sibling-count()")
                      .counterIncrement("step")
                      .flex()
                      .itemsBaseline()
                      .gap(CssLength.rem(0.75)))
          .select(
              ":scope > li::before",
              s ->
                  s.content("counter(step)")
                      .inlineFlex()
                      .itemsCenter()
                      .justifyCenter()
                      .minInlineSize(CssLength.rem(1.75))
                      .blockSize(CssLength.rem(1.75))
                      .rounded(CssLength.px(999))
                      .fontWeight(600)
                      .tabularNums()
                      .bgRaw(
                          "color-mix(in oklab, "
                              + Tokens.ACCENT_PRIMARY.varReference()
                              + " "
                              + "calc(20% + (60% * var(--step-index) / var(--step-total))), "
                              + Tokens.SURFACE_SUBTLE.varReference()
                              + ")")
                      .textColor(Tokens.TEXT_PRIMARY));
    }

    @Override
    public Element render() {
      return Elements.ol().children(listItems(steps));
    }
  }
}
