package dev.nthings.tessera.components;

import dev.nthings.tessera.core.ElementBlock;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TagHost;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.components.WidgetBridge;
import dev.nthings.tessera.internal.components.WidgetBridge.ImageParts;

/// Image with an accessible description and reserved layout space. Create one with [#of(String,
/// String)]; it decodes asynchronously and loads lazily (`loading="lazy"`). For a page's hero image
/// with eager loading and head preload, wrap it with `HeroImage.of(image)` in
/// `dev.nthings.tessera.document.components`. Style the `<img>`, give it ARIA states, a ref or a
/// transition key with [#element(ElementBlock)].
///
/// - Set [#dimensions(int, int)] or a CSS aspect ratio, or the compiler reports `image-dimensions`.
/// - Missing alt text reports `image-alt`. Empty alt marks the image decorative and also needs
///   `role="presentation"` or `aria-hidden="true"`, or the compiler reports `decorative-image`.
public final class Image extends BuiltinComponent implements TagHost, FluentBuilder<Image> {

  static {
    WidgetBridge.registerImage(image -> new ImageParts(image.src, image.render(true)));
  }

  private final String src;
  private final TagElement img;

  private Image(String src, String alt) {
    if (src.isBlank()) {
      throw new IllegalArgumentException("Image src cannot be blank");
    }
    this.src = src;
    this.img = Elements.el(HtmlTag.IMG).attr("src", src).attr("alt", alt);
  }

  /// Creates an image of `src` described by `alt`. Throws IllegalArgumentException if `src` is
  /// blank.
  public static Image of(String src, String alt) {
    return new Image(src, alt);
  }

  @Override
  public Image self() {
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
  /// transition key. The image sets `loading`, `decoding` and `fetchpriority` when it renders, so a
  /// value `block` gives one of them wins.
  public Image element(ElementBlock block) {
    block.configure(img);
    return this;
  }

  @Override
  public TagElement toTagElement() {
    return render(false);
  }

  @Override
  IntoElement expand(ViewContext context) {
    return toTagElement();
  }

  /// A copy of the configured `<img>` with its loading attributes.
  private TagElement render(boolean hero) {
    var element = img.copy();
    if (hero) {
      defaultAttr(element, "fetchpriority", "high");
    } else {
      defaultAttr(element, "loading", "lazy");
    }
    defaultAttr(element, "decoding", "async");
    return element;
  }

  private static void defaultAttr(TagElement element, String name, String value) {
    if (!AuthoringAccess.tag(element).attributes.containsKey(name)) {
      element.attr(name, value);
    }
  }
}
