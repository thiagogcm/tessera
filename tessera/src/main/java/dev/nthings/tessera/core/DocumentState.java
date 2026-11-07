package dev.nthings.tessera.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Per-page state shared by every [RenderContext] of one compilation: component registries keyed
/// by [DocumentKey], `<head>` contributions, and end-of-document sections.
public final class DocumentState {

  private final Map<DocumentKey<?>, Object> values = new LinkedHashMap<>();
  private final List<Contribution<Element>> headElements = new ArrayList<>();
  private final Map<String, DocumentHeading> headings = new LinkedHashMap<>();
  private final Map<String, ClientBehavior> behaviors = new LinkedHashMap<>();
  private final Map<DocumentKey<?>, Contribution<Supplier<? extends IntoElement>>>
      documentEndSections = new LinkedHashMap<>();

  <T> T getOrCreate(DocumentKey<T> key) {
    return key.cast(values.computeIfAbsent(key, _ -> key.create()));
  }

  /// State stored under `key`, or `null` if no component on the page created it.
  public <T> @Nullable T get(DocumentKey<T> key) {
    var value = values.get(key);
    return value != null ? key.cast(value) : null;
  }

  /// Content registered for a document-level position, with its originating component path and
  /// the call site that placed that component.
  public record Contribution<T>(
      T content, List<String> componentPath, @Nullable SourceLocation origin) {
    public Contribution {
      componentPath = List.copyOf(componentPath);
    }
  }

  void contributeHead(
      Element element, List<String> componentPath, @Nullable SourceLocation origin) {
    headElements.add(new Contribution<>(element, componentPath, origin));
  }

  void atDocumentEnd(
      DocumentKey<?> owner,
      Supplier<? extends IntoElement> section,
      List<String> componentPath,
      @Nullable SourceLocation origin) {
    documentEndSections.putIfAbsent(owner, new Contribution<>(section, componentPath, origin));
  }

  /// Registers `behavior` once per document; later contributions with the same id are ignored.
  void contributeBehavior(ClientBehavior behavior) {
    behaviors.putIfAbsent(behavior.id(), behavior);
  }

  /// Registered client behaviors in contribution order.
  public List<ClientBehavior> behaviors() {
    return List.copyOf(behaviors.values());
  }

  public List<Contribution<Element>> headElements() {
    return Collections.unmodifiableList(headElements);
  }

  void registerHeading(DocumentHeading heading) {
    if (headings.putIfAbsent(heading.targetDomId(), heading) != null) {
      throw new IllegalArgumentException(
          "Duplicate section id '" + heading.targetDomId() + "' on the same page");
    }
  }

  /// Registered navigation headings in registration order, independent of component types.
  public List<DocumentHeading> headings() {
    return List.copyOf(headings.values());
  }

  /// Snapshot of the registered end-of-document sections, in registration order.
  public List<Contribution<Supplier<? extends IntoElement>>> documentEndSections() {
    return List.copyOf(documentEndSections.values());
  }
}
