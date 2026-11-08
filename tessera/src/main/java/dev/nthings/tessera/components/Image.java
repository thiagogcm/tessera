package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.core.InteractiveElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.Interactivity;
import dev.nthings.tessera.css.StyleRefinement;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Core Web Vitals-optimized image component enforcing LCP priority hoisting and layout
/// stability.
///
/// - [#hero(String, String)] tags the page's primary Largest Contentful Paint candidate,
///   emitting `fetchpriority="high"` and hoisting an explicit `<link rel="preload" as="image">`
///   into the document `<head>`.
/// - [#of(String, String)] provisions below-the-fold images with `loading="lazy"` and
///   `decoding="async"`.
/// - Compile-time Phase 2 enforcement requires either explicit dimensions or a CSS `aspect-ratio`
///   to eliminate Cumulative Layout Shift (CLS).
public final class Image implements RenderOnce, InteractiveElement<Image> {

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

  public static Image of(String src, String alt) {
    return new Image(src, alt, false);
  }

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
  public Interactivity interactivity() {
    return img.interactivity();
  }

  @Override
  public Map<String, String> attributes() {
    return img.attributes();
  }

  @Override
  public Map<String, IdReference> idReferences() {
    return img.idReferences();
  }

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

  public boolean isHero() {
    return hero;
  }

  @Override
  public Element render(RenderContext context) {
    if (hero) {
      var ctx = context;
      var slot = ctx.document(HERO);
      if (slot.src != null) {
        throw new IllegalStateException(
            "Only one hero image allowed per page: already registered '"
                + slot.src
                + "', cannot register '"
                + src
                + "'");
      }
      slot.src = src;
      ctx.contributeHead(
          Elements.link()
              .attr("rel", "preload")
              .attr("as", "image")
              .attr("href", src)
              .attr("fetchpriority", "high"));
    }
    return img;
  }
}
