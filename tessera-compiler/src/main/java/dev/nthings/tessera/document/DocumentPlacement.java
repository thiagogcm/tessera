package dev.nthings.tessera.document;

import dev.nthings.tessera.internal.document.DocumentAccess;
import dev.nthings.tessera.internal.document.DocumentNode;

/// Closed wrapper around internal document placements so [DocumentTree] permits only same-package
/// types.
record DocumentPlacement(DocumentNode node) implements DocumentTree {
  static {
    DocumentAccess.registerPlacement(
        tree -> ((DocumentPlacement) tree).node(), DocumentPlacement::new);
  }

  @Override
  public DocumentTree key(String key) {
    return new DocumentPlacement(node.withKey(key));
  }
}
