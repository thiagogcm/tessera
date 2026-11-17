package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.IntoElement;

/// An HTML document to compile with [Compiler]: its `lang`, its `<title>`, and the content of its
/// `<body>`. The two-argument constructor sets `lang` to `en`.
///
/// The theme is a compile setting ([Compiler#compile(PageDocument, Theme, OutputMode)] or the
/// site's theme), not part of the document.
public record PageDocument(String lang, String title, IntoElement bodyRoot) {

  public PageDocument(String title, IntoElement bodyRoot) {
    this("en", title, bodyRoot);
  }
}
