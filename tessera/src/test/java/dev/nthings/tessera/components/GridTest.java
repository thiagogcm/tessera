package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.support.CssBlocks;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Grid")
class GridTest {

  @Test
  @DisplayName("auto-fit columns use the minimum width and honor the gap")
  void autoFitUsesMinimumWidthAndGap() {
    var page =
        renderMain(
            Grid.autoFit(
                    CssLength.rem(10), div().text("One"), div().text("Two"), div().text("Three"))
                .gap(CssLength.rem(0.75)));

    var container = page.element("main > div");
    var grid = container.child(0);
    assertThat(page.ruleOf(container, "u-"))
        .contains("container-name:tessera-grid")
        .contains("container-type:inline-size");
    assertThat(page.ruleOf(grid, "u-"))
        .contains("display:grid")
        .contains("grid-template-columns:repeat(auto-fit, minmax(min(100%, 10rem), 1fr))")
        .contains("gap:0.75rem");
    assertThat(grid.children()).extracting(Element::text).containsExactly("One", "Two", "Three");
  }

  @Test
  @DisplayName("fixed columns collapse to one column below 36rem")
  void fixedColumnsCollapseOnNarrowContainers() {
    var page =
        renderMain(
            Grid.columns(3, div().text("One"), div().text("Two"), div().text("Three"))
                .gap(CssLength.rem(0.75)));
    var grid = page.element("main > div").child(0);

    assertThat(page.ruleOf(grid, "u-"))
        .contains("display:grid")
        .contains("grid-template-columns:minmax(0, 1fr)")
        .contains("gap:0.75rem");
    assertThat(CssBlocks.block(page.css(), "@container tessera-grid (inline-size >= 36rem){"))
        .contains("grid-template-columns:repeat(3, minmax(0, 1fr))");
  }

  @Test
  @DisplayName("cell spans apply only when the grid is wide enough")
  void cellSpansApplyOnlyInWideGrids() {
    var page =
        renderMain(
            Grid.columns(
                3,
                Grid.cell(2, 1, div().text("Wide")),
                Grid.cell(1, 2, div().text("Tall")),
                div().text("Cell")));

    var cells = page.elements("main > div > div > div");
    assertThat(page.ruleOf(cells.get(0), "cq-")).isEqualTo("grid-column:span 2;grid-row:span 1");
    assertThat(page.ruleOf(cells.get(1), "cq-")).isEqualTo("grid-column:span 1;grid-row:span 2");
  }

  @Test
  @DisplayName("rejects a non-positive column count")
  void rejectsNonPositiveColumnCount() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Grid.columns(0))
        .withMessage("Repeat count must be positive: 0");
  }
}
