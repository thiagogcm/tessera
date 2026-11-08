package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.custom;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.ul;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.render.CompilationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fragment and custom element authoring")
class ElementSurfaceTest {

  @Test
  @DisplayName("a fragment keeps direct child content-model positions")
  void fragmentKeepsDirectChildren() {
    var page = renderMain(ul().child(fragment().children(li().text("One"), li().text("Two"))));

    assertThat(page.elements("ul > li")).hasSize(2);
    assertThat(page.elements("ul > div")).isEmpty();
  }

  @Test
  @DisplayName("an unlabelled button inside a fragment is reported")
  void fragmentChildrenParticipateInDiagnostics() {
    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() -> renderMain(fragment().child(button())))
        .withMessageContaining("interactive-label");
  }

  record Rows() implements RenderView {
    @Override
    public Element render(RenderContext context) {
      return fragment().children(li().text("One"), li().text("Two"));
    }
  }

  @Test
  @DisplayName("a view rooted in a fragment is hosted by one div carrying its data-view")
  void fragmentRootedViewGetsOneHost() {
    var page = renderMain(ul().child(new Rows()));

    assertThat(page.elements("ul > div[data-view=rows]")).hasSize(1);
    assertThat(page.elements("ul > div[data-view=rows] > li")).hasSize(2);
  }

  @Test
  @DisplayName("a custom element renders its content")
  void customElementRendersContent() {
    var page = renderMain(custom("my-widget").text("Hello"));

    assertThat(page.element("main > my-widget")).hasText("Hello");
  }
}
