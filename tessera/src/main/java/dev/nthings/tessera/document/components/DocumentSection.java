package dev.nthings.tessera.document.components;

import dev.nthings.tessera.components.Section;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.internal.components.WidgetBridge;
import dev.nthings.tessera.internal.document.DocumentNode;
import java.util.Collection;
import java.util.List;

/// Document decorator that continues a portable [Section]'s body with document-only content, such
/// as paragraphs holding footnote references or deferred overlays.
public final class DocumentSection {

  private DocumentSection() {}

  /// Places `section` with document `content` appended after its portable children. `section` is
  /// read when the page is expanded, so later configuration still applies.
  public static DocumentContent of(Section section, DocumentContent... content) {
    return of(section, List.of(content));
  }

  /// Places `section` with document `content` appended after its portable children, in iteration
  /// order.
  public static DocumentContent of(Section section, Collection<? extends DocumentContent> content) {
    var items = List.copyOf(content);
    return DocumentNode.namedComponent(
        DocumentSection.class,
        _ -> {
          var parts = WidgetBridge.section(section);
          return DocumentElement.of(parts.section())
              .child(DocumentElement.of(parts.body()).children(items));
        });
  }
}
