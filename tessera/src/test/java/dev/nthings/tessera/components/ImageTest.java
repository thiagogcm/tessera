package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.css.AspectRatio;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Image")
class ImageTest {

  @Nested
  @DisplayName("hero and secondary image")
  class HeroAndSecondary {

    private static RenderedPage page;

    @BeforeAll
    static void renderImages() {
      page =
          renderMain(
              Image.of("/assets/hero-banner.webp", "Hero Banner")
                  .hero()
                  .dimensions(1280, 720)
                  .element(img -> img.aspectRatio(AspectRatio.of(16, 9))),
              Image.of("/assets/thumbnail.webp", "Secondary Thumbnail").dimensions(400, 300));
    }

    @Test
    @DisplayName("the hero image is preloaded from head with high priority")
    void heroIsPreloaded() {
      assertThat(page.element("head > link[rel=preload][as=image]"))
          .hasAttribute("href", "/assets/hero-banner.webp")
          .hasAttribute("fetchpriority", "high");
    }

    @Test
    @DisplayName("the hero img is high priority and has no loading attribute")
    void heroHasHighPriority() {
      assertThat(page.element("img[src='/assets/hero-banner.webp']"))
          .hasAttribute("fetchpriority", "high")
          .doesNotHaveAttribute("loading");
    }

    @Test
    @DisplayName("non-hero images load lazily and decode asynchronously")
    void secondaryImageLoadsLazily() {
      assertThat(page.element("img[src='/assets/thumbnail.webp']"))
          .hasAttribute("loading", "lazy")
          .hasAttribute("decoding", "async")
          .doesNotHaveAttribute("fetchpriority");
    }
  }

  @Test
  @DisplayName("a second hero on a page reports a duplicate-hero-image diagnostic")
  void reportsDuplicateHero() {
    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(
            () ->
                renderMain(
                    Image.of("/hero1.webp", "Hero 1").hero().dimensions(800, 400),
                    Image.of("/hero2.webp", "Hero 2").hero().dimensions(800, 400)))
        .satisfies(
            failure ->
                assertThat(failure.diagnostics())
                    .singleElement()
                    .satisfies(
                        d -> {
                          assertThat(d.rule()).isEqualTo("duplicate-hero-image");
                          assertThat(d.location().components()).containsExactly("Image");
                        }));
  }

  @Test
  @DisplayName("element options reach the img and an explicit loading value wins")
  void elementOptionsReachImg() {
    var page =
        renderMain(
            Image.of("/assets/divider.svg", "")
                .dimensions(640, 8)
                .element(img -> img.ariaHidden(true).attr("loading", "eager")));

    assertThat(page.element("img"))
        .hasAttribute("aria-hidden", "true")
        .hasAttribute("loading", "eager")
        .hasAttribute("decoding", "async")
        .hasAttribute("width", "640");
  }
}
