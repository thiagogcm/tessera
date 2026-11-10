package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Carousel")
class CarouselTest {

  @Test
  @DisplayName("is a focusable region named by its label")
  void trackIsFocusableRegion() {
    var page = renderMain(Carousel.of("Demo").slide(p().text("1")).slide(p().text("2")));

    var track = page.element("main > [role=region]");

    assertThat(track).hasAttribute("aria-label", "Demo").hasAttribute("tabindex", "0");
    assertThat(track.select("> .carousel-item > .carousel-card > p"))
        .extracting(slide -> slide.text())
        .containsExactly("1", "2");
  }

  @Test
  @DisplayName("emits native ::scroll-button prev/next controls with alt text")
  void emitsNativeScrollButtons() {
    var css = renderMain(Carousel.of("Demo").slide(p().text("1"))).css();

    assertThat(css)
        .contains("::scroll-button(inline-start){content:\"‹\" / \"Previous slide\"")
        .contains("::scroll-button(inline-end){content:\"›\" / \"Next slide\"")
        .contains("::scroll-button(*):disabled");
  }
}
