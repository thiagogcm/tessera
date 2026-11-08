package dev.nthings.tessera.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Per-page state shared by all [RenderContext]s of one compilation.
public final class DocumentState {

  private final Map<DocumentKey<?>, Object> values = new LinkedHashMap<>();
  private final List<Contribution<Element>> headElements = new ArrayList<>();
  private final Map<String, DocumentHeading> headings = new LinkedHashMap<>();
  private final Map<String, ClientBehavior> behaviors = new LinkedHashMap<>();
  private final Map<DocumentKey<?>, Contribution<Function<RenderContext, ? extends IntoElement>>>
      documentEndSections = new LinkedHashMap<>();

  <T> T getOrCreate(DocumentKey<T> key) {
    return key.cast(values.computeIfAbsent(key, _ -> key.create()));
  }

  /// Content placed at a document-level position, with its component path and placing call site.
  public record Contribution<T>(
      T content, List<String> componentPath, @Nullable SourceLocation origin) {}

  void contributeHead(
      Element element, List<String> componentPath, @Nullable SourceLocation origin) {
    headElements.add(new Contribution<>(element, componentPath, origin));
  }

  void atDocumentEnd(
      DocumentKey<?> owner,
      Function<RenderContext, ? extends IntoElement> section,
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

  /// Keeps the first heading per target; a reused id is reported as `duplicate-id`.
  void registerHeading(DocumentHeading heading) {
    headings.putIfAbsent(heading.targetDomId(), heading);
  }

  /// Headings in registration order.
  public List<DocumentHeading> headings() {
    return List.copyOf(headings.values());
  }

  /// End-of-document sections in registration order.
  public List<Contribution<Function<RenderContext, ? extends IntoElement>>> documentEndSections() {
    return List.copyOf(documentEndSections.values());
  }
}
