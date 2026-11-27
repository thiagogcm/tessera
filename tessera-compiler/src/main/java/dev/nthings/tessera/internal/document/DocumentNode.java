package dev.nthings.tessera.internal.document;

import dev.nthings.tessera.core.DocumentContent;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.DocumentScope;
import dev.nthings.tessera.document.DocumentTree;
import dev.nthings.tessera.document.WasmBundle;
import dev.nthings.tessera.internal.authoring.PortableNode;
import dev.nthings.tessera.internal.authoring.SourceProvenance;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Static placements captured when content is added to a document builder.
public sealed interface DocumentNode {

  default DocumentNode withKey(String key) {
    return new Keyed(key, this);
  }

  record Portable(Element element) implements DocumentNode {
    @Override
    public DocumentNode withKey(String key) {
      return new Portable(element.key(key));
    }
  }

  record Raw(String html) implements DocumentNode {}

  record Deferred(DocumentNode child, int priority) implements DocumentNode {}

  record Late(Function<DocumentContext, ? extends DocumentContent> content)
      implements DocumentNode {}

  record Host(TagElement host, List<DocumentNode> children) implements DocumentNode {
    public Host {
      children = List.copyOf(children);
    }
  }

  /// A Wasm host with the page-wide DOM id `id`, whose fallback content shares the id scope the
  /// live root renders in.
  record Mount(String id, String view, WasmBundle bundle, DocumentNode fallback)
      implements DocumentNode {}

  record Fragment(List<DocumentNode> children) implements DocumentNode {
    public Fragment {
      children = List.copyOf(children);
    }
  }

  record Keyed(String key, DocumentNode content) implements DocumentNode {
    public Keyed {
      PortableNode.requireKey(key);
    }

    @Override
    public DocumentNode withKey(String key) {
      return new Keyed(key, content);
    }
  }

  record ComponentPlacement(
      Class<?> type,
      @Nullable SourceLocation source,
      Function<DocumentContext, ? extends DocumentContent> expand)
      implements DocumentNode {}

  record ScopePlacement(DocumentScope scope, @Nullable SourceLocation source)
      implements DocumentNode {}

  static DocumentTree namedComponent(
      Class<?> type, Function<DocumentContext, ? extends DocumentContent> expand) {
    return DocumentAccess.wrap(component(type, expand));
  }

  static DocumentNode component(
      Class<?> type, Function<DocumentContext, ? extends DocumentContent> expand) {
    return new ComponentPlacement(type, SourceProvenance.capture(), expand);
  }

  /// Lowers content at the moment it is placed, so source tracking records the placing call.
  static DocumentNode capture(DocumentContent content) {
    return switch (content) {
      case IntoElement portable -> new Portable(portable.intoElement());
      case DocumentElement element -> DocumentAccess.host(element);
      case DocumentTree tree -> DocumentAccess.unwrap(tree);
      case DocumentComponent component -> component(component.getClass(), component::expand);
      case DocumentScope scope -> new ScopePlacement(scope, SourceProvenance.capture());
      default -> {
        var builtin = DocumentAccess.builtin(content);
        if (builtin == null) {
          throw new IllegalArgumentException(
              content.getClass().getName()
                  + " is not placeable: implement DocumentComponent or DocumentScope");
        }
        yield builtin;
      }
    };
  }
}
