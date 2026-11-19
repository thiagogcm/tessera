package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.css.AspectRatio;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.components.HeroImage;
import dev.nthings.tessera.support.Diagnostics;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Image")
class ImageTest {

  @Test
  @DisplayName(
      "the hero loads eagerly with high priority and is preloaded once; others load lazily")
  void heroIsPreloadedAndOthersAreLazy() {
    var page =
        renderDocumentMain(
            HeroImage.of(
                Image.of("/assets/hero-banner.webp", "Hero Banner")
                    .dimensions(1280, 720)
                    .element(img -> img.aspectRatio(AspectRatio.of(16, 9)))),
            DocumentContent.of(
                Image.of("/assets/thumbnail.webp", "Thumbnail").dimensions(400, 300)));

    var hero = page.element("img[alt=\"Hero Banner\"]");
    assertThat(hero).hasAttribute("fetchpriority", "high").doesNotHaveAttribute("loading");
    assertThat(page.elements("head > link[rel=preload]"))
        .singleElement()
        .satisfies(preload ->
            assertThat(preload)
                .hasAttribute("as", "image")
                .hasAttribute("href", hero.attr("src"))
                .hasAttribute("fetchpriority", "high"));
    assertThat(page.element("img[alt=Thumbnail]"))
        .hasAttribute("loading", "lazy")
        .hasAttribute("decoding", "async")
        .doesNotHaveAttribute("fetchpriority");
  }

  @Test
  @DisplayName("a loading value set on the img wins over the lazy default")
  void explicitLoadingWins() {
    var page =
        renderMain(
            Image.of("/assets/divider.svg", "")
                .dimensions(640, 8)
                .element(img -> img.ariaHidden(true).attr("loading", "eager")));

    assertThat(page.element("img"))
        .hasAttribute("aria-hidden", "true")
        .hasAttribute("loading", "eager");
  }

  @Test
  @DisplayName("a second hero on a page is reported")
  void reportsDuplicateHero() {
    var found =
        Diagnostics.of(() ->
            renderDocumentMain(
                HeroImage.of(Image.of("/hero1.webp", "Hero 1").dimensions(800, 400)),
                HeroImage.of(Image.of("/hero2.webp", "Hero 2").dimensions(800, 400))));

    assertThat(found)
        .singleElement()
        .satisfies(d -> {
          assertThat(d.rule()).isEqualTo("duplicate-hero-image");
          assertThat(d.components()).containsExactly("HeroImage");
        });
  }

  static Stream<Arguments> inaccessibleImages() {
    return Stream.of(
        argumentSet(
            "empty alt, not hidden", Image.of("/a.png", "").dimensions(1, 1), "decorative-image"),
        argumentSet("no reserved space", Image.of("/a.png", "Chart"), "image-dimensions"));
  }

  @ParameterizedTest
  @MethodSource("inaccessibleImages")
  @DisplayName("an image that would be misread or shift the layout is reported")
  void reportsInaccessibleImage(Image image, String rule) {
    assertThat(Diagnostics.of(() -> renderMain(image)))
        .singleElement()
        .satisfies(d -> {
          assertThat(d.rule()).isEqualTo(rule);
          assertThat(d.tag()).isEqualTo("img");
        });
  }
}
