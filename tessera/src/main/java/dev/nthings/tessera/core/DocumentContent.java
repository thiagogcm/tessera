package dev.nthings.tessera.core;

/// Content that a static document places. Portable content ([IntoElement]) is document content, so
/// an element, a component or a scope goes straight into a page body or a document builder. The
/// compiler adds document-only content: raw HTML, deferred and late content, document components
/// and document scopes. Portable builders accept only [IntoElement], so document-only content never
/// reaches a portable tree or a browser root.
///
/// Implement `DocumentComponent` or `DocumentScope` from the compiler module for custom document
/// content; a type that implements this interface directly is rejected when it is placed.
public interface DocumentContent {

  /// Gives this content a stable identity among its siblings, as [IntoElement#key(String)] does.
  /// Calling `key` again replaces the key. Throws IllegalArgumentException if `key` is blank.
  DocumentContent key(String key);
}
