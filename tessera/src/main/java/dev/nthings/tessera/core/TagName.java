package dev.nthings.tessera.core;

/// The name of an element: a built-in [HtmlTag], an author-defined [CustomElement], or an [SvgTag].
public sealed interface TagName permits HtmlTag, CustomElement, SvgTag {

  /// The element namespace that a DOM backend creates the element in.
  enum Namespace {
    HTML("http://www.w3.org/1999/xhtml"),
    SVG("http://www.w3.org/2000/svg");

    private final String uri;

    Namespace(String uri) {
      this.uri = uri;
    }

    /// The namespace URI, as passed to `createElementNS`.
    public String uri() {
      return uri;
    }
  }

  String tagName();

  /// Whether the element has no children or end tag.
  boolean isVoid();

  /// The namespace the element belongs to; HTML unless stated otherwise.
  default Namespace namespace() {
    return Namespace.HTML;
  }

  /// Native heading level, or 0 for elements that are not headings.
  default int headingLevel() {
    return 0;
  }
}
