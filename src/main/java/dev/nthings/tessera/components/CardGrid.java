package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CssProperty;
import dev.nthings.tessera.css.ScopeStyle;
import java.util.List;

/// Scoped CSS Subgrid container for [Card] items.
///
/// Aligns the 4 canonical card regions (`media`, `header`, `content`, `footer`) across sibling
/// cards in the same row using `grid-template-rows: subgrid; grid-row: span 4`.
public record CardGrid(
    String id, CssLength minCardWidth, CssLength gap, List<? extends IntoElement> cards)
    implements RenderView {

  public CardGrid {
    cards = List.copyOf(cards);
  }

  public static CardGrid of(
      String id, CssLength minCardWidth, CssLength gap, IntoElement... cards) {
    return new CardGrid(id, minCardWidth, gap, List.of(cards));
  }

  @Override
  public ElementId entityId() {
    return ElementId.of(id);
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(s -> s.gridAutoFit(minCardWidth).colGap(gap).rowGap(gap))
        .select(
            ":scope > article, :scope > label",
            s ->
                s.subgridRows(4)
                    .rowGap(CssLength.rem(0.75))
                    // A query container is layout-contained and cannot be a subgrid.
                    .put(CssProperty.CONTAINER_TYPE, "normal"));
  }

  @Override
  public Element render() {
    return Elements.section().children(cards);
  }
}
