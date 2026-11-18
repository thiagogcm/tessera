package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.document.DocumentContent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/// One component case: docs copy and a fresh factory per variant.
record Story(
    String id,
    String title,
    String summary,
    List<String> features,
    List<String> code,
    List<Variant> variants) {

  /// Named state of this case; `content` runs on every render.
  record Variant(String name, String label, boolean inDocs, Supplier<DocumentContent> content) {
    DocumentContent create() {
      return content.get();
    }
  }

  String heading(Variant variant) {
    return title + " — " + variant.label();
  }

  Demo documentation() {
    return Demo.of(id, title)
        .summary(summary)
        .features(features.toArray(String[]::new))
        .code(code.toArray(String[]::new))
        .stage(this::documented);
  }

  private List<DocumentContent> documented() {
    var items = new ArrayList<DocumentContent>();
    for (var variant : variants) {
      if (variant.inDocs()) {
        items.add(DocumentContent.of(caseLink(variant)));
        items.add(variant.create());
      }
    }
    return items;
  }

  private IntoElement caseLink(Variant variant) {
    return p().child(a(Catalog.casePath(id, variant.name())).text(variant.label() + " case"));
  }
}
