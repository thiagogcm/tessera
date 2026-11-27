package dev.nthings.tessera.document;

import dev.nthings.tessera.core.DocumentContent;
import dev.nthings.tessera.internal.document.DocumentNode;

/// Document-only content produced by [DocumentElement] and the [Documents] factories. The hierarchy
/// is closed; custom document content implements [DocumentComponent] or [DocumentScope] instead.
public sealed interface DocumentTree extends DocumentContent
    permits DocumentElement, DocumentPlacement {

  @Override
  default DocumentTree key(String key) {
    return new DocumentPlacement(DocumentNode.capture(this).withKey(key));
  }
}
