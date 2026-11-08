package dev.nthings.tessera.core;

import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Explicit context for one expansion turn. Render callbacks, late elements and document-end
/// sections receive it. Don't keep it after the callback returns, and don't share mutable
/// application state through it: callbacks may run concurrently across pages.
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

  /// Compiler-facing: creates the root context of one document compilation.
  public static RenderContext forDocument(
      StyleSheetCollector styles, Tokens tokens, DocumentState state) {
    return new RenderContext(ViewScope.ROOT, styles, tokens, state, List.of(), null);
  }

  /// The [ViewScope] that ids created in this turn live in.
  public ViewScope viewScope() {
    return viewScope;
  }

  /// Compiler-facing: the stylesheet collector for the whole compilation.
  public StyleSheetCollector styles() {
    return styles;
  }

  /// The design tokens of this compilation.
  public Tokens tokens() {
    return tokens;
  }

  /// Per-page state for `key`, created on first use. Use it for facts that must hold once per page,
  /// such as whether a shared head element was already contributed.
  public <T> T document(DocumentKey<T> key) {
    return docState.getOrCreate(key);
  }

  /// Adds `element` to the page's `<head>`, after `<title>`, in contribution order. Nothing is
  /// deduplicated: N placements add N elements, so guard shared head content with
  /// [#document(DocumentKey)].
  public void contributeHead(IntoElement element) {
    docState.contributeHead(element.intoElement(), componentPath, origin);
  }

  /// Adds `section` to the end of `<body>`, after the page's late elements. Only the first
  /// registration per `owner` counts; later ones are ignored.
  public void atDocumentEnd(
      DocumentKey<?> owner, Function<RenderContext, ? extends IntoElement> section) {
    docState.atDocumentEnd(owner, section, componentPath, origin);
  }

  /// Adds `behavior` to the page once. The first registration with a given id wins, and later ones
  /// with that id are ignored silently.
  public void contributeBehavior(ClientBehavior behavior) {
    docState.contributeBehavior(behavior);
  }

  /// Headings registered so far on this page, in registration order. Late elements see every
  /// heading in the body.
  public List<DocumentHeading> headings() {
    return docState.headings();
  }

  /// Registers a heading for `localId` in the current view scope, and returns it. The element
  /// that is the heading must emit the returned `targetDomId` as its id. For a target registered
  /// twice, the first heading is kept and the second is ignored silently. Throws
  /// IllegalArgumentException if `level` is outside 1 to 6 or `localId` is empty.
  public DocumentHeading registerHeading(String localId, String title, int level) {
    var heading =
        new DocumentHeading(viewScope.extend(ElementId.of(localId)).domId(), title, level);
    docState.registerHeading(heading);
    return heading;
  }

  /// Compiler-facing: a context whose view scope is extended by `viewEntityId`.
  public RenderContext withViewScope(ElementId viewEntityId) {
    return new RenderContext(
        viewScope.extend(viewEntityId), styles, tokens, docState, componentPath, origin);
  }

  /// Compiler-facing: the names of the enclosing components, kept with document contributions for
  /// diagnostics.
  public List<String> componentPath() {
    return componentPath;
  }

  /// Compiler-facing: the call site of the innermost enclosing component, when source tracking is
  /// on. Elements that component builds itself are attributed to it.
  public @Nullable SourceLocation origin() {
    return origin;
  }

  /// Compiler-facing: enters `component` for diagnostics, keeping the view scope. Without
  /// `placedAt`, the enclosing origin is kept.
  public RenderContext withComponent(String component, @Nullable SourceLocation placedAt) {
    var path = new ArrayList<>(componentPath);
    path.add(component);
    return withComponentPath(path, placedAt != null ? placedAt : origin);
  }

  /// Compiler-facing: a context with the given component path and origin, keeping the view scope.
  public RenderContext withComponentPath(List<String> path, @Nullable SourceLocation origin) {
    return new RenderContext(viewScope, styles, tokens, docState, path, origin);
  }
}
