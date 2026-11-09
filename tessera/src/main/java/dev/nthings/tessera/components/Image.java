package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.ElementBlock;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.TagElement;
import org.jspecify.annotations.Nullable;

/// Image with an accessible description and reserved layout space. Create one with
/// [#of(String, String)]; it decodes asynchronously and loads lazily (`loading="lazy"`) unless it
/// is the page's [#hero()] image. Style the `<img>`, give it ARIA states, a ref or a transition key
/// with [#element(ElementBlock)].
///
/// - Set [#dimensions(int, int)] or a CSS aspect ratio, or the compiler reports
///   `image-dimensions`.
/// - Missing alt text reports `image-alt`. Empty alt marks the image decorative and also needs
///   `role="presentation"` or `aria-hidden="true"`, or the compiler reports `decorative-image`.
public final class Image extends BuiltinComponent {

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
    this.img = new TagElement(HtmlTag.IMG).attr("src", src).attr("alt", alt);
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
    return this;
  }

  /// Sets the `width` and `height` attributes, which reserve the image's layout space. Throws
  /// IllegalArgumentException if either is not positive.
  public Image dimensions(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Image width and height must be positive");
    }
    img.dimensions(width, height);
    return this;
  }

  /// Runs `block` on the `<img>` now, to set its styles, ARIA states, attributes, refs or
  /// transition key. The image sets `loading`, `decoding` and `fetchpriority` when it renders,
  /// so a value `block` gives one of them wins.
  public Image element(ElementBlock block) {
    block.configure(img);
    return this;
  }

  @Override
  IntoElement render(RenderContext context) {
    var element = img.copy();
    if (hero) {
      defaultAttr(element, "fetchpriority", "high");
    } else {
      defaultAttr(element, "loading", "lazy");
    }
    defaultAttr(element, "decoding", "async");
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
        return element;
      }
      slot.src = src;
      context.contributeHead(
          Elements.link()
              .attr("rel", "preload")
              .attr("as", "image")
              .attr("href", src)
              .attr("fetchpriority", "high"));
    }
    return element;
  }

  private static void defaultAttr(TagElement element, String name, String value) {
    if (!element.attributes().containsKey(name)) {
      element.attr(name, value);
    }
  }
}
