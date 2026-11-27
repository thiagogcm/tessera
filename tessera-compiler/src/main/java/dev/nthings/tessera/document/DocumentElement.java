package dev.nthings.tessera.document;

import dev.nthings.tessera.core.DocumentContent;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TagHost;
import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.document.DocumentAccess;
import dev.nthings.tessera.internal.document.DocumentNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
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
  public static DocumentElement of(TagHost host) {
    return new DocumentElement(host.toTagElement());
  }

  /// Wraps `host` and runs `configure` on the resulting document element, allowing mixed portable
  /// and document children to be appended in one block.
  public static DocumentElement of(TagHost host, Consumer<DocumentElement> configure) {
    var element = of(host);
    configure.accept(element);
    return element;
  }

  @Override
  public DocumentElement self() {
    return this;
  }

  /// Adds `child` as the next child. Throws IllegalStateException for a void host.
  public DocumentElement child(DocumentContent child) {
    return append(DocumentNode.capture(child));
  }

  /// Adds a text child, escaped on output.
  public DocumentElement text(String text) {
    return child(Elements.text(text));
  }

  /// Adds each child in order.
  public DocumentElement children(DocumentContent... children) {
    for (var child : children) {
      append(DocumentNode.capture(child));
    }
    return this;
  }

  /// Adds each child in iteration order.
  public DocumentElement children(Collection<? extends DocumentContent> children) {
    for (var child : children) {
      append(DocumentNode.capture(child));
    }
    return this;
  }

  /// Adds each child from `stream` in order. The stream is consumed.
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
