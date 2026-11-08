package dev.nthings.tessera.render;

import dev.nthings.tessera.core.IntoElement;

/// Top-level HTML5 document specification compiled by [PageCompiler].
///
/// Design tokens are a compile setting ([PageCompiler#compile(PageDocument, Tokens, OutputMode)]
/// or the site's tokens), not part of the document.
public record PageDocument(String lang, String title, IntoElement bodyRoot) {

  public PageDocument(String title, IntoElement bodyRoot) {
    this("en", title, bodyRoot);
  }
}
