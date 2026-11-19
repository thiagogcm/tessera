package dev.nthings.tessera.document;

import dev.nthings.tessera.internal.authoring.SourceProvenance;
import dev.nthings.tessera.internal.document.DocumentNode;

/// Component that needs document assembly: head contributions, page scripts, late or document-end
/// content, the collected outline, or document children such as raw HTML. It receives a
/// [DocumentContext] and returns [DocumentContent], so it can be placed only in static pages.
/// Portable `Component`s placed inside it still receive only a `ViewContext`.
@FunctionalInterface
public interface DocumentComponent extends DocumentContent {

  /// Builds the component's content. Expansion calls this for each place the component appears.
  /// Don't keep `context` after the call returns.
  DocumentContent expand(DocumentContext context);

  @Override
  default DocumentTree intoDocumentTree() {
    return new DocumentPlacement(
        new DocumentNode.ComponentPlacement(getClass(), SourceProvenance.capture(), this::expand));
  }
}
