package dev.nthings.tessera.css;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.render;

import dev.nthings.tessera.css.FlexLayout.Align;
import dev.nthings.tessera.css.FlexLayout.Wrap;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FlexLayout")
class FlexLayoutTest {

  @Test
  @DisplayName("a section lays out as a column with the recipe's spacing and keeps its tag")
  void sectionKeepsColumnSemantics() {
    var page =
        render(
            section()
                .layout(FlexLayout.column(CssLength.rem(0.75), Align.STRETCH, Wrap.NOWRAP))
                .children(p().text("One"), p().text("Two")));

    var column = page.element("section");
    assertThat(column.children()).extracting(child -> child.tagName()).containsExactly("p", "p");
    assertThat(page.ruleOf(column, "u-"))
        .contains("display:flex")
        .contains("flex-direction:column")
        .contains("flex-wrap:nowrap")
        .contains("align-items:stretch")
        .contains("gap:0.75rem");
  }

  @Test
  @DisplayName("an inline row stays in the surrounding line")
  void inlineRowStaysBesideNeighbors() {
    var page =
        render(
            p().children(
                    span().text("Before"),
                    span()
                        .layout(
                            FlexLayout.inlineRow(CssLength.rem(0.25), Align.BASELINE, Wrap.NOWRAP))
                        .children(span().text("A"), span().text("B")),
                    span().text("After")));

    var line = page.element("p");
    assertThat(line.children())
        .extracting(child -> child.tagName())
        .containsExactly("span", "span", "span");
    assertThat(page.ruleOf(line.child(1), "u-"))
        .contains("display:inline-flex")
        .contains("flex-direction:row")
        .contains("align-items:baseline")
        .contains("gap:0.25rem");
  }

  @Test
  @DisplayName("a height-constrained column leaves scrolling to the inner panel's own declarations")
  void constrainedColumnKeepsScrollDeclarations() {
    var page =
        render(
            section()
                .layout(FlexLayout.column(CssLength.rem(0.5), Align.STRETCH, Wrap.NOWRAP))
                .blockSize(CssLength.rem(8))
                .child(
                    div()
                        .flexGrow(1)
                        .flexShrink(1)
                        .minBlockSize(CssLength.px(0))
                        .overflowYAuto()
                        .child(p().text("Tall content"))));

    var column = page.element("section");
    var panel = page.element("section > div");
    assertThat(page.ruleOf(column, "u-"))
        .contains("display:flex")
        .contains("flex-direction:column")
        .contains("block-size:8rem");
    assertThat(page.ruleOf(panel, "u-"))
        .contains("flex-grow:1")
        .contains("flex-shrink:1")
        .contains("min-block-size:0px")
        .contains("overflow-y:auto")
        .doesNotContain("flex-direction");
  }

  @Test
  @DisplayName("refining one element's gap leaves other uses of the same recipe unchanged")
  void gapRefinementStaysOnOneElement() {
    var links = FlexLayout.row(Tokens.SPACE_4.length(), Align.CENTER, Wrap.WRAP);
    RenderedPage page =
        render(
            main()
                .children(
                    section().id("tuned").layout(links).gap(CssLength.rem(2)).child(p().text("A")),
                    nav().id("shared").layout(links).child(p().text("B"))));

    assertThat(page.ruleOf(page.elementById("tuned"), "u-"))
        .contains("gap:2rem")
        .contains("display:flex")
        .contains("flex-wrap:wrap");
    assertThat(page.ruleOf(page.elementById("shared"), "u-"))
        .contains("gap:var(--space-4)")
        .doesNotContain("gap:2rem");
  }
}
