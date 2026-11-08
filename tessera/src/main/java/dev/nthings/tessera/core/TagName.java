package dev.nthings.tessera.core;

/// Emitted element name: a built-in [HtmlTag] or an author-defined [CustomElement].
/// Sealed so compiler dispatch stays exhaustive.
public sealed interface TagName permits HtmlTag, CustomElement {

  String tagName();

  /// Whether the element has no children or end tag.
  boolean isVoid();

  /// Native heading level, or 0 for elements that are not headings.
  default int headingLevel() {
    return 0;
  }
}
