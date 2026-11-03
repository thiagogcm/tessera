package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import java.util.List;

/**
 * Scoped CSS Subgrid container for {@link Card} items.
 *
 * <p>Aligns the 4 canonical card regions ({@code media}, {@code header}, {@code content}, {@code
 * footer}) across sibling cards in the same row using {@code grid-template-rows: subgrid; grid-row:
 * span 4}.
 */
public record CardGrid(
    String id, CssLength minCardWidth, CssLength gap, List<? extends IntoElement> cards)
    implements RenderView {

  public CardGrid(String id, CssLength minCardWidth, CssLength gap, IntoElement... cards) {
    this(id, minCardWidth, gap, List.of(cards));
  }

  @Override
  public ElementId entityId() {
    return ElementId.of(id);
  }

  @Override
  public String viewScopeRule() {
    return ":scope{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,"
        + minCardWidth.toCss()
        + "),1fr));column-gap:"
        + gap.toCss()
        + ";row-gap:"
        + gap.toCss()
        + "}"
        + ":scope > article,:scope > label{"
        + "display:grid;grid-row:span 4;grid-template-rows:subgrid;row-gap:0.75rem"
        + "}";
  }

  @Override
  public Element render() {
    return Elements.section().children(cards);
  }
}
