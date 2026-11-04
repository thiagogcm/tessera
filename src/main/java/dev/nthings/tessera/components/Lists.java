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

/// Semantic HTML5 list primitives: [BulletList] (` `), [NumberedList] (` `), [DescriptionList]
/// (`<dl>`), and [StepList] (numbered steps powered by Chrome 156+ `sibling-index()`).
public final class Lists {

  private Lists() {}

  public record BulletList(List<Element> items) implements RenderOnce {

    public static BulletList ofStrings(String... items) {
      return new BulletList(Arrays.stream(items).<Element>map(Elements::text).toList());
    }

    public static BulletList of(IntoElement... items) {
      return new BulletList(Arrays.stream(items).map(IntoElement::intoElement).toList());
    }

    @Override
    public Element render() {
      TagElement ul =
          Elements.ul().flex().flexCol().gap(CssLength.rem(0.5)).px(CssLength.rem(1.25));
      for (var item : items) {
        ul.child(Elements.li().child(item));
      }
      return ul;
    }
  }

  public record NumberedList(List<Element> items) implements RenderOnce {

    public static NumberedList ofStrings(String... items) {
      return new NumberedList(Arrays.stream(items).<Element>map(Elements::text).toList());
    }

    public static NumberedList of(IntoElement... items) {
      return new NumberedList(Arrays.stream(items).map(IntoElement::intoElement).toList());
    }

    @Override
    public Element render() {
      TagElement ol =
          Elements.ol()
              .flex()
              .flexCol()
              .gap(CssLength.rem(0.5))
              .px(CssLength.rem(1.25))
              .tabularNums();
      for (var item : items) {
        ol.child(Elements.li().child(item));
      }
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
      TagElement dl =
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
      return new StepList(id, Arrays.stream(steps).<Element>map(Elements::text).toList());
    }

    public static StepList of(String id, IntoElement... steps) {
      return new StepList(id, Arrays.stream(steps).map(IntoElement::intoElement).toList());
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
                  s.cssContent("counter(step)")
                      .inlineFlex()
                      .itemsCenter()
                      .justifyCenter()
                      .minInlineSize(CssLength.rem(1.75))
                      .blockSize(CssLength.rem(1.75))
                      .rounded(CssLength.px(999))
                      .fontWeight(600)
                      .tabularNums()
                      .bgRaw(
                          "color-mix(in oklch, var(--accent-primary) "
                              + "calc(20% + (60% * var(--step-index) / var(--step-total))), "
                              + "var(--surface-subtle))")
                      .textColor(Tokens.TEXT_PRIMARY));
    }

    @Override
    public Element render() {
      TagElement ol = Elements.ol();
      for (var step : steps) {
        ol.child(Elements.li().child(step));
      }
      return ol;
    }
  }
}
