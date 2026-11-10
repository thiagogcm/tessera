package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.PageScript;
import dev.nthings.tessera.view.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/// The document side of one page compilation: head contributions, client behaviors and
/// end-of-body sections, as the [DocumentPass] backend receives them. Per-page values and headings
/// live in the expansion's [dev.nthings.tessera.view.DocumentRegistry], as in every backend.
final class DocumentState {

  private final List<Contribution<Element>> headElements = new ArrayList<>();
  private final Map<String, PageScript> behaviors = new LinkedHashMap<>();
  private final Map<DocumentKey<?>, Contribution<Function<DocumentContext, ? extends IntoElement>>>
      documentEndSections = new LinkedHashMap<>();

  /// Content placed at a page-level position, with the component context that placed it.
  record Contribution<T>(T content, Location placedBy) {}

  void contributeHead(Element element, Location placedBy) {
    headElements.add(new Contribution<>(element, placedBy));
  }

  void atDocumentEnd(
      DocumentKey<?> owner,
      Function<DocumentContext, ? extends IntoElement> section,
      Location placedBy) {
    documentEndSections.putIfAbsent(owner, new Contribution<>(section, placedBy));
  }

  /// Registers `behavior` once per page; a later behavior with the same id is ignored.
  void contributeScript(PageScript behavior) {
    behaviors.putIfAbsent(behavior.id(), behavior);
  }

  List<PageScript> scripts() {
    return List.copyOf(behaviors.values());
  }

  /// Head contributions, in registration order; live, since laying one out may add more.
  List<Contribution<Element>> headElements() {
    return Collections.unmodifiableList(headElements);
  }

  /// End-of-page sections, in registration order. Only the first section per owner key is kept.
  List<Contribution<Function<DocumentContext, ? extends IntoElement>>> documentEndSections() {
    return List.copyOf(documentEndSections.values());
  }
}
