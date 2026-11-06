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
  private final List<Element> headElements = new ArrayList<>();
  private final Map<String, DocumentHeading> headings = new LinkedHashMap<>();
  private final Map<DocumentKey<?>, Supplier<? extends IntoElement>> documentEndSections =
      new LinkedHashMap<>();

  <T> T getOrCreate(DocumentKey<T> key) {
    return key.cast(values.computeIfAbsent(key, _ -> key.create()));
  }

  /// State stored under `key`, or `null` if no component on the page created it.
  public <T> @Nullable T get(DocumentKey<T> key) {
    var value = values.get(key);
    return value != null ? key.cast(value) : null;
  }

  void contributeHead(Element element) {
    headElements.add(element);
  }

  void atDocumentEnd(DocumentKey<?> owner, Supplier<? extends IntoElement> section) {
    documentEndSections.putIfAbsent(owner, section);
  }

  public List<Element> headElements() {
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
  public List<Supplier<? extends IntoElement>> documentEndSections() {
    return List.copyOf(documentEndSections.values());
  }
}
