package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.Counter;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CssNumber;
import dev.nthings.tessera.css.CustomProperty;
import dev.nthings.tessera.css.GridTrack;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.TrackList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/// List primitives: [BulletList] (`<ul>`), [NumberedList] (`<ol>`), [DescriptionList] (`<dl>`),
/// and [StepList] (an `<ol>` drawn as numbered steps). Items of the bullet, numbered and step
/// lists are wrapped in `<li>`.
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

  /// Unordered list of items.
  public record BulletList(List<Element> items) implements Component {

    /// Creates a list from plain text items.
    public static BulletList ofStrings(String... items) {
      return new BulletList(texts(items));
    }

    /// Creates a list from elements, each of which may contain markup.
    public static BulletList of(IntoElement... items) {
      return new BulletList(elements(items));
    }

    @Override
    public Element render(RenderContext context) {
      var ul = Elements.ul().flex().flexCol().gap(Tokens.SPACE_2).px(Tokens.SPACE_5);
      ul.children(listItems(items));
      return ul;
    }
  }

  /// Ordered list of items.
  public record NumberedList(List<Element> items) implements Component {

    /// Creates a list from plain text items.
    public static NumberedList ofStrings(String... items) {
      return new NumberedList(texts(items));
    }

    /// Creates a list from elements, each of which may contain markup.
    public static NumberedList of(IntoElement... items) {
      return new NumberedList(elements(items));
    }

    @Override
    public Element render(RenderContext context) {
      var ol = Elements.ol().flex().flexCol().gap(Tokens.SPACE_2).px(Tokens.SPACE_5).tabularNums();
      ol.children(listItems(items));
      return ol;
    }
  }

  /// Term and its description, for a [DescriptionList].
  public record Term(String term, Element description) {
    /// Creates a term whose description is plain text.
    public static Term of(String term, String description) {
      return new Term(term, Elements.text(description));
    }

    /// Creates a term whose description is `description`, which may contain markup.
    public static Term of(String term, IntoElement description) {
      return new Term(term, description.intoElement());
    }
  }

  /// Description list: each term is a `<dt>` and each description a `<dd>`.
  public record DescriptionList(List<Term> terms) implements Component {

    /// Creates a description list from `terms`.
    public static DescriptionList of(Term... terms) {
      return new DescriptionList(List.of(terms));
    }

    @Override
    public Element render(RenderContext context) {
      var dl =
          Elements.dl()
              .grid()
              .gridColumns(
                  TrackList.of(
                      GridTrack.minmax(GridTrack.length(CssLength.rem(8)), GridTrack.maxContent()),
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

  /// Ordered list drawn as numbered steps. The step badges shade toward the accent color as the
  /// steps progress. `id` identifies the list within its view scope.
  public record StepList(String id, List<Element> steps) implements View {

    private static final Counter STEP = Counter.named("step");
    private static final CustomProperty STEP_INDEX = CustomProperty.named("step-index");
    private static final CustomProperty STEP_TOTAL = CustomProperty.named("step-total");

    /// Creates a step list from plain text steps.
    public static StepList ofStrings(String id, String... steps) {
      return new StepList(id, texts(steps));
    }

    /// Creates a step list from elements, each of which may contain markup.
    public static StepList of(String id, IntoElement... steps) {
      return new StepList(id, elements(steps));
    }

    @Override
    public String viewId() {
      return "steps-" + id;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s -> s.listStyleNone().flex().flexCol().gap(Tokens.SPACE_3).counterReset(STEP))
          .select(
              ":scope > li",
              s ->
                  s.customProperty(STEP_INDEX, CssNumber.siblingIndex())
                      .customProperty(STEP_TOTAL, CssNumber.siblingCount())
                      .counterIncrement(STEP)
                      .flex()
                      .itemsBaseline()
                      .gap(Tokens.SPACE_3))
          .select(
              ":scope > li::before",
              s ->
                  s.content(CssContent.counter(STEP))
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
    public Element render(RenderContext context) {
      return Elements.ol().children(listItems(steps));
    }
  }
}
