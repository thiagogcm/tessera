package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedText;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.Declarations;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Checks document contracts and collects outgoing URLs on the linked tree.
final class PageValidator {
  @FunctionalInterface
  interface Reporter {
    void report(Location location, String rule, String message, String suggestion);
  }

  final Set<String> registeredDomIds;
  final Set<String> ambiguousDomIds;
  final Map<String, ResolvedTag> elementsById = new HashMap<>();
  final Set<String> labelledControlIds = new HashSet<>();
  private final Reporter reporter;

  private PageValidator(Set<String> ids, Set<String> ambiguousIds, Reporter reporter) {
    this.registeredDomIds = ids;
    this.ambiguousDomIds = ambiguousIds;
    this.reporter = reporter;
  }

  void report(Location location, String rule, String message, String suggestion) {
    reporter.report(location, rule, message, suggestion);
  }

  static Map<String, Location> validate(
      List<ResolvedNode> body,
      List<ResolvedNode> head,
      Set<String> ids,
      Set<String> ambiguousIds,
      Reporter reporter) {
    var state = new PageValidator(ids, ambiguousIds, reporter);
    index(body, state);
    index(head, state);
    body.forEach(node -> validateNodeTree(node, state));
    head.forEach(node -> validateNodeTree(node, state));
    var links = new LinkedHashMap<String, Location>();
    state.checkLinks(body, links);
    state.checkLinks(head, links);
    AccessibilityRules.check(body, state);
    return Collections.unmodifiableMap(links);
  }

  private void checkLinks(List<ResolvedNode> nodes, Map<String, Location> links) {
    for (var node : nodes) {
      if (node instanceof ResolvedTag tag) {
        for (var attribute : URL_ATTRIBUTES) {
          var url = tag.attributes().get(attribute);
          if (url == null || url.isBlank()) {
            continue;
          }
          URI reference;
          try {
            reference = new URI(url);
          } catch (URISyntaxException invalid) {
            continue;
          }
          links.putIfAbsent(url, tag.location());
          if (tag.tag() == HtmlTag.A
              && attribute.equals("href")
              && url.startsWith("#")
              && url.length() > 1
              && !registeredDomIds.contains(reference.getFragment())) {
            report(
                tag.location(),
                "missing-fragment",
                "Anchor link href=\""
                    + url
                    + "\" references a non-existent fragment ID in the document",
                "Use the target element's ElementRef, or correct the fragment to match an existing ID.");
          }
        }
        checkLinks(tag.children(), links);
      }
    }
  }

  private static final List<String> URL_ATTRIBUTES = List.of("href", "src", "poster");

  /// `<img>` contracts checkable from the element alone: alt text, then CLS-safe dimensions.
  static void validateImage(
      Map<String, String> attrs, Declarations style, ExpansionState state, Location location) {
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
  static void index(List<ResolvedNode> nodes, PageValidator state) {
    for (var node : nodes) {
      if (node instanceof ResolvedTag tag) {
        var id = tag.attributes().get("id");
        if (id != null) {
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

  static void validateNodeTree(ResolvedNode node, PageValidator state) {
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

  private static void validateTagAccessibility(ResolvedTag tag, PageValidator state) {
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
      try {
        new URI(url);
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
      ResolvedTag source, ResolvedTag target, PageValidator state) {
    var command = source.attributes().get("command");
    if (command == null) {
      return;
    }
    var builtin = BuiltinCommand.fromWireValue(command);
    if (builtin == null) {
      return; // Custom and platform commands are outside Tessera's built-in set.
    }
    var requirement =
        switch (builtin.target()) {
          case DIALOG -> target.tag() == HtmlTag.DIALOG ? null : "a <dialog> element";
          case POPOVER ->
              target.attributes().containsKey("popover")
                  ? null
                  : "an element with a popover attribute";
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
