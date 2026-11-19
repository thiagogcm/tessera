package dev.nthings.tessera.document;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.document.DocumentAccess;
import dev.nthings.tessera.internal.document.DocumentNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/// A portable host element whose children continue with document content. The host keeps its own
/// attributes, styles and children; children added here follow them. Use it to place document
/// content, such as a footnote or late content, inside markup:
///
/// ```java
/// DocumentElement.of(p().text("Supported in current engines"))
///     .child(Footnote.of("Chromium and Firefox.").reference());
/// ```
public final class DocumentElement implements DocumentTree, FluentBuilder<DocumentElement> {

  static {
    DocumentAccess.registerHost(element -> new DocumentNode.Host(element.host, element.children));
  }

  private final TagElement host;
  private final List<DocumentNode> children = new ArrayList<>();

  private DocumentElement(TagElement host) {
    this.host = host;
  }

  /// Wraps `host`, whose attributes, styles and children are read when the page is expanded.
  public static DocumentElement of(TagElement host) {
    return new DocumentElement(host);
  }

  @Override
  public DocumentElement self() {
    return this;
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

  /// Adds portable children in order.
  public DocumentElement children(IntoElement first, IntoElement... rest) {
    child(first);
    for (var child : rest) {
      child(child);
    }
    return this;
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

  /// Adds each document child from `stream` in order. The stream is consumed.
  public DocumentElement children(Stream<? extends DocumentContent> stream) {
    stream.forEach(this::child);
    return this;
  }

  private DocumentElement append(DocumentNode child) {
    AuthoringAccess.tag(host).requireChildren();
    children.add(child);
    return this;
  }
}
