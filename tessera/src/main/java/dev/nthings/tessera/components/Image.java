package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.core.InteractiveElement;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.StyleRefinement;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Image with an accessible description and reserved layout space. Create one with
/// [#of(String, String)]; it loads lazily (`loading="lazy"`, `decoding="async"`) unless it is the
/// page's [#hero()] image.
///
/// - Set [#dimensions(int, int)] or a CSS aspect ratio, or the compiler reports
///   `image-dimensions`.
/// - Missing alt text reports `image-alt`. Empty alt marks the image decorative and also needs
///   `role="presentation"` or `aria-hidden="true"`, or the compiler reports `decorative-image`.
public final class Image extends BuiltinComponent implements InteractiveElement<Image> {

  private static final class HeroSlot {
    private @Nullable String src;
  }

  private static final DocumentKey<HeroSlot> HERO =
      DocumentKey.of("hero-image", HeroSlot.class, HeroSlot::new);

  private final String src;
  private final TagElement img;
  private boolean hero;

  private Image(String src, String alt) {
    if (src.isBlank()) {
      throw new IllegalArgumentException("Image src cannot be blank");
    }
    this.src = src;
    this.img = Elements.img(src, alt);
  }

  /// Creates an image of `src` described by `alt`. Throws IllegalArgumentException if `src` is
  /// blank.
  public static Image of(String src, String alt) {
    return new Image(src, alt);
  }

  /// Makes this the page's main above-the-fold image: it loads eagerly with
  /// `fetchpriority="high"`, and a `<link rel="preload">` for it goes in `<head>`. A page allows
  /// one hero; a second reports `duplicate-hero-image`.
  public Image hero() {
    this.hero = true;
    img.attr("fetchpriority", "high");
    img.attributes().remove("loading");
    return this;
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

  @Override
  IntoElement render(RenderContext context) {
    if (hero) {
      var slot = context.document(HERO);
      var first = slot.src;
      if (first != null) {
        context.report(
            "duplicate-hero-image",
            "Only one hero image is allowed per page: '"
                + first
                + "' is already the hero, so '"
                + src
                + "' cannot be",
            "Call hero() on one image per page; load the others lazily.");
        return img;
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
