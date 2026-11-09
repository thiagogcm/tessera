package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.ClientBehavior;
import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/// Per-page state shared by all [PageContext]s of one page compilation.
final class DocumentState {

  private final Map<DocumentKey<?>, Object> values = new LinkedHashMap<>();
  private final List<Contribution<Element>> headElements = new ArrayList<>();
  private final List<Contribution<DocumentHeading>> headings = new ArrayList<>();
  private final Map<ElementRef, Boolean> headingTargets = new IdentityHashMap<>();
  private final Map<String, ClientBehavior> behaviors = new LinkedHashMap<>();
  private final Map<DocumentKey<?>, Contribution<Function<RenderContext, ? extends IntoElement>>>
      documentEndSections = new LinkedHashMap<>();

  /// Content placed at a page-level position, with the component context that placed it.
  record Contribution<T>(T content, ViewLocation placedBy) {}

  <T> T getOrCreate(DocumentKey<T> key) {
    return key.getOrCreate(values);
  }

  void contributeHead(Element element, ViewLocation placedBy) {
    headElements.add(new Contribution<>(element, placedBy));
  }

  void atDocumentEnd(
      DocumentKey<?> owner,
      Function<RenderContext, ? extends IntoElement> section,
      ViewLocation placedBy) {
    documentEndSections.putIfAbsent(owner, new Contribution<>(section, placedBy));
  }

  /// Registers `behavior` once per page; a later behavior with the same id is ignored.
  void contributeBehavior(ClientBehavior behavior) {
    behaviors.putIfAbsent(behavior.id(), behavior);
  }

  List<ClientBehavior> behaviors() {
    return List.copyOf(behaviors.values());
  }

  /// Head contributions, in registration order; live, since laying one out may add more.
  List<Contribution<Element>> headElements() {
    return Collections.unmodifiableList(headElements);
  }

  /// Registers `heading` for its target. A second heading for the same target is ignored.
  void registerHeading(DocumentHeading heading, ViewLocation placedBy) {
    if (headingTargets.putIfAbsent(heading.target(), Boolean.TRUE) == null) {
      headings.add(new Contribution<>(heading, placedBy));
    }
  }

  /// Headings with their registration context, in registration order.
  List<Contribution<DocumentHeading>> headingContributions() {
    return List.copyOf(headings);
  }

  List<DocumentHeading> headings() {
    return headings.stream().map(Contribution::content).toList();
  }

  /// End-of-page sections, in registration order. Only the first section per owner key is kept.
  List<Contribution<Function<RenderContext, ? extends IntoElement>>> documentEndSections() {
    return List.copyOf(documentEndSections.values());
  }
}
