package dev.nthings.tessera.document;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.DocumentHostData;
import dev.nthings.tessera.internal.authoring.DocumentNode;
import java.util.Collection;

/// A portable host element whose children continue with document content. The host keeps its own
/// attributes, styles and children; children added here follow them. Use it to place document
/// content, such as a footnote or late content, inside markup:
///
/// ```java
/// DocumentElement.of(p().text("Supported in current engines"))
///     .child(Footnote.of("Chromium and Firefox.").reference());
/// ```
public final class DocumentElement implements DocumentContent {

  static {
    AuthoringAccess.registerDocumentHost(element -> element.data);
  }

  private final DocumentHostData data;

  private DocumentElement(TagElement host) {
    this.data = new DocumentHostData(host);
  }

  /// Wraps `host`, whose attributes, styles and children are read when the page is expanded.
  public static DocumentElement of(TagElement host) {
    return new DocumentElement(host);
  }

  /// Adds portable `child` as the next child. Throws IllegalStateException for a void host.
  public DocumentElement child(IntoElement child) {
    return append(new DocumentNode.Portable(child.intoElement()));
  }

  /// Adds document `child` as the next child. Throws IllegalStateException for a void host.
  public DocumentElement child(DocumentContent child) {
    return append(DocumentNode.capture(child));
  }

  /// Adds a text child, escaped on output.
  public DocumentElement text(String text) {
    return child(Elements.text(text));
  }

  /// Adds each document child in order.
  public DocumentElement children(DocumentContent... children) {
    for (var child : children) {
      append(DocumentNode.capture(child));
    }
    return this;
  }

  /// Adds each document child in iteration order.
  public DocumentElement children(Collection<? extends DocumentContent> children) {
    for (var child : children) {
      append(DocumentNode.capture(child));
    }
    return this;
  }

  private DocumentElement append(DocumentNode child) {
    AuthoringAccess.tag(data.host).requireChildren();
    data.children.add(child);
    return this;
  }
}
