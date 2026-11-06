package dev.nthings.tessera.render;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.css.CssProperty;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.render.ResolvedNode.ResolvedEmpty;
import dev.nthings.tessera.render.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.render.ResolvedNode.ResolvedSlot;
import dev.nthings.tessera.render.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.render.ResolvedNode.ResolvedText;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Checks document contracts and collects outgoing URLs on the linked tree.
final class PageValidator {
  private PageValidator() {}

  private static final List<String> URL_ATTRIBUTES = List.of("href", "src", "poster");

  /// `<img>` contracts checkable from the element alone: alt text, then CLS-safe dimensions.
  static void validateImage(Map<String, String> attrs, StyleRefinement style) {
    var alt = attrs.get("alt");
    if (alt == null) {
      throw new AccessibilityViolationException(
          "<img> element is missing a required 'alt' attribute");
    }
    if (alt.isEmpty()
        && !"presentation".equals(attrs.get("role"))
        && !"true".equals(attrs.get("aria-hidden"))) {
      throw new AccessibilityViolationException(
          "Decorative <img> with empty alt=\"\" must declare role=\"presentation\" or aria-hidden=\"true\"");
    }
    var hasWidthAndHeight = attrs.containsKey("width") && attrs.containsKey("height");
    if (!hasWidthAndHeight && style.get(CssProperty.ASPECT_RATIO) == null) {
      throw new AccessibilityViolationException(
          "<img> element with src=\""
              + attrs.getOrDefault("src", "")
              + "\" must declare both 'width' and 'height' attributes or a CSS 'aspect-ratio' to"
              + " prevent Cumulative Layout Shift (CLS)");
    }
  }

  static void validateNodeTree(ResolvedNode node, CompilationState state) {
    switch (node) {
      case ResolvedEmpty _, ResolvedText _, ResolvedRaw _ -> {}
      case ResolvedSlot _ -> throw unlinked();
      case ResolvedTag tag -> {
        validateTagAccessibility(tag, state);
        for (var child : tag.children()) {
          validateNodeTree(child, state);
        }
        var companion = tag.companionSibling();
        if (companion != null) {
          validateNodeTree(companion, state);
        }
      }
    }
  }

  private static void validateTagAccessibility(ResolvedTag tag, CompilationState state) {
    var attrs = tag.attributes();
    var registeredDomIds = state.registeredDomIds;

    if (tag.tag() == HtmlTag.BUTTON || tag.tag() == HtmlTag.A) {
      if (!hasAccessibleLabel(tag)) {
        throw new AccessibilityViolationException(
            "Interactive <"
                + tag.tag().tagName()
                + "> element must have text content or an aria-label / aria-labelledby / title attribute");
      }
    }

    for (var urlAttribute : URL_ATTRIBUTES) {
      var url = attrs.get(urlAttribute);
      if (url != null && !url.isBlank()) {
        state.outgoingHrefs.add(url);
      }
    }

    if (tag.tag() == HtmlTag.A) {
      var href = attrs.get("href");
      if (href != null) {
        if (href.startsWith("#") && href.length() > 1) {
          var fragment = href.substring(1);
          if (!registeredDomIds.contains(fragment)) {
            throw new AccessibilityViolationException(
                "Anchor link href=\""
                    + href
                    + "\" references a non-existent fragment ID in the document");
          }
        }
      }
    }

    var commandFor = attrs.get("commandfor");
    if (commandFor != null && !registeredDomIds.contains(commandFor)) {
      throw new AccessibilityViolationException(
          "Invoker attribute commandfor=\""
              + commandFor
              + "\" references a non-existent target ID in the document");
    }

    var interestFor = attrs.get("interestfor");
    if (interestFor != null && !registeredDomIds.contains(interestFor)) {
      throw new AccessibilityViolationException(
          "Interest invoker attribute interestfor=\""
              + interestFor
              + "\" references a non-existent target ID in the document");
    }
  }

  private static boolean hasAccessibleLabel(ResolvedTag tag) {
    var attrs = tag.attributes();
    if (isNonBlank(attrs.get("aria-label"))
        || isNonBlank(attrs.get("aria-labelledby"))
        || isNonBlank(attrs.get("title"))) {
      return true;
    }
    return containsReadableText(tag.children());
  }

  private static boolean containsReadableText(List<ResolvedNode> children) {
    for (var child : children) {
      var found =
          switch (child) {
            case ResolvedText(var text) -> !text.isBlank();
            case ResolvedRaw(var raw) -> !raw.isBlank();
            case ResolvedEmpty _ -> false;
            case ResolvedSlot _ -> throw unlinked();
            case ResolvedTag nested ->
                (nested.tag() == HtmlTag.IMG && isNonBlank(nested.attributes().get("alt")))
                    || isNonBlank(nested.attributes().get("aria-label"))
                    || containsReadableText(nested.children());
          };
      if (found) {
        return true;
      }
    }
    return false;
  }

  private static IllegalStateException unlinked() {
    return new IllegalStateException("Late-element slot reached validation/paint before linking");
  }

  private static boolean isNonBlank(@Nullable String value) {
    return value != null && !value.isBlank();
  }
}
