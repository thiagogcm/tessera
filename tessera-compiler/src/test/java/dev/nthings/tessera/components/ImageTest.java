package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.el;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalStateException;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.AspectRatio;
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
            Image.of("/assets/thumbnail.webp", "Thumbnail").dimensions(400, 300));

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
        argumentSet(
            "empty alt, unknown dimensions still checks decorative semantics",
            Image.of("/a.png", "").unknownDimensions(),
            "decorative-image"),
        argumentSet("no reserved space", Image.of("/a.png", "Chart"), "image-dimensions"),
        argumentSet(
            "raw img, no alt, unknown dimensions",
            el(HtmlTag.IMG).attr("src", "/a.png").unknownDimensions(),
            "image-alt"));
  }

  @ParameterizedTest
  @MethodSource("inaccessibleImages")
  @DisplayName("an image that would be misread or shift the layout is reported")
  void reportsInaccessibleImage(IntoElement image, String rule) {
    assertThat(Diagnostics.of(() -> renderMain(image)))
        .singleElement()
        .satisfies(d -> {
          assertThat(d.rule()).isEqualTo(rule);
          assertThat(d.tag()).isEqualTo("img");
        });
  }

  @Test
  @DisplayName(
      "unknownDimensions opts an image out of image-dimensions, including through HeroImage")
  void unknownDimensionsAllowsUnmeasuredImages() {
    var page =
        renderDocumentMain(
            HeroImage.of(Image.of("https://example.com/hero.jpg", "Hero").unknownDimensions()),
            Image.of("https://example.com/photo.jpg", "Photo").unknownDimensions(),
            img("https://example.com/inline.png", "Inline").unknownDimensions());

    assertThat(page.elements("img")).hasSize(3);
  }

  @Test
  @DisplayName("unknownDimensions is rejected on non-img tags")
  void unknownDimensionsRejectsNonImages() {
    assertThatIllegalStateException().isThrownBy(() -> div().unknownDimensions());
  }
}
