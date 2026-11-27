package dev.nthings.tessera.document.components;

import dev.nthings.tessera.core.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentTree;
import dev.nthings.tessera.internal.document.DocumentAccess;
import dev.nthings.tessera.internal.document.DocumentNode;

/// Base of built-in document components. Placing one wraps its [#expand(DocumentContext)] in a
/// named document component placement, so `expand` stays off the public component types.
/// Diagnostics name it by its simple class name.
abstract class BuiltinDocumentComponent implements DocumentContent {
  static {
    DocumentAccess.registerBuiltin(content ->
        content instanceof BuiltinDocumentComponent builtin
            ? DocumentNode.component(builtin.getClass(), builtin::expand)
            : null);
  }

  /// Builds the component's document content; called by the compiler for each placement.
  abstract DocumentContent expand(DocumentContext context);

  @Override
  public final DocumentTree key(String key) {
    return DocumentAccess.wrap(DocumentNode.capture(this).withKey(key));
  }
}
