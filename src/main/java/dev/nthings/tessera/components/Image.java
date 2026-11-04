package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.FluentBuilder;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.StyleRefinement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/// Core Web Vitals-optimized image component enforcing LCP priority hoisting and layout
/// stability.
///
/// - [String)][#hero(String,] tags the page's primary Largest Contentful Paint candidate,
///   emitting `fetchpriority="high"` and hoisting an explicit `<link rel="preload" as="image">`
///   into the document `<head>`.
/// - [String)][#of(String,] provisions below-the-fold images with `loading="lazy"` and
///   `decoding="async"`.
/// - Compile-time Phase 2 enforcement requires either explicit dimensions or a CSS `aspect-ratio`
///   to eliminate Cumulative Layout Shift (CLS).
public final class Image implements RenderOnce, FluentBuilder<Image> {

  private final String src;
  private final String alt;
  private final boolean hero;
  private @Nullable Integer width;
  private @Nullable Integer height;
  private @Nullable String aspectRatio;
  private @Nullable TransitionKey transitionKey;
  private final Map<String, String> attributes = new LinkedHashMap<>();
  private final StyleRefinement style = new StyleRefinement();

  private Image(String src, String alt, boolean hero) {
    if (src == null || src.isBlank()) {
      throw new IllegalArgumentException("Image src cannot be blank");
    }
    this.src = src;
    this.alt = alt != null ? alt : "";
    this.hero = hero;
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

  public Image dimensions(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Image width and height must be positive");
    }
    this.width = width;
    this.height = height;
    return this;
  }

  public Image aspectRatio(String ratio) {
    this.aspectRatio = ratio;
    return this;
  }

  public Image aspectRatio(int widthRatio, int heightRatio) {
    this.aspectRatio = widthRatio + " / " + heightRatio;
    return this;
  }

  public Image transitionKey(TransitionKey key) {
    this.transitionKey = key;
    return this;
  }

  public Image attr(String name, String value) {
    attributes.put(name, value);
    return this;
  }

  public Image className(String className) {
    String existing = attributes.get("class");
    attributes.put(
        "class", existing == null || existing.isBlank() ? className : existing + " " + className);
    return this;
  }

  public Image style(Consumer<StyleRefinement> styler) {
    styler.accept(style);
    return this;
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
  public Element render() {
    if (hero) {
      RenderContext.current().registerHeroImage(src);
    }
    TagElement img = Elements.img(src, alt);
    if (hero) {
      img.attr("fetchpriority", "high");
      img.attributes().remove("loading");
    } else {
      img.attr("loading", "lazy");
      img.attr("decoding", "async");
    }
    if (width != null && height != null) {
      img.dimensions(width, height);
    }
    if (aspectRatio != null) {
      img.aspectRatio(aspectRatio);
    }
    if (transitionKey != null) {
      img.transitionKey(transitionKey);
    }
    attributes.forEach(img::attr);
    img.style().mergeFrom(style);
    return img;
  }
}
