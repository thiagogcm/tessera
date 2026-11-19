package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.document.DocumentContent;

/// An HTML document to compile with [Compiler]: its `lang`, its `<title>`, and the content of its
/// `<body>`. The two-argument constructor sets `lang` to `en`.
///
/// The theme is a compile setting ([Compiler#compile(PageDocument, Theme, OutputMode)] or the
/// site's theme), not part of the document.
public record PageDocument(String lang, String title, DocumentContent bodyRoot) {

  public PageDocument(String title, DocumentContent bodyRoot) {
    this("en", title, bodyRoot);
  }

  /// A document whose body is portable `bodyRoot`.
  public PageDocument(String lang, String title, IntoElement bodyRoot) {
    this(lang, title, DocumentContent.of(bodyRoot));
  }

  /// A document in `en` whose body is portable `bodyRoot`.
  public PageDocument(String title, IntoElement bodyRoot) {
    this("en", title, DocumentContent.of(bodyRoot));
  }
}
