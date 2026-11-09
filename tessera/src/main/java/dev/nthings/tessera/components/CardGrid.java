package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// Responsive grid of [Card]s, created with `CardGrid.of`. Cards in the same row align their media,
/// header, content, and footer regions, so their footers line up.
public record CardGrid(
    String id, CssLength minCardWidth, CssLength gap, List<? extends IntoElement> cards)
    implements View {

  /// Creates a grid with `id` as its instance id. It fits as many columns as are at least
  /// `minCardWidth` wide, and `gap` sets the row and column gap.
  public static CardGrid of(
      String id, CssLength minCardWidth, CssLength gap, IntoElement... cards) {
    return new CardGrid(id, minCardWidth, gap, List.of(cards));
  }

  @Override
  public String viewId() {
    return id;
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(s -> s.gridAutoFit(minCardWidth).colGap(gap).rowGap(gap))
        .select(
            ":scope > article, :scope > label",
            s ->
                s.subgridRows(4)
                    .rowGap(Tokens.SPACE_3)
                    // Layout containment (container-type) would break subgrid.
                    .containerNormal());
  }

  @Override
  public Element render(RenderContext context) {
    return Elements.section().children(cards);
  }
}
