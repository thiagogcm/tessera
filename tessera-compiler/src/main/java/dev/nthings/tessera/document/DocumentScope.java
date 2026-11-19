package dev.nthings.tessera.document;

import dev.nthings.tessera.core.ScopeDefinition;
import dev.nthings.tessera.internal.authoring.SourceProvenance;
import dev.nthings.tessera.internal.document.DocumentNode;

/// Styled scope whose host holds document content, such as a layout around late or end-of-page
/// sections. See [ScopeDefinition] for its id, name and styles. Its host comes from the document
/// builder family, so it is always one authored element.
public interface DocumentScope extends ScopeDefinition, DocumentContent {

  /// Builds the scope's host, which carries `data-scope` and `data-scope-id`.
  DocumentElement expand(DocumentContext context);

  @Override
  default DocumentTree intoDocumentTree() {
    return new DocumentPlacement(new DocumentNode.ScopePlacement(this, SourceProvenance.capture()));
  }
}
