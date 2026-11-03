package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/**
 * Semantic HTML5 list primitives: {@link BulletList} ({@code <ul>}), {@link NumberedList} ({@code
 * <ol>}), {@link DescriptionList} ({@code <dl>}), and {@link StepList} (numbered steps powered by
 * Chrome 156+ {@code sibling-index()}).
 */
public final class Lists {

  private Lists() {}

  public record BulletList(List<Element> items) implements RenderOnce {

    public static BulletList ofStrings(String... items) {
      return new BulletList(java.util.Arrays.stream(items).<Element>map(Elements::text).toList());
    }

    public static BulletList of(IntoElement... items) {
      return new BulletList(java.util.Arrays.stream(items).map(IntoElement::intoElement).toList());
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
      return new NumberedList(java.util.Arrays.stream(items).<Element>map(Elements::text).toList());
    }

    public static NumberedList of(IntoElement... items) {
      return new NumberedList(
          java.util.Arrays.stream(items).map(IntoElement::intoElement).toList());
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
      return new StepList(id, java.util.Arrays.stream(steps).<Element>map(Elements::text).toList());
    }

    public static StepList of(String id, IntoElement... steps) {
      return new StepList(
          id, java.util.Arrays.stream(steps).map(IntoElement::intoElement).toList());
    }

    @Override
    public ElementId entityId() {
      return ElementId.of("steps-" + id);
    }

    @Override
    public String viewScopeRule() {
      return ":scope{"
          + "list-style:none;display:flex;flex-direction:column;gap:0.75rem;"
          + "counter-reset:step"
          + "}"
          + ":scope > li{"
          + "--step-index:sibling-index();--step-total:sibling-count();"
          + "counter-increment:step;"
          + "display:flex;align-items:baseline;gap:0.75rem"
          + "}"
          + ":scope > li::before{"
          + "content:counter(step);"
          + "display:inline-flex;align-items:center;justify-content:center;"
          + "min-inline-size:1.75rem;block-size:1.75rem;border-radius:999px;"
          + "font-weight:600;font-variant-numeric:tabular-nums;"
          + "background:color-mix(in oklch, var(--accent-primary) "
          + "calc(20% + (60% * var(--step-index) / var(--step-total))), var(--surface-subtle));"
          + "color:var(--text-primary)"
          + "}";
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
