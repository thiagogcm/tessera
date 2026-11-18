package dev.nthings.tessera.view;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.internal.authoring.TagData;
import dev.nthings.tessera.internal.css.CssAccess;
import dev.nthings.tessera.view.ViewExpansion.Reporter;
import dev.nthings.tessera.view.ViewSnapshot.Tag;
import dev.nthings.tessera.view.ViewSnapshot.Text;
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

/// Shared element, reference and accessibility checks for linked document or mounted content.
public final class ViewValidation {
  final Set<String> registeredDomIds;
  final Set<String> ambiguousDomIds;
  final Map<String, Tag<?>> elementsById = new HashMap<>();
  final Set<String> labelledControlIds = new HashSet<>();
  private final Reporter reporter;

  private ViewValidation(Set<String> ids, Set<String> ambiguousIds, Reporter reporter) {
    this.registeredDomIds = ids;
    this.ambiguousDomIds = ambiguousIds;
    this.reporter = reporter;
  }

  /// Checks linked content, document or mounted, with the shared element, reference and
  /// accessibility rules. The result runs [#checkDocument] on the same content.
  public static ViewValidation check(
      List<? extends ViewSnapshot.Node<?>> body,
      List<? extends ViewSnapshot.Node<?>> head,
      Set<String> ids,
      Set<String> ambiguousIds,
      Reporter reporter) {
    var state = new ViewValidation(ids, ambiguousIds, reporter);
    index(body, state);
    index(head, state);
    body.forEach(node -> validateNodeTree(node, state));
    head.forEach(node -> validateNodeTree(node, state));
    AccessibilityRules.check(body, state);
    return state;
  }

  /// Checks the whole-document rules a mounted root, which shares its page with host content, does
  /// not have: literal fragment targets, landmark names and heading order. Returns the first
  /// location of each outgoing URL.
  public Map<String, Location> checkDocument(
      List<? extends ViewSnapshot.Node<?>> body, List<? extends ViewSnapshot.Node<?>> head) {
    var links = new LinkedHashMap<String, Location>();
    checkLinks(body, links);
    checkLinks(head, links);
    AccessibilityRules.checkDocument(body, this);
    return Collections.unmodifiableMap(links);
  }

  private void checkLinks(List<? extends ViewSnapshot.Node<?>> nodes, Map<String, Location> links) {
    for (var node : nodes) {
      if (node instanceof ViewSnapshot.Group<?> group) {
        checkLinks(group.children(), links);
      } else if (node instanceof Tag<?> tag) {
        for (var urlAttribute : URL_ATTRIBUTES) {
          var url = tag.attributes().get(urlAttribute);
          if (url != null && !url.isBlank()) {
            checkLink(tag, urlAttribute, url, links);
          }
        }
        checkLinks(tag.children(), links);
      }
    }
  }

  private void checkLink(Tag<?> tag, String urlAttribute, String url, Map<String, Location> links) {
    URI reference;
    try {
      reference = new URI(url);
    } catch (URISyntaxException invalid) {
      return; // check reported it as invalid-url
    }
    links.putIfAbsent(url, tag.location());
    if (tag.tag() == HtmlTag.A
        && urlAttribute.equals("href")
        && url.length() > 1
        && url.startsWith("#")
        && !registeredDomIds.contains(reference.getFragment())) {
      report(
          tag.location(),
          "missing-fragment",
          "Anchor link href=\"" + url + "\" references a non-existent fragment ID in the document",
          "Use the target element's ElementRef, or correct the fragment to match an existing ID.");
    }
  }

  void report(Location location, String rule, String message, String fix) {
    reporter.report(location, rule, message, fix);
  }

  private static final List<String> URL_ATTRIBUTES = List.of("href", "src", "poster");

  /// Element contracts checked while the expanded element still carries authored declarations:
  /// image contracts, and URLs that would run script outside document assembly. HTML attribute
  /// names are already lowercase: the builder normalizes them.
  static void checkElement(ViewNode.Tag<?> tag, Declarations style, Reporter reporter) {
    var location = tag.location();
    if (tag.tag() == HtmlTag.IMG) {
      validateImage(tag.attributes(), style, reporter, location);
    }
    for (var attribute : tag.attributes().entrySet()) {
      var name = attribute.getKey();
      if (!TagData.URL_ATTRIBUTES.contains(name)) {
        continue;
      }
      var scheme = scheme(attribute.getValue());
      if (scheme.equals("javascript")) {
        reporter.report(
            location,
            "script-url",
            "Attribute " + name + " holds a javascript: URL, which runs script from markup",
            "Bind a Java handler with the browser module's LiveElement, or contribute a page script"
                + " from document content.");
      } else if (tag.tag() == HtmlTag.IFRAME && name.equals("src") && scheme.equals("data")) {
        reporter.report(
            location,
            "script-url",
            "An iframe src holds a data: URL, an inline document that can run script",
            "Load the frame from a published URL instead of embedding its document.");
      }
    }
  }

  /// The scheme of `url` in lowercase, as a browser parses it: ignoring surrounding whitespace and
  /// control characters and embedded tabs and newlines, or an empty string for a relative URL.
  private static String scheme(String url) {
    var scheme = new StringBuilder();
    for (var index = 0; index < url.length(); index++) {
      var character = url.charAt(index);
      if (character == ':') {
        return scheme.toString();
      }
      if (character <= ' ') {
        if (scheme.isEmpty() || character == '\t' || character == '\n' || character == '\r') {
          continue;
        }
        return "";
      }
      if (!(Character.isLetterOrDigit(character)
          || character == '+'
          || character == '-'
          || character == '.')) {
        return "";
      }
      scheme.append(Character.toLowerCase(character));
    }
    return "";
  }

  /// `<img>` contracts checkable from the element alone: alt text, then CLS-safe dimensions.
  private static void validateImage(
      Map<String, String> attrs, Declarations style, Reporter reporter, Location location) {
    var alt = attrs.get("alt");
    if (alt == null) {
      reporter.report(
          location,
          "image-alt",
          "<img> element is missing a required 'alt' attribute",
          "Provide descriptive alt text, or mark the image as decorative with empty alt text and presentation semantics.");
    }
    if (alt != null
        && alt.isEmpty()
        && !"presentation".equals(attrs.get("role"))
        && !"true".equals(attrs.get("aria-hidden"))) {
      reporter.report(
          location,
          "decorative-image",
          "Decorative <img> with empty alt=\"\" must declare role=\"presentation\" or aria-hidden=\"true\"",
          "Add role=\"presentation\" or aria-hidden=\"true\", or provide descriptive alt text.");
    }
    var hasWidthAndHeight = attrs.containsKey("width") && attrs.containsKey("height");
    if (!hasWidthAndHeight && !CssAccess.get().hasAspectRatio(style)) {
      reporter.report(
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
  static void index(List<? extends ViewSnapshot.Node<?>> nodes, ViewValidation state) {
    for (var node : nodes) {
      if (node instanceof ViewSnapshot.Group<?> group) {
        index(group.children(), state);
      } else if (node instanceof Tag<?> tag) {
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

  static void validateNodeTree(ViewSnapshot.Node<?> node, ViewValidation state) {
    switch (node) {
      case Text<?> _, ViewSnapshot.Special<?> _ -> {}
      case ViewSnapshot.Group<?> group ->
          group.children().forEach(child -> validateNodeTree(child, state));
      case Tag<?> tag -> {
        validateTagAccessibility(tag, state);
        for (var child : tag.children()) {
          validateNodeTree(child, state);
        }
      }
    }
  }

  private static void validateTagAccessibility(Tag<?> tag, ViewValidation state) {
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

  private static void validateCommandTarget(Tag<?> source, Tag<?> target, ViewValidation state) {
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
  static boolean declaresRole(Tag<?> tag) {
    var role = tag.attributes().get("role");
    return role != null && Role.fromWireValue(role) != null;
  }

  private static boolean hasAccessibleLabel(Tag<?> tag) {
    return hasAuthorName(tag) || containsReadableText(tag.children());
  }

  /// Whether `tag` carries an author-supplied name: `aria-label`, `aria-labelledby`, or `title`.
  static boolean hasAuthorName(Tag<?> tag) {
    return hasAriaName(tag) || isNonBlank(tag.attributes().get("title"));
  }

  /// Whether `tag` is named through `aria-label` or `aria-labelledby`.
  static boolean hasAriaName(Tag<?> tag) {
    var attrs = tag.attributes();
    return tag.unresolvedAttributes().contains("aria-labelledby")
        || isNonBlank(attrs.get("aria-label"))
        || isNonBlank(attrs.get("aria-labelledby"));
  }

  static boolean containsReadableText(List<? extends ViewSnapshot.Node<?>> children) {
    for (var child : children) {
      var found =
          switch (child) {
            case Text(var text) -> !text.isBlank();
            case ViewSnapshot.Special<?> special -> special.readable();
            case ViewSnapshot.Group<?> group -> containsReadableText(group.children());
            case Tag<?> nested ->
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
