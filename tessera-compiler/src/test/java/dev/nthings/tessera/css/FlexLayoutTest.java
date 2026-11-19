package dev.nthings.tessera.css;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.render;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.FlexLayout.Align;
import dev.nthings.tessera.css.FlexLayout.Wrap;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("FlexLayout")
class FlexLayoutTest {

  static Stream<Arguments> recipes() {
    return Stream.of(
        argumentSet(
            "column section",
            section(),
            "section",
            FlexLayout.column(CssLength.rem(0.75), Align.STRETCH, Wrap.NOWRAP),
            "display:flex",
            "flex-direction:column"),
        argumentSet(
            "inline row span",
            span(),
            "span",
            FlexLayout.inlineRow(CssLength.rem(0.25), Align.BASELINE, Wrap.NOWRAP),
            "display:inline-flex",
            "flex-direction:row"));
  }

  @ParameterizedTest
  @MethodSource("recipes")
  @DisplayName("a recipe keeps the element's tag and children and lays them out")
  void recipeKeepsElementSemantics(
      TagElement host, String tag, FlexLayout layout, String display, String direction) {
    var page =
        render(main().child(host.layout(layout).children(span().text("A"), span().text("B"))));

    var laidOut = page.element("main > *");
    assertThat(laidOut.tagName()).isEqualTo(tag);
    assertThat(laidOut.children()).extracting(child -> child.text()).containsExactly("A", "B");
    assertThat(page.ruleOf(laidOut, "u-")).contains(display, direction, "gap:");
  }

  @Test
  @DisplayName("refining one element's gap leaves other uses of the same recipe unchanged")
  void refinesGapLocally() {
    var links = FlexLayout.row(Tokens.SPACE_4, Align.CENTER, Wrap.WRAP);
    var page =
        render(
            main()
                .children(
                    section()
                        .id(ElementRef.scoped("tuned"))
                        .layout(links)
                        .gap(CssLength.rem(2))
                        .child(span().text("A")),
                    nav().id(ElementRef.scoped("shared")).layout(links).child(span().text("B"))));

    assertThat(page.ruleOf(page.elementById("tuned"), "u-"))
        .contains("gap:2rem")
        .doesNotContain("var(--space-4)");
    assertThat(page.ruleOf(page.elementById("shared"), "u-")).contains("gap:var(--space-4)");
  }
}
