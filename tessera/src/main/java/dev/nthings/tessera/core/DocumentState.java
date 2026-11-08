package dev.nthings.tessera.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Compiler-facing: per-page state shared by all [RenderContext]s of one page compilation.
/// Components reach it through [RenderContext], not directly.
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

  /// Compiler-facing: content placed at a page-level position, with the component path and the
  /// call site that placed it.
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

  /// Registers `behavior` once per page; a later behavior with the same id is ignored.
  void contributeBehavior(ClientBehavior behavior) {
    behaviors.putIfAbsent(behavior.id(), behavior);
  }

  /// Compiler-facing: registered client behaviors, in registration order.
  public List<ClientBehavior> behaviors() {
    return List.copyOf(behaviors.values());
  }

  /// Compiler-facing: head contributions, in registration order.
  public List<Contribution<Element>> headElements() {
    return Collections.unmodifiableList(headElements);
  }

  /// Registers `heading` for its target. A second heading for the same target is ignored silently;
  /// a repeated element id is reported as `duplicate-id`.
  void registerHeading(DocumentHeading heading) {
    headings.putIfAbsent(heading.targetDomId(), heading);
  }

  /// Compiler-facing: headings, in registration order.
  public List<DocumentHeading> headings() {
    return List.copyOf(headings.values());
  }

  /// Compiler-facing: end-of-page sections, in registration order. Only the first section per owner
  /// key is kept.
  public List<Contribution<Function<RenderContext, ? extends IntoElement>>> documentEndSections() {
    return List.copyOf(documentEndSections.values());
  }
}
