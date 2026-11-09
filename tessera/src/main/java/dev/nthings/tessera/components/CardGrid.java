package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ParentElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;

/// Responsive grid of [Card]s, created with [#of(CssLength, IntoElement...)]. It fits as many
/// columns as are at least the minimum card width. Cards in the same row align their media,
/// header, body, and footer regions, so their footers line up.
public final class CardGrid extends BuiltinView implements ParentElement<CardGrid> {

  private final CssLength minCardWidth;
  private CssLength gap = Tokens.SPACE_4;
  private final List<Element> cards = new ArrayList<>();

  private CardGrid(CssLength minCardWidth) {
    this.minCardWidth = minCardWidth;
  }

  /// Creates a grid of `cards` whose columns are at least `minCardWidth` wide. Add more cards as
  /// children.
  public static CardGrid of(CssLength minCardWidth, IntoElement... cards) {
    return new CardGrid(minCardWidth).children(cards);
  }

  @Override
  public CardGrid self() {
    return this;
  }

  @Override
  public void appendChild(Element child) {
    cards.add(child);
  }

  /// Names this instance in the enclosing view scope. Without it, the compiler assigns
  /// `card-grid-<n>`.
  public CardGrid id(String id) {
    this.id = id;
    return this;
  }

  /// Sets the row and column gap. The default is [Tokens#SPACE_4].
  public CardGrid gap(CssLength gap) {
    this.gap = gap;
    return this;
  }

  @Override
  String viewType() {
    return "card-grid";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s -> s.gridAutoFit(minCardWidth).colGap(gap).rowGap(gap))
        .select(
            ":scope > article, :scope > label",
            s ->
                s.subgridRows(4)
                    .rowGap(Tokens.SPACE_3)
                    // Layout containment (container-type) would break subgrid.
                    .containerNone());
  }

  @Override
  IntoElement render(RenderContext context) {
    return Elements.section().children(cards);
  }
}
