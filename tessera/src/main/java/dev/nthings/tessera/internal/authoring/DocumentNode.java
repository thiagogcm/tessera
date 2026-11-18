package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.DocumentScope;
import dev.nthings.tessera.document.WasmBundle;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Static placements captured when content is added to a document builder.
public sealed interface DocumentNode extends DocumentContent {

  record Portable(Element element) implements DocumentNode {
    @Override
    public DocumentContent key(String key) {
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

  record Mount(ElementRef id, String view, WasmBundle bundle, DocumentNode fallback)
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
    public DocumentContent key(String key) {
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

  /// A built-in decorator with the diagnostic name of the type it decorates.
  record NamedComponent(Class<?> type, DocumentComponent body) implements DocumentComponent {
    @Override
    public DocumentContent expand(DocumentContext context) {
      return body.expand(context);
    }
  }

  static DocumentNode capture(DocumentContent content) {
    return switch (content) {
      case DocumentNode node -> node;
      case DocumentElement element -> AuthoringAccess.documentHost(element);
      case NamedComponent(var type, var body) ->
          new ComponentPlacement(type, SourceProvenance.capture(), body::expand);
      case DocumentScope scope -> new ScopePlacement(scope, SourceProvenance.capture());
      case DocumentComponent component ->
          new ComponentPlacement(
              component.getClass(), SourceProvenance.capture(), component::expand);
    };
  }
}
