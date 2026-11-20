package dev.nthings.tessera.view;

import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.internal.view.LinkedNode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

/// Live snapshots and compiled pages as jsoup documents, so both backends share assertions.
final class LiveDom {
  private LiveDom() {}

  /// `snapshot`'s roots as the body of a document.
  static Document dom(ViewSnapshot<?> snapshot) {
    var document = Document.createShell("");
    append(document.body(), snapshot.roots());
    return document;
  }

  /// The compiled page of `content`.
  static Document page(IntoElement content) {
    return Jsoup.parse(Compiler.forPage().compile(PageDocument.of("Test", content)).html());
  }

  /// The compiled page of document `content`.
  static Document page(DocumentContent content) {
    return Jsoup.parse(Compiler.forPage().compile(PageDocument.of("Test", content)).html());
  }

  /// One element of a body: tag, attributes other than generated styling and scope markers, text.
  /// Attribute names are lowercased, as the HTML parser reads them.
  record Semantic(String tag, Map<String, String> attributes, String text) {}

  private static final Set<String> GENERATED = Set.of("class", "data-scope", "data-scope-id");

  /// The body's elements in document order, without generated class and scope markers.
  static List<Semantic> semantics(Document document) {
    return document.body().getAllElements().stream()
        .map(element -> {
          var attributes = new HashMap<String, String>();
          element
              .attributes()
              .forEach(attribute -> {
                if (!GENERATED.contains(attribute.getKey())) {
                  attributes.put(attribute.getKey().toLowerCase(Locale.ROOT), attribute.getValue());
                }
              });
          return new Semantic(element.tagName(), attributes, element.ownText());
        })
        .toList();
  }

  private static void append(Element parent, List<? extends LinkedNode<?>> nodes) {
    for (var node : nodes) {
      switch (node) {
        case LinkedNode.Tag<?> tag -> {
          var element = parent.appendElement(tag.tag().tagName());
          tag.attributes().forEach(element::attr);
          append(element, tag.children());
        }
        case LinkedNode.Text(var text) -> parent.appendChild(new TextNode(text));
        case LinkedNode.Group<?> group -> append(parent, group.children());
        case LinkedNode.Special<?> _ -> {}
      }
    }
  }
}
