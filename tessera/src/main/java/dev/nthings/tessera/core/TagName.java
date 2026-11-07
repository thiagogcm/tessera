package dev.nthings.tessera.core;

/// Name of an emitted HTML element: a built-in [HtmlTag] or an author-defined [CustomElement].
///
/// The sealed hierarchy keeps compiler dispatch exhaustive while allowing custom elements alongside
/// the closed set of semantic tags.
public sealed interface TagName permits HtmlTag, CustomElement {

  /// The serialized element name, such as `div` or `my-widget`.
  String tagName();

  /// Whether the element cannot have children or an end tag.
  boolean isVoid();

  /// Native heading level, or 0 for elements that are not headings.
  default int headingLevel() {
    return 0;
  }
}
