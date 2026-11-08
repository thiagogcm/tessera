package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// Subgrid container for [Card]s. Sibling cards align their four regions (media, header, content,
/// footer) row by row.
public record CardGrid(
    String id, CssLength minCardWidth, CssLength gap, List<? extends IntoElement> cards)
    implements RenderView {

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
                    .rowGap(Tokens.SPACE_3)
                    // Layout containment (container-type) would break subgrid.
                    .containerNormal());
  }

  @Override
  public Element render(RenderContext context) {
    return Elements.section().children(cards);
  }
}
