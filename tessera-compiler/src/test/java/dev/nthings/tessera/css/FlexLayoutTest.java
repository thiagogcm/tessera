package dev.nthings.tessera.css;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.render;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.css.FlexLayout.Align;
import dev.nthings.tessera.css.FlexLayout.Wrap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FlexLayout")
class FlexLayoutTest {

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
