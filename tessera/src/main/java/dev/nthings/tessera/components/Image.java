package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.core.InteractiveElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.StyleRefinement;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Image with an accessible description and reserved layout space.
///
/// - [#of(String, String)] loads lazily: `loading="lazy"` and `decoding="async"`.
/// - [#hero(String, String)] is the page's main above-the-fold image: it loads eagerly with
///   `fetchpriority="high"`, and its `<link rel="preload">` goes in `<head>`. A page allows one
///   hero; a second throws IllegalStateException during render.
/// - Set [#dimensions(int, int)] or a CSS aspect ratio, or the compiler reports
///   `image-dimensions`.
/// - Missing alt text reports `image-alt`. Empty alt marks the image decorative and also needs
///   `role="presentation"` or `aria-hidden="true"`, or the compiler reports `decorative-image`.
public final class Image implements Component, InteractiveElement<Image> {

  private static final class HeroSlot {
    private @Nullable String src;
  }

  private static final DocumentKey<HeroSlot> HERO =
      DocumentKey.of("hero-image", HeroSlot.class, HeroSlot::new);

  private final String src;
  private final String alt;
  private final boolean hero;
  private final TagElement img;

  private Image(String src, String alt, boolean hero) {
    if (src.isBlank()) {
      throw new IllegalArgumentException("Image src cannot be blank");
    }
    this.src = src;
    this.alt = alt;
    this.hero = hero;
    this.img = Elements.img(src, alt);
    if (hero) {
      img.attr("fetchpriority", "high");
      img.attributes().remove("loading");
    }
  }

  /// Creates an image that loads lazily. Throws IllegalArgumentException if `src` is blank.
  public static Image of(String src, String alt) {
    return new Image(src, alt, false);
  }

  /// Creates the page's hero image. Throws IllegalArgumentException if `src` is blank.
  public static Image hero(String src, String alt) {
    return new Image(src, alt, true);
  }

  @Override
  public Image self() {
    return this;
  }

  @Override
  public StyleRefinement style() {
    return img.style();
  }

  @Override
  public Image transitionKey(TransitionKey key) {
    img.transitionKey(key);
    return this;
  }

  @Override
  public Map<String, String> attributes() {
    return img.attributes();
  }

  @Override
  public Map<String, IdReference> idReferences() {
    return img.idReferences();
  }

  /// Sets the `width` and `height` attributes. Throws IllegalArgumentException if either is not
  /// positive.
  @Override
  public Image dimensions(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Image width and height must be positive");
    }
    return InteractiveElement.super.dimensions(width, height);
  }

  public String src() {
    return src;
  }

  public String alt() {
    return alt;
  }

  /// Whether this image was created with [#hero(String, String)].
  public boolean isHero() {
    return hero;
  }

  @Override
  public Element render(RenderContext context) {
    if (hero) {
      var slot = context.document(HERO);
      if (slot.src != null) {
        throw new IllegalStateException(
            "Only one hero image allowed per page: already registered '"
                + slot.src
                + "', cannot register '"
                + src
                + "'");
      }
      slot.src = src;
      context.contributeHead(
          Elements.link()
              .attr("rel", "preload")
              .attr("as", "image")
              .attr("href", src)
              .attr("fetchpriority", "high"));
    }
    return img;
  }
}
