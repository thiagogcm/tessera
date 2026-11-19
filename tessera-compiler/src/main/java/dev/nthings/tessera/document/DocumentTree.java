package dev.nthings.tessera.document;

/// Lowered document content produced by [DocumentElement], [Documents] factories,
/// [DocumentComponent], [DocumentScope], and built-in document components. The hierarchy is closed;
/// custom document builders implement [DocumentContent] instead.
public sealed interface DocumentTree extends DocumentContent
    permits DocumentElement, DocumentPlacement {

  @Override
  default DocumentTree intoDocumentTree() {
    return this;
  }
}
