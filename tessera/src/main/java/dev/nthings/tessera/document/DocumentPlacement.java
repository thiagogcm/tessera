package dev.nthings.tessera.document;

import dev.nthings.tessera.internal.document.DocumentAccess;
import dev.nthings.tessera.internal.document.DocumentNode;

/// Closed wrapper around internal document placements so `DocumentContent` permits only
/// same-package types.
record DocumentPlacement(DocumentNode node) implements DocumentContent {
  static {
    DocumentAccess.registerPlacement(
        content -> ((DocumentPlacement) content).node(), DocumentPlacement::new);
  }

  @Override
  public DocumentContent key(String key) {
    return new DocumentPlacement(node.withKey(key));
  }
}
