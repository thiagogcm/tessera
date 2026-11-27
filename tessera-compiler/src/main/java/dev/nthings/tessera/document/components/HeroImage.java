package dev.nthings.tessera.document.components;

import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.core.DocumentContent;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.internal.components.WidgetBridge;
import dev.nthings.tessera.internal.document.DocumentNode;
import org.jspecify.annotations.Nullable;

/// Document decorator that places a portable [Image] as the page's main above-the-fold hero image:
/// it loads eagerly with `fetchpriority="high"` and contributes a `<link rel="preload">` to
/// `<head>`. Preloading is a document operation, so the result is [DocumentContent]. A page allows
/// one hero image; a second reports `duplicate-hero-image`.
public final class HeroImage {

  private static final class HeroSlot {
    private @Nullable String src;
  }

  private static final ViewKey<HeroSlot> HERO =
      ViewKey.of("hero-image", HeroSlot.class, HeroSlot::new);

  private HeroImage() {}

  /// Places `image` as the page's hero image. Configure `image` (dimensions, element styles, ARIA)
  /// before expanding the page.
  public static DocumentContent of(Image image) {
    return DocumentNode.namedComponent(HeroImage.class, context -> expand(image, context));
  }

  private static DocumentContent expand(Image image, DocumentContext context) {
    var parts = WidgetBridge.image(image);
    var slot = context.state(HERO);
    var first = slot.src;
    if (first != null) {
      context.report(
          "duplicate-hero-image",
          "Only one hero image is allowed per page: '"
              + first
              + "' is already the hero, so '"
              + parts.src()
              + "' cannot be",
          "Place one hero image per page; load the others lazily.");
      return parts.eagerElement();
    }
    slot.src = parts.src();
    context.contributeHead(
        Elements.link()
            .attr("rel", "preload")
            .attr("as", "image")
            .attr("href", parts.src())
            .attr("fetchpriority", "high"));
    return parts.eagerElement();
  }
}
