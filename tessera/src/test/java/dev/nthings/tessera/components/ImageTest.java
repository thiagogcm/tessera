package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalStateException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.css.AspectRatio;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Image")
class ImageTest {

  @Nested
  @DisplayName("on a page with a hero and a secondary image")
  class HeroAndSecondary {

    private static RenderedPage page;

    @BeforeAll
    static void renderImages() {
      page =
          renderMain(
              Image.hero("/assets/hero-banner.webp", "Hero Banner")
                  .dimensions(1280, 720)
                  .aspectRatio(AspectRatio.of(16, 9)),
              Image.of("/assets/thumbnail.webp", "Secondary Thumbnail").dimensions(400, 300));
    }

    @Test
    @DisplayName("the hero is preloaded from <head> with high fetch priority (LCP)")
    void heroIsPreloadedWithHighFetchPriority() {
      assertThat(page.element("head > link[rel=preload][as=image]"))
          .hasAttribute("href", "/assets/hero-banner.webp")
          .hasAttribute("fetchpriority", "high");
    }

    @Test
    @DisplayName("the hero <img> is high priority and never lazy")
    void heroIsHighPriorityAndNeverLazy() {
      assertThat(page.element("img[src='/assets/hero-banner.webp']"))
          .hasAttribute("fetchpriority", "high")
          .doesNotHaveAttribute("loading");
    }

    @Test
    @DisplayName("below-the-fold images load lazily and decode asynchronously")
    void belowTheFoldImagesLoadLazily() {
      assertThat(page.element("img[src='/assets/thumbnail.webp']"))
          .hasAttribute("loading", "lazy")
          .hasAttribute("decoding", "async")
          .doesNotHaveAttribute("fetchpriority");
    }
  }

  @Test
  @DisplayName("only one hero image is allowed per page")
  void rejectsMoreThanOneHeroPerPage() {
    assertThatIllegalStateException()
        .isThrownBy(
            () ->
                renderMain(
                    Image.hero("/hero1.webp", "Hero 1").dimensions(800, 400),
                    Image.hero("/hero2.webp", "Hero 2").dimensions(800, 400)))
        .withMessageContaining("Only one hero image allowed per page");
  }
}
