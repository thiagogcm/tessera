package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedText;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.StyleRefinement;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Checks document contracts and collects outgoing URLs on the linked tree.
final class PageValidator {
  private PageValidator() {}

  private static final List<String> URL_ATTRIBUTES = List.of("href", "src", "poster");

  /// `<img>` contracts checkable from the element alone: alt text, then CLS-safe dimensions.
  static void validateImage(
      Map<String, String> attrs,
      StyleRefinement style,
      CompilationState state,
      ViewLocation location) {
    var alt = attrs.get("alt");
    if (alt == null) {
      state.report(
          location,
          "image-alt",
          "<img> element is missing a required 'alt' attribute",
          "Provide descriptive alt text, or mark the image as decorative with empty alt text and presentation semantics.");
    }
    if (alt != null
        && alt.isEmpty()
        && !"presentation".equals(attrs.get("role"))
        && !"true".equals(attrs.get("aria-hidden"))) {
      state.report(
          location,
          "decorative-image",
          "Decorative <img> with empty alt=\"\" must declare role=\"presentation\" or aria-hidden=\"true\"",
          "Add role=\"presentation\" or aria-hidden=\"true\", or provide descriptive alt text.");
    }
    var hasWidthAndHeight = attrs.containsKey("width") && attrs.containsKey("height");
    if (!hasWidthAndHeight && !style.hasAspectRatio()) {
      state.report(
          location,
          "image-dimensions",
          "<img> element with src=\""
              + attrs.getOrDefault("src", "")
              + "\" must declare both 'width' and 'height' attributes or a CSS 'aspect-ratio' to"
              + " prevent Cumulative Layout Shift (CLS)",
          "Set .dimensions(width, height) or a CSS aspect-ratio to reserve the image's layout space.");
    }
  }

  /// Indexes the linked elements checks look up: by id and labelled control ids.
  static void index(List<ResolvedNode> nodes, CompilationState state) {
    for (var node : nodes) {
      if (node instanceof ResolvedTag tag) {
        var id = tag.attributes().get("id");
        if (id != null && !id.isBlank()) {
          state.elementsById.putIfAbsent(id, tag);
        }
        var labelled = tag.attributes().get("for");
        if (tag.tag() == HtmlTag.LABEL && labelled != null) {
          state.labelledControlIds.add(labelled);
        }
        index(tag.children(), state);
      }
    }
  }

  static void validateNodeTree(ResolvedNode node, CompilationState state) {
    switch (node) {
      case ResolvedText _, ResolvedRaw _ -> {}
      case ResolvedTag tag -> {
        validateTagAccessibility(tag, state);
        for (var child : tag.children()) {
          validateNodeTree(child, state);
        }
      }
    }
  }

  private static void validateTagAccessibility(ResolvedTag tag, CompilationState state) {
    var attrs = tag.attributes();
    var registeredDomIds = state.registeredDomIds;

    // A declared role hands naming to AccessibilityRules' role-name-required.
    if ((tag.tag() == HtmlTag.BUTTON || tag.tag() == HtmlTag.A) && !declaresRole(tag)) {
      if (!hasAccessibleLabel(tag)) {
        state.report(
            tag.location(),
            "interactive-label",
            "Interactive <"
                + tag.tag().tagName()
                + "> element must have text content or an aria-label / aria-labelledby / title attribute",
            "Add visible label text, .ariaLabel(...), or .ariaLabelledBy(...) referencing a label.");
      }
    }

    for (var urlAttribute : URL_ATTRIBUTES) {
      var url = attrs.get(urlAttribute);
      if (url == null || url.isBlank()) {
        continue;
      }
      URI reference;
      try {
        reference = new URI(url);
      } catch (URISyntaxException invalid) {
        state.report(
            tag.location(),
            "invalid-url",
            "Attribute "
                + urlAttribute
                + "=\""
                + url
                + "\" is not a valid URI: "
                + invalid.getReason(),
            "Correct the URL syntax and percent-encode spaces and literal percent signs.");
        continue;
      }
      state.outgoingLinks.putIfAbsent(url, state.location(tag.location()));
      if (tag.tag() == HtmlTag.A
          && urlAttribute.equals("href")
          && url.startsWith("#")
          && url.length() > 1) {
        var fragment = reference.getFragment();
        if (!registeredDomIds.contains(fragment)) {
          state.report(
              tag.location(),
              "missing-fragment",
              "Anchor link href=\""
                  + url
                  + "\" references a non-existent fragment ID in the document",
              "Use the target element's ElementRef, or correct the fragment to match an existing ID.");
        }
      }
    }

    var commandFor = attrs.get("commandfor");
    if (commandFor != null) {
      var target = state.elementsById.get(commandFor);
      if (target == null) {
        state.report(
            tag.location(),
            "missing-command-target",
            "Invoker attribute commandfor=\""
                + commandFor
                + "\" references a non-existent target ID in the document",
            "Bind an ElementRef on the target and pass that reference to .command(...).");
      } else if (!state.ambiguousDomIds.contains(commandFor)) {
        validateCommandTarget(tag, target, state);
      }
    }

    var interestFor = attrs.get("interestfor");
    if (interestFor != null && !registeredDomIds.contains(interestFor)) {
      state.report(
          tag.location(),
          "missing-interest-target",
          "Interest invoker attribute interestfor=\""
              + interestFor
              + "\" references a non-existent target ID in the document",
          "Point interestfor at an existing element, or use .interestTooltip(...) to create a linked tooltip.");
    }
  }

  private static void validateCommandTarget(
      ResolvedTag source, ResolvedTag target, CompilationState state) {
    var command = source.attributes().get("command");
    if (command == null) {
      return;
    }
    var requirement =
        switch (command.toLowerCase(Locale.ROOT)) {
          case "show-modal", "close", "request-close" ->
              target.tag() == HtmlTag.DIALOG ? null : "a <dialog> element";
          case "toggle-popover", "show-popover", "hide-popover" ->
              target.attributes().containsKey("popover")
                  ? null
                  : "an element with a popover attribute";
          default -> null; // Custom and platform commands are outside Tessera's built-in set.
        };
    if (requirement != null) {
      state.report(
          source.location(),
          "incompatible-command-target",
          "Command \""
              + command
              + "\" targets "
              + "<"
              + target.tag().tagName()
              + "> with ID \""
              + target.attributes().get("id")
              + "\""
              + " but requires "
              + requirement,
          "Point this command at "
              + requirement
              + ", or choose a command compatible with the target.");
    }
  }

  /// Whether `tag` declares a role that the accessibility rules model.
  static boolean declaresRole(ResolvedTag tag) {
    var role = tag.attributes().get("role");
    return role != null && Role.fromWireValue(role) != null;
  }

  private static boolean hasAccessibleLabel(ResolvedTag tag) {
    return hasAuthorName(tag) || containsReadableText(tag.children());
  }

  /// Whether `tag` carries an author-supplied name: `aria-label`, `aria-labelledby`, or `title`.
  static boolean hasAuthorName(ResolvedTag tag) {
    return hasAriaName(tag) || isNonBlank(tag.attributes().get("title"));
  }

  /// Whether `tag` is named through `aria-label` or `aria-labelledby`.
  static boolean hasAriaName(ResolvedTag tag) {
    var attrs = tag.attributes();
    return tag.unresolvedAttributes().contains("aria-labelledby")
        || isNonBlank(attrs.get("aria-label"))
        || isNonBlank(attrs.get("aria-labelledby"));
  }

  static boolean containsReadableText(List<ResolvedNode> children) {
    for (var child : children) {
      var found =
          switch (child) {
            case ResolvedText(var text) -> !text.isBlank();
            case ResolvedRaw(var raw) -> !raw.isBlank();
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

  static boolean isNonBlank(@Nullable String value) {
    return value != null && !value.isBlank();
  }
}
