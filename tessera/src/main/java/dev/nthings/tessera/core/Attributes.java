package dev.nthings.tessera.core;

import dev.nthings.tessera.internal.authoring.TagData;
import java.util.Locale;
import java.util.regex.Pattern;

/// Checks shared by the generic attribute setters.
final class Attributes {
  private Attributes() {}

  /// An attribute name output can emit verbatim: ASCII letters, digits, `-`, `_`, `.` and `:`,
  /// starting with a letter, `_` or `:`.
  private static final Pattern NAME = Pattern.compile("[A-Za-z_:][A-Za-z0-9_:.-]*");

  /// Refuses names that are not plain attribute names, `id`, `role` and `aria-*`, which have typed
  /// methods, and the attributes that embed script or markup: inline event handlers (`on*`) and
  /// `srcdoc`. Names are compared case-insensitively, as HTML does. Checks `attribute` as
  /// [#requireGeneric(String)] does and returns the name to store: lowercase on an HTML element,
  /// whose attribute names are case-insensitive, and as written on SVG, whose names such as
  /// `viewBox` are case-sensitive.
  static String generic(TagName tag, String attribute) {
    requireGeneric(attribute);
    return tag.namespace() == TagName.Namespace.HTML
        ? attribute.toLowerCase(Locale.ROOT)
        : attribute;
  }

  /// Checks `attribute` as [#generic(TagName, String)] does for an attribute that holds DOM ids,
  /// and refuses URL-valued attributes: an id written there would become a URL that skips the URL
  /// checks. Links use `href(ElementRef)`.
  static String idList(TagName tag, String attribute) {
    var name = generic(tag, attribute);
    if (TagData.URL_ATTRIBUTES.contains(name.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException(
          "'"
              + attribute
              + "' holds a URL, not DOM ids: link to an element with href(ElementRef) instead");
    }
    return name;
  }

  static void requireGeneric(String attribute) {
    if (!NAME.matcher(attribute).matches()) {
      throw new IllegalArgumentException(
          "Attribute name '"
              + attribute
              + "' must start with a letter, '_' or ':' and contain only ASCII letters, digits,"
              + " '-', '_', '.' or ':'");
    }
    var lower = attribute.toLowerCase(Locale.ROOT);
    if (lower.startsWith("on")) {
      throw new IllegalArgumentException(
          "Inline event handler attribute '"
              + attribute
              + "' runs script from markup: bind a Java handler with the browser module's"
              + " LiveElement, or contribute a page script from document content");
    }
    if (lower.equals("srcdoc")) {
      throw new IllegalArgumentException(
          "The 'srcdoc' attribute embeds unvalidated markup: load the frame from src instead");
    }
    if (lower.equals("id")) {
      throw new IllegalArgumentException(
          "Use id(ElementRef) instead of the attribute 'id', so the compiler can scope and link it");
    }
    if (lower.equals("role") || lower.startsWith("aria-")) {
      throw new IllegalArgumentException(
          "Use the typed TagElement method (role(Role), aria...(...)) instead of the attribute '"
              + attribute
              + "'");
    }
  }
}
