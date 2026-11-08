package dev.nthings.tessera.core;

import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Explicit context for one expansion turn, passed to every render and document callback.
public final class RenderContext {

  private final ViewScope viewScope;
  private final StyleSheetCollector styles;
  private final Tokens tokens;
  private final DocumentState docState;
  private final List<String> componentPath;
  private final @Nullable SourceLocation origin;

  private RenderContext(
      ViewScope viewScope,
      StyleSheetCollector styles,
      Tokens tokens,
      DocumentState docState,
      List<String> componentPath,
      @Nullable SourceLocation origin) {
    this.viewScope = viewScope;
    this.styles = styles;
    this.tokens = tokens;
    this.docState = docState;
    this.componentPath = List.copyOf(componentPath);
    this.origin = origin;
  }

  /// Creates the root context of one document compilation.
  public static RenderContext forDocument(
      StyleSheetCollector styles, Tokens tokens, DocumentState state) {
    return new RenderContext(ViewScope.ROOT, styles, tokens, state, List.of(), null);
  }

  public ViewScope viewScope() {
    return viewScope;
  }

  public StyleSheetCollector styles() {
    return styles;
  }

  public Tokens tokens() {
    return tokens;
  }

  /// Per-page state for `key`, created on first use.
  public <T> T document(DocumentKey<T> key) {
    return docState.getOrCreate(key);
  }

  /// Adds `element` to the page's `<head>` (after `<title>`).
  public void contributeHead(IntoElement element) {
    docState.contributeHead(element.intoElement(), componentPath, origin);
  }

  /// Appends `section` to the end of `<body>` after layout; only the first registration per
  /// `owner` counts.
  public void atDocumentEnd(
      DocumentKey<?> owner, Function<RenderContext, ? extends IntoElement> section) {
    docState.atDocumentEnd(owner, section, componentPath, origin);
  }

  /// Contributes `behavior` to the document once, regardless of how many components request it.
  public void contributeBehavior(ClientBehavior behavior) {
    docState.contributeBehavior(behavior);
  }

  public DocumentState documentState() {
    return docState;
  }

  /// Registers metadata for a local element id in the current view scope.
  public DocumentHeading registerHeading(String localId, String title, int level) {
    var heading =
        new DocumentHeading(viewScope.extend(ElementId.of(localId)).domId(), title, level);
    docState.registerHeading(heading);
    return heading;
  }

  /// Derives a context whose identity scope is nested under `viewEntityId`.
  public RenderContext withViewScope(ElementId viewEntityId) {
    return new RenderContext(
        viewScope.extend(viewEntityId), styles, tokens, docState, componentPath, origin);
  }

  /// Enclosing components of the current expansion, retained by document contributions.
  public List<String> componentPath() {
    return componentPath;
  }

  /// The call site that placed the innermost enclosing component, while [SourceTracking] is
  /// active. Elements a component builds itself are attributed to it.
  public @Nullable SourceLocation origin() {
    return origin;
  }

  /// Enters a component without changing its element identity scope. `placedAt` is where
  /// authoring code placed the component; without one, the enclosing component's origin stays.
  public RenderContext withComponent(String component, @Nullable SourceLocation placedAt) {
    var path = new ArrayList<>(componentPath);
    path.add(component);
    return withComponentPath(path, placedAt != null ? placedAt : origin);
  }

  /// Expands registered document content with the component path and origin captured at
  /// registration.
  public RenderContext withComponentPath(List<String> path, @Nullable SourceLocation origin) {
    return new RenderContext(viewScope, styles, tokens, docState, path, origin);
  }
}
