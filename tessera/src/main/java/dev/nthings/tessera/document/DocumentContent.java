package dev.nthings.tessera.document;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.internal.document.DocumentNode;

/// Content that only a static document places: raw HTML, deferred and late content, document
/// components and scopes, and portable content converted with [#of(IntoElement)]. Pass it to page
/// bodies, document builders such as [DocumentElement], and other document content. Portable
/// builders do not accept it, and there is no conversion back to portable or to live content.
public sealed interface DocumentContent
    permits DocumentComponent, DocumentScope, DocumentElement, DocumentPlacement {

  /// Portable `content` placed as document content. It is lowered now, as a builder's `child` would
  /// lower it.
  static DocumentContent of(IntoElement content) {
    return new DocumentPlacement(new DocumentNode.Portable(content.intoElement()));
  }

  /// Gives this content a stable identity among its siblings, as [IntoElement#key(String)] does.
  /// Calling `key` again replaces the key. Throws IllegalArgumentException if `key` is blank.
  default DocumentContent key(String key) {
    return new DocumentPlacement(new DocumentNode.Keyed(key, DocumentNode.capture(this)));
  }
}
